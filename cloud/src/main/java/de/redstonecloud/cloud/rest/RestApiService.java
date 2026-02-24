package de.redstonecloud.cloud.rest;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import de.redstonecloud.cloud.RedstoneCloud;
import de.redstonecloud.cloud.config.entires.RestApiSettings;
import de.redstonecloud.cloud.config.entires.RestApiToken;
import de.redstonecloud.cloud.player.CloudPlayer;
import de.redstonecloud.shared.server.Server;
import de.redstonecloud.shared.server.Template;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

@Log4j2
@RequiredArgsConstructor
public class RestApiService {
    public static final String PERM_CLOUD_READ = "cloud.read";
    public static final String PERM_SERVER_MANAGE = "cloud.server.manage";
    public static final String PERM_SERVER_EXECUTE = "cloud.server.execute";
    public static final String PERM_PLAYER_READ = "cloud.player.read";

    private final RestApiSettings settings;
    private final Gson gson = new Gson();
    private final Map<String, TokenContext> tokenContexts = new ConcurrentHashMap<>();

    private HttpServer httpServer;

    public void start() {
        if (!settings.enabled()) {
            return;
        }

        reloadTokens(false);
        ensureStarted();
    }

    public void stop() {
        if (httpServer == null) {
            return;
        }

        httpServer.stop(1);
        httpServer = null;
        log.info("REST API stopped.");
    }

    public void reloadTokens() {
        reloadTokens(true);
    }

    private void reloadTokens(boolean startIfNeeded) {
        indexTokens();
        log.info("REST API token cache reloaded ({} active token(s)).", tokenContexts.size());

        if (tokenContexts.isEmpty()) {
            if (httpServer != null) {
                stop();
            }
            return;
        }

        if (startIfNeeded) {
            ensureStarted();
        }
    }

    private void ensureStarted() {
        if (!settings.enabled()) {
            return;
        }

        if (httpServer != null) {
            return;
        }

        if (tokenContexts.isEmpty()) {
            log.error("REST API is enabled but no valid tokens are configured. Skipping startup.");
            return;
        }

        try {
            httpServer = HttpServer.create(new InetSocketAddress(settings.host(), settings.port()), 0);
            httpServer.setExecutor(Executors.newFixedThreadPool(Math.max(2, Runtime.getRuntime().availableProcessors() / 2)));
            httpServer.createContext("/", this::handleRequest);
            httpServer.start();
            log.info("REST API started at http://{}:{}.", settings.host(), settings.port());
        } catch (Exception e) {
            log.error("Failed to start REST API on {}:{}", settings.host(), settings.port(), e);
        }
    }

    private void indexTokens() {
        tokenContexts.clear();
        for (RestApiToken token : settings.tokens()) {
            if (token == null || !token.enabled()) {
                continue;
            }

            String value = token.token() == null ? "" : token.token().trim();
            if (value.isEmpty() || value.equalsIgnoreCase("CHANGE_ME")) {
                log.warn("Skipping REST API token '{}' because it is empty or still default.", token.name());
                continue;
            }

            Set<String> permissions = new HashSet<>();
            if (token.permissions() != null) {
                token.permissions().stream()
                        .filter(permission -> permission != null && !permission.trim().isEmpty())
                        .map(String::trim)
                        .forEach(permissions::add);
            }

            tokenContexts.put(value, new TokenContext(token.name(), permissions));
        }
    }

