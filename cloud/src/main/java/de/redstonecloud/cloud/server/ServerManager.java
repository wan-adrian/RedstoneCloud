package de.redstonecloud.cloud.server;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import de.redstonecloud.api.components.ServerStatus;
import de.redstonecloud.cloud.RedstoneCloud;
import de.redstonecloud.cloud.events.defaults.ServerCreateEvent;
import de.redstonecloud.cloud.events.defaults.ServerStartEvent;
import de.redstonecloud.cloud.utils.Directories;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Manages server lifecycle, templates, and server types.
 * Singleton pattern ensuring single instance across application.
 */
@Getter
@Log4j2
public class ServerManager {

    private static final Gson GSON = new Gson();
    private static final int DEFAULT_SHUTDOWN_TIME_MS = 5000;
    private static final int MIN_PORT = 10000;
    private static final int MAX_PORT = 50000;

    private static volatile ServerManager INSTANCE;

    private final Object2ObjectOpenHashMap<String, ServerType> types = new Object2ObjectOpenHashMap<>();
    private final Object2ObjectOpenHashMap<String, Template> templates = new Object2ObjectOpenHashMap<>();
    private final ConcurrentHashMap<String, Server> servers = new ConcurrentHashMap<>();

    /**
     * Gets the singleton instance of ServerManager.
     * Thread-safe double-checked locking implementation.
     *
     * @return the ServerManager instance
     */
    public static ServerManager getInstance() {
        if (INSTANCE == null) {
            synchronized (ServerManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new ServerManager();
                }
            }
        }
        return INSTANCE;
    }

    private ServerManager() {
        log.info("Initializing ServerManager");
        loadServerTypes();
        loadTemplates();
        log.info("ServerManager initialized with {} types and {} templates",
                types.size(), templates.size());
    }

    /**
     * Loads all template configurations from the template_configs directory.
     */
    private void loadTemplates() {
        Path templatesDir = Directories.TEMPLATE_CONFIGS_DIR.toPath();

        try {
            Files.createDirectories(templatesDir);

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(templatesDir, "*.json")) {
                for (Path file : stream) {
                    loadTemplate(file);
                }
            }

            log.info("Loaded {} templates", templates.size());
        } catch (IOException e) {
            log.error("Failed to load templates from directory: {}", templatesDir, e);
        }
    }

    private void loadTemplate(Path file) {
        try {
            String content = Files.readString(file, StandardCharsets.UTF_8);
            JsonObject data = GSON.fromJson(content, JsonObject.class);

            String name = data.get("name").getAsString();
            String typeName = data.get("type").getAsString();
            ServerType type = types.get(typeName);

            if (type == null) {
                log.error("Template {} references unknown server type: {}", name, typeName);
                return;
            }

            Template template = Template.builder()
                    .name(name)
                    .type(type)
                    .maxPlayers(data.get("maxPlayers").getAsInt())
                    .minServers(data.get("minServers").getAsInt())
                    .maxServers(data.get("maxServers").getAsInt())
                    .staticServer(data.get("staticServer").getAsBoolean())
                    .shutdownTimeMs(data.has("shutdownTimeMs")
                            ? data.get("shutdownTimeMs").getAsInt()
                            : DEFAULT_SHUTDOWN_TIME_MS)
                    .maxBootTimeMs(data.has("maxBootTimeMs")
                            ? data.get("maxBootTimeMs").getAsInt()
                            : 60*1000)
                    .stopOnEmpty(data.has("stopOnEmpty") && data.get("stopOnEmpty").getAsBoolean())
                    .seperator(data.has("seperator") ? data.get("seperator").getAsString() : "-")
                    .build();

            templates.put(name, template);
            log.debug("Loaded template: {}", name);

        } catch (IOException e) {
            log.error("Failed to read template file: {}", file.getFileName(), e);
        } catch (Exception e) {
            log.error("Failed to parse template file: {}", file.getFileName(), e);
        }
    }

    /**
     * Loads all server type configurations from the types directory.
     */
    private void loadServerTypes() {
        Path typesDir = Directories.TYPES_DIR.toPath();
        log.info(typesDir.toUri().toString());

        try {
            Files.createDirectories(typesDir);

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(typesDir, "*.json")) {
                for (Path file : stream) {
                    loadServerType(file);
                }
            }

            log.info("Loaded {} server types", types.size());
        } catch (IOException e) {
            log.error("Failed to load server types from directory: {}", typesDir, e);
        }
    }

    private void loadServerType(Path file) {
        try {
            String content = Files.readString(file, StandardCharsets.UTF_8);
            JsonObject data = GSON.fromJson(content, JsonObject.class);

            String name = data.get("name").getAsString();
            JsonArray startCommandArray = data.getAsJsonArray("startCommand");
            String[] startCommand = new String[startCommandArray.size()];

            for (int i = 0; i < startCommandArray.size(); i++) {
                startCommand[i] = startCommandArray.get(i).getAsString();
            }

            ServerType serverType = new ServerType(
                    name,
                    startCommand,
                    data.get("isProxy").getAsBoolean(),
                    data.get("logsPath").isJsonNull() ? null : data.get("logsPath").getAsString(),
                    data.get("portSettingFile").getAsString(),
                    data.get("portSettingPlaceholder").getAsString(),
                    !data.has("stopCommand") ? "stop" : data.get("stopCommand").getAsString()
            );

            types.put(name, serverType);
            log.debug("Loaded server type: {}", name);

        } catch (IOException e) {
            log.error("Failed to read server type file: {}", file.getFileName(), e);
        } catch (Exception e) {
            log.error("Failed to parse server type file: {}", file.getFileName(), e);
        }
    }

    /**
     * Removes a server from the manager.
     *
     * @param server the server to remove
     */
    public void remove(Server server) {
        if (server == null) {
            log.warn("Attempted to remove null server");
            return;
        }

        servers.remove(server.getName().toUpperCase());
        log.debug("Removed server: {}", server.getName());
    }

    /**
     * Adds a server to the manager.
     *
     * @param server the server to add
     */
    public void add(Server server) {
        if (server == null) {
            log.warn("Attempted to add null server");
            return;
        }

        servers.put(server.getName().toUpperCase(), server);
        log.debug("Added server: {}", server.getName());
    }

    /**
     * Gets a server by name (case-insensitive).
     *
     * @param name the server name
     * @return the server, or null if not found
     */
    public Server getServer(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        return servers.get(name.toUpperCase());
    }

    /**
     * Gets a template by name.
     *
     * @param name the template name
     * @return the template, or null if not found
     */
    public Template getTemplate(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        return templates.get(name);
    }

    /**
     * Starts a new server with auto-generated ID.
     *
     * @param template the template to use
     * @return the created server, or null if cancelled
     */
    public Server startServer(Template template) {
        return startServer(template, null);
    }

    /**
     * Starts a new server with specified or auto-generated ID.
     *
     * @param template the template to use
     * @param id the server ID, or null for auto-generation
     * @return the created server, or null if cancelled
     */
    public Server startServer(Template template, Integer id) {
        if (template == null) {
            log.error("Cannot start server: template is null");
            return null;
        }

        log.info("Creating server from template: {}", template.getName());

        // Build server instance
        Server server = Server.builder()
                .template(template)
                .uuid(UUID.randomUUID())
                .createdAt(System.currentTimeMillis())
                .type(template.getType())
                .port(generateRandomPort())
                .build();

        server.initName(id);

        // Fire creation event
        ServerCreateEvent event = RedstoneCloud.getInstance()
                .getEventManager()
                .callEvent(new ServerCreateEvent(server));

        if (event.isCancelled()) {
            log.info("Server creation cancelled by event for template: {}", template.getName());
            return null;
        }

        // Prepare and register server
        server.prepare();
        add(server);
        template.setRunningServers(template.getRunningServers() + 1);

        // Schedule server start
        RedstoneCloud.getInstance().getScheduler().scheduleDelayedTask(() -> {
            server.start();
            RedstoneCloud.getInstance()
                    .getEventManager()
                    .callEvent(new ServerStartEvent(server));
        }, TimeUnit.SECONDS, 1);

        log.info("Server {} created and scheduled to start", server.getName());
        return server;
    }

    private int generateRandomPort() {
        return ThreadLocalRandom.current().nextInt(MIN_PORT, MAX_PORT + 1);
    }

    /**
     * Stops all running servers.
     * Blocks until all servers have stopped or timeout occurs.
     *
     * @return true if all servers stopped successfully
     */
    public boolean stopAll() {
        if (servers.isEmpty()) {
            log.info("No servers to stop");
            return true;
        }

        log.info("Stopping all {} servers", servers.size());

        // Create snapshot to avoid concurrent modification
        List<Server> serverList = new ArrayList<>(servers.values());

        // Create futures for all stop operations
        List<CompletableFuture<Void>> stopFutures = serverList.stream()
                .map(server -> CompletableFuture.runAsync(() -> server.kill()))
                .collect(Collectors.toList());

        try {
            // Wait for all servers to stop
            CompletableFuture.allOf(stopFutures.toArray(new CompletableFuture[0]))
                    .get(60, TimeUnit.SECONDS);

            log.info("All servers stopped successfully");
            return true;

        } catch (TimeoutException e) {
            log.error("Timeout while stopping servers - some servers may still be running");
            return false;
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error while stopping servers", e);
            return false;
        }
    }

    /**
     * Gets all servers using a specific template.
     *
     * @param template the template to filter by
     * @return array of matching servers
     */
    public Server[] getServersByTemplate(Template template) {
        if (template == null) {
            return new Server[0];
        }

        return servers.values().stream()
                .filter(server -> template.equals(server.getTemplate()))
                .toArray(Server[]::new);
    }

    /**
     * Gets all servers of a specific type.
     *
     * @param type the server type to filter by
     * @return array of matching servers
     */
    public Server[] getServersByType(ServerType type) {
        if (type == null) {
            return new Server[0];
        }

        return servers.values().stream()
                .filter(server -> type.equals(server.getType()))
                .toArray(Server[]::new);
    }

    /**
     * Finds the best servers for a template based on available slots.
     * Returns servers sorted by free slots (fewest free slots first).
     *
     * @param template the template to search for
     * @return array of best server results sorted by free slots
     */
    public BestServerResult[] getBestServer(Template template) {
        if (template == null) {
            return new BestServerResult[0];
        }

        return servers.values().stream()
                .filter(server -> template.equals(server.getTemplate()))
                .filter(server -> server.getStatus() == ServerStatus.RUNNING)
                .filter(server -> server.getPlayers().size() < template.getMaxPlayers())
                .map(server -> new BestServerResult(
                        server,
                        template.getMaxPlayers() - server.getPlayers().size()
                ))
                .sorted(Comparator.comparingInt(BestServerResult::freeSlots))
                .toArray(BestServerResult[]::new);
    }

    /**
     * Calculates total free slots across all running servers for a template.
     *
     * @param template the template to calculate for
     * @return total number of free slots
     */
    public int getTemplateFreeSlots(Template template) {
        if (template == null) {
            return 0;
        }

        return servers.values().stream()
                .filter(server -> template.equals(server.getTemplate()))
                .filter(server -> server.getStatus() == ServerStatus.RUNNING)
                .mapToInt(server -> template.getMaxPlayers() - server.getPlayers().size())
                .sum();
    }

    /**
     * Gets the total number of running servers.
     *
     * @return count of running servers
     */
    public int getRunningServerCount() {
        return (int) servers.values().stream()
                .filter(server -> server.getStatus() == ServerStatus.RUNNING)
                .count();
    }

    /**
     * Gets all currently managed servers.
     *
     * @return collection of all servers
     */
    public Collection<Server> getAllServers() {
        return new ArrayList<>(servers.values());
    }

    /**
     * Result of finding the best server for load balancing.
     *
     * @param server the server instance
     * @param freeSlots number of available player slots
     */
    public record BestServerResult(Server server, int freeSlots) {
    }
}