    private void handleRequest(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod().toUpperCase(Locale.ROOT);
        String path = exchange.getRequestURI().getPath();
        try {
            if (!path.startsWith("/api/v1")) {
                sendError(exchange, 404, "Route not found.");
                return;
            }

            if (method.equals("GET") && path.equals("/api/v1/health")) {
                JsonObject out = new JsonObject();
                out.addProperty("status", "ok");
                out.addProperty("running", RedstoneCloud.isRunning());
                sendJson(exchange, 200, out);
                return;
            }

            if (method.equals("GET") && path.equals("/api/v1/me")) {
                TokenContext token = authenticate(exchange, null);
                if (token == null) {
                    return;
                }

                JsonObject out = new JsonObject();
                out.addProperty("tokenName", token.name());
                JsonArray permissions = new JsonArray();
                token.permissions().forEach(permissions::add);
                out.add("permissions", permissions);
                sendJson(exchange, 200, out);
                return;
            }

            if (method.equals("GET") && path.equals("/api/v1/servers")) {
                if (authenticate(exchange, Set.of(PERM_CLOUD_READ)) == null) {
                    return;
                }
                sendJson(exchange, 200, getServersPayload());
                return;
            }

            if (method.equals("GET") && path.startsWith("/api/v1/servers/")) {
                if (authenticate(exchange, Set.of(PERM_CLOUD_READ)) == null) {
                    return;
                }
                String serverName = path.substring("/api/v1/servers/".length()).trim();
                if (serverName.isEmpty()) {
                    sendError(exchange, 400, "Missing server name.");
                    return;
                }

                Server server = RedstoneCloud.getInstance().getServerManager().getServer(serverName);
                if (server == null) {
                    sendError(exchange, 404, "Server not found.");
                    return;
                }

                sendJson(exchange, 200, JsonParser.parseString(server.toString()).getAsJsonObject());
                return;
            }

            if (method.equals("GET") && path.equals("/api/v1/players")) {
                if (authenticate(exchange, Set.of(PERM_PLAYER_READ)) == null) {
                    return;
                }
                sendJson(exchange, 200, getPlayersPayload());
                return;
            }

            if (method.equals("GET") && path.equals("/api/v1/templates")) {
                if (authenticate(exchange, Set.of(PERM_CLOUD_READ)) == null) {
                    return;
                }
                sendJson(exchange, 200, getTemplatesPayload());
                return;
            }

            if (method.equals("POST") && path.equals("/api/v1/servers/start")) {
                if (authenticate(exchange, Set.of(PERM_SERVER_MANAGE)) == null) {
                    return;
                }
                startServer(exchange);
                return;
            }

            if (method.equals("POST") && path.endsWith("/stop") && path.startsWith("/api/v1/servers/")) {
                if (authenticate(exchange, Set.of(PERM_SERVER_MANAGE)) == null) {
                    return;
                }
                String serverName = path.substring("/api/v1/servers/".length(), path.length() - "/stop".length()).trim();
                changeServerState(exchange, serverName, "stop");
                return;
            }

            if (method.equals("POST") && path.endsWith("/kill") && path.startsWith("/api/v1/servers/")) {
                if (authenticate(exchange, Set.of(PERM_SERVER_MANAGE)) == null) {
                    return;
                }
                String serverName = path.substring("/api/v1/servers/".length(), path.length() - "/kill".length()).trim();
                changeServerState(exchange, serverName, "kill");
                return;
            }

            if (method.equals("POST") && path.endsWith("/execute") && path.startsWith("/api/v1/servers/")) {
                if (authenticate(exchange, Set.of(PERM_SERVER_EXECUTE)) == null) {
                    return;
                }
                String serverName = path.substring("/api/v1/servers/".length(), path.length() - "/execute".length()).trim();
                executeCommand(exchange, serverName);
                return;
            }

            sendError(exchange, 404, "Route not found.");
        } catch (BadRequestException e) {
            sendError(exchange, 400, e.getMessage());
        } catch (Exception e) {
            log.error("REST API request failed: {} {}", method, path, e);
            sendError(exchange, 500, "Internal server error.");
        } finally {
            exchange.close();
        }
    }

    private void startServer(HttpExchange exchange) throws IOException {
        JsonObject body = parseBody(exchange);
        if (!body.has("template")) {
            sendError(exchange, 400, "Missing 'template' in body.");
            return;
        }

        String templateName = body.get("template").getAsString();
        Template template = RedstoneCloud.getInstance().getServerManager().getTemplate(templateName);
        if (template == null) {
            sendError(exchange, 404, "Template not found.");
            return;
        }

        Integer id = body.has("id") && !body.get("id").isJsonNull() ? body.get("id").getAsInt() : null;
        Server started = RedstoneCloud.getInstance().getServerManager().startServer(template, id);
        if (started == null) {
            sendError(exchange, 409, "Server could not be started.");
            return;
        }

        sendJson(exchange, 201, JsonParser.parseString(started.toString()).getAsJsonObject());
    }

    private void executeCommand(HttpExchange exchange, String serverName) throws IOException {
        if (serverName.isEmpty()) {
            sendError(exchange, 400, "Missing server name.");
            return;
        }

        Server server = RedstoneCloud.getInstance().getServerManager().getServer(serverName);
        if (server == null) {
            sendError(exchange, 404, "Server not found.");
            return;
        }

        JsonObject body = parseBody(exchange);
        if (!body.has("command") || body.get("command").getAsString().trim().isEmpty()) {
            sendError(exchange, 400, "Missing 'command' in body.");
            return;
        }

        String command = body.get("command").getAsString();
        server.writeConsole(command);

        JsonObject out = new JsonObject();
        out.addProperty("message", "Command sent.");
        out.addProperty("server", server.getName());
        out.addProperty("command", command);
        sendJson(exchange, 200, out);
    }

    private void changeServerState(HttpExchange exchange, String serverName, String action) throws IOException {
        if (serverName.isEmpty()) {
            sendError(exchange, 400, "Missing server name.");
            return;
        }

        Server server = RedstoneCloud.getInstance().getServerManager().getServer(serverName);
        if (server == null) {
            sendError(exchange, 404, "Server not found.");
            return;
        }

        switch (action) {
            case "stop" -> server.stop();
            case "kill" -> server.kill();
            default -> {
                sendError(exchange, 400, "Unknown action.");
                return;
            }
        }

        JsonObject out = new JsonObject();
        out.addProperty("message", "Action sent.");
        out.addProperty("server", server.getName());
        out.addProperty("action", action);
        sendJson(exchange, 200, out);
    }

    private JsonObject getServersPayload() {
        JsonObject out = new JsonObject();
        JsonArray servers = new JsonArray();

        RedstoneCloud.getInstance().getServerManager().getServers().values().stream()
                .sorted(Comparator.comparing(Server::getName))
                .forEach(server -> servers.add(JsonParser.parseString(server.toString()).getAsJsonObject()));

        out.addProperty("count", servers.size());
        out.add("servers", servers);
        return out;
    }

    private JsonObject getPlayersPayload() {
        JsonObject out = new JsonObject();
        JsonArray players = new JsonArray();

        RedstoneCloud.getInstance().getPlayerManager().getPlayers().values().stream()
                .sorted(Comparator.comparing(CloudPlayer::getName, String.CASE_INSENSITIVE_ORDER))
                .forEach(player -> players.add(JsonParser.parseString(player.toString()).getAsJsonObject()));

        out.addProperty("count", players.size());
        out.add("players", players);
        return out;
    }

    private JsonObject getTemplatesPayload() {
        JsonObject out = new JsonObject();
        JsonArray templates = new JsonArray();

        RedstoneCloud.getInstance().getServerManager().getTemplates().values().stream()
                .sorted(Comparator.comparing(Template::getName))
                .forEach(template -> {
                    JsonObject item = new JsonObject();
                    item.addProperty("name", template.getName());
                    item.addProperty("type", template.getType().name());
                    item.addProperty("minServers", template.getMinServers());
                    item.addProperty("maxServers", template.getMaxServers());
                    item.addProperty("runningServers", template.getRunningServers());
                    item.addProperty("static", template.isStaticServer());
                    templates.add(item);
                });

        out.addProperty("count", templates.size());
        out.add("templates", templates);
        return out;
    }

    private TokenContext authenticate(HttpExchange exchange, Set<String> requiredPermissions) throws IOException {
        Optional<String> token = extractToken(exchange);
        if (token.isEmpty()) {
            sendError(exchange, 401, "Missing API token.");
            return null;
        }

        TokenContext context = tokenContexts.get(token.get());
        if (context == null) {
            sendError(exchange, 401, "Invalid API token.");
            return null;
        }

        if (requiredPermissions == null || requiredPermissions.isEmpty()) {
            return context;
        }

        Set<String> permissions = context.permissions();
        if (permissions.contains("*") || permissions.containsAll(requiredPermissions)) {
            return context;
        }

        sendError(exchange, 403, "Missing permissions: " + String.join(",", requiredPermissions));
        return null;
    }

    private Optional<String> extractToken(HttpExchange exchange) {
        String authorization = exchange.getRequestHeaders().getFirst("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            String token = authorization.substring("Bearer ".length()).trim();
            if (!token.isEmpty()) {
                return Optional.of(token);
            }
        }

        String plainToken = exchange.getRequestHeaders().getFirst("X-Api-Token");
        if (plainToken == null || plainToken.trim().isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(plainToken.trim());
    }

    private JsonObject parseBody(HttpExchange exchange) throws IOException {
        String payload = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8).trim();
        if (payload.isEmpty()) {
            return new JsonObject();
        }

        try {
            var element = JsonParser.parseString(payload);
            if (!element.isJsonObject()) {
                throw new BadRequestException("Request body must be a JSON object.");
            }
            return element.getAsJsonObject();
        } catch (JsonSyntaxException e) {
            throw new BadRequestException("Malformed JSON in request body.");
        }
    }

    private void sendError(HttpExchange exchange, int statusCode, String message) throws IOException {
        JsonObject out = new JsonObject();
        out.addProperty("status", statusCode);
        out.addProperty("error", message);
        sendJson(exchange, statusCode, out);
    }

    private void sendJson(HttpExchange exchange, int statusCode, JsonObject payload) throws IOException {
        byte[] bytes = gson.toJson(payload).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        exchange.getResponseBody().write(bytes);
    }

    private record TokenContext(String name, Set<String> permissions) {}

    private static final class BadRequestException extends RuntimeException {
        private BadRequestException(String message) {
            super(message);
        }
    }
}
