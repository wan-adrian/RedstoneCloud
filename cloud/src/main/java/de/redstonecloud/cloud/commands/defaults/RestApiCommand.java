package de.redstonecloud.cloud.commands.defaults;

import de.redstonecloud.cloud.RedstoneCloud;
import de.redstonecloud.cloud.commands.Command;
import de.redstonecloud.cloud.config.entires.RestApiSettings;
import de.redstonecloud.cloud.config.entires.RestApiToken;
import de.redstonecloud.shared.commands.CommandCompletion;
import lombok.extern.log4j.Log4j2;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Log4j2
public class RestApiCommand extends Command {
    private static final SecureRandom RANDOM = new SecureRandom();

    public RestApiCommand(String cmd) {
        super(cmd);
        CommandCompletion completion = CommandCompletion.root();

        completion.add(CommandCompletion.literal("status"));
        completion.add(CommandCompletion.literal("help"));

        CommandCompletion.Node token = CommandCompletion.literal("token");
        token.then(CommandCompletion.literal("list"));
        token.then(CommandCompletion.literal("add")
                .then(CommandCompletion.param(CommandCompletion.ParamType.ANY)));
        token.then(CommandCompletion.literal("rotate")
                .then(CommandCompletion.param(CommandCompletion.ParamType.REST_TOKEN)));
        token.then(CommandCompletion.literal("remove")
                .then(CommandCompletion.param(CommandCompletion.ParamType.REST_TOKEN)));
        token.then(CommandCompletion.literal("enable")
                .then(CommandCompletion.param(CommandCompletion.ParamType.REST_TOKEN)));
        token.then(CommandCompletion.literal("disable")
                .then(CommandCompletion.param(CommandCompletion.ParamType.REST_TOKEN)));
        token.then(CommandCompletion.literal("show")
                .then(CommandCompletion.param(CommandCompletion.ParamType.REST_TOKEN)));
        completion.add(token);

        CommandCompletion.Node perm = CommandCompletion.literal("perm");
        perm.then(CommandCompletion.literal("list")
                .then(CommandCompletion.param(CommandCompletion.ParamType.REST_TOKEN)));
        perm.then(CommandCompletion.literal("add")
                .then(CommandCompletion.param(CommandCompletion.ParamType.REST_TOKEN)
                        .then(CommandCompletion.param(CommandCompletion.ParamType.PERMISSION))));
        perm.then(CommandCompletion.literal("remove")
                .then(CommandCompletion.param(CommandCompletion.ParamType.REST_TOKEN)
                        .then(CommandCompletion.param(CommandCompletion.ParamType.PERMISSION))));
        completion.add(perm);

        CommandCompletion.Node permission = CommandCompletion.literal("permission");
        permission.then(CommandCompletion.literal("list")
                .then(CommandCompletion.param(CommandCompletion.ParamType.REST_TOKEN)));
        permission.then(CommandCompletion.literal("add")
                .then(CommandCompletion.param(CommandCompletion.ParamType.REST_TOKEN)
                        .then(CommandCompletion.param(CommandCompletion.ParamType.PERMISSION))));
        permission.then(CommandCompletion.literal("remove")
                .then(CommandCompletion.param(CommandCompletion.ParamType.REST_TOKEN)
                        .then(CommandCompletion.param(CommandCompletion.ParamType.PERMISSION))));
        completion.add(permission);

        setCompletions(completion);
    }

    @Override
    protected void onCommand(String[] args) {
        if (args.length == 0 || "help".equalsIgnoreCase(args[0])) {
            showHelp();
            return;
        }

        if (!getServer().getConfig().restApi().enabled()) {
            log.error("REST API is disabled in config. Enable RestAPI first.");
            return;
        }

        String section = args[0].toLowerCase();
        switch (section) {
            case "status" -> showStatus();
            case "token" -> handleToken(args);
            case "perm", "permission" -> handlePermission(args);
            default -> {
                log.error("Unknown subcommand: {}", args[0]);
                showHelp();
            }
        }
    }

    private void showHelp() {
        log.info("Usage:");
        log.info("  restapi status");
        log.info("  restapi token list");
        log.info("  restapi token add <name> [token]");
        log.info("  restapi token rotate <name>");
        log.info("  restapi token remove <name>");
        log.info("  restapi token enable <name>");
        log.info("  restapi token disable <name>");
        log.info("  restapi token show <name>");
        log.info("  restapi perm list <name>");
        log.info("  restapi perm add <name> <permission>");
        log.info("  restapi perm remove <name> <permission>");
    }

    private void showStatus() {
        RestApiSettings settings = getServer().getConfig().restApi();
        long enabledTokens = settings.tokens().stream().filter(RestApiToken::enabled).count();
        log.info("REST API status:");
        log.info("  enabled: {}", settings.enabled());
        log.info("  bind: {}:{}", settings.host(), settings.port());
        log.info("  configured tokens: {}", settings.tokens().size());
        log.info("  enabled tokens: {}", enabledTokens);
    }

    private void handleToken(String[] args) {
        if (args.length < 2) {
            log.error("Usage: restapi token <list|add|rotate|remove|enable|disable|show>");
            return;
        }

        String action = args[1].toLowerCase();
        switch (action) {
            case "list" -> listTokens();
            case "add" -> addToken(args);
            case "rotate" -> rotateToken(args);
            case "remove" -> removeToken(args);
            case "enable" -> setTokenEnabled(args, true);
            case "disable" -> setTokenEnabled(args, false);
            case "show" -> showToken(args);
            default -> log.error("Unknown token action: {}", args[1]);
        }
    }

    private void handlePermission(String[] args) {
        if (args.length < 3) {
            log.error("Usage: restapi perm <list|add|remove> <tokenName> [permission]");
            return;
        }

        String action = args[1].toLowerCase();
        RestApiToken token = findToken(args[2]);
        if (token == null) {
            log.error("Token not found: {}", args[2]);
            return;
        }

        switch (action) {
            case "list" -> {
                log.info("Permissions for {}: {}", token.name(), token.permissions());
            }
            case "add" -> {
                if (args.length < 4) {
                    log.error("Usage: restapi perm add <tokenName> <permission>");
                    return;
                }
                String permission = args[3].trim();
                if (permission.isEmpty()) {
                    log.error("Permission cannot be empty.");
                    return;
                }
                if (!token.permissions().contains(permission)) {
                    token.permissions().add(permission);
                    persistAndReload();
                }
                log.info("Permission '{}' added to token '{}'.", permission, token.name());
            }
            case "remove" -> {
                if (args.length < 4) {
                    log.error("Usage: restapi perm remove <tokenName> <permission>");
                    return;
                }
                String permission = args[3].trim();
                if (!token.permissions().remove(permission)) {
                    log.error("Permission '{}' not found on token '{}'.", permission, token.name());
                    return;
                }
                persistAndReload();
                log.info("Permission '{}' removed from token '{}'.", permission, token.name());
            }
            default -> log.error("Unknown permission action: {}", args[1]);
        }
    }

    private void listTokens() {
        List<RestApiToken> tokens = getServer().getConfig().restApi().tokens();
        if (tokens.isEmpty()) {
            log.info("No REST API tokens configured.");
            return;
        }

        log.info("Configured REST API tokens:");
        for (RestApiToken token : tokens) {
            log.info("  - {} | enabled={} | permissions={}", token.name(), token.enabled(), token.permissions());
        }
    }

    private void addToken(String[] args) {
        if (args.length < 3) {
            log.error("Usage: restapi token add <name> [token]");
            return;
        }

        String name = args[2].trim();
        if (name.isEmpty()) {
            log.error("Token name cannot be empty.");
            return;
        }
        if (findToken(name) != null) {
            log.error("Token '{}' already exists.", name);
            return;
        }

        String value = args.length >= 4 ? args[3].trim() : generateToken();
        if (value.isEmpty()) {
            log.error("Token value cannot be empty.");
            return;
        }

        RestApiToken newToken = new RestApiToken();
        newToken.name(name);
        newToken.token(value);
        newToken.permissions(new ArrayList<>(List.of("cloud.read")));
        newToken.enabled(true);

        getServer().getConfig().restApi().tokens().add(newToken);
        persistAndReload();

        log.info("Token '{}' created.", name);
        log.info("Token value: {}", value);
    }

    private void rotateToken(String[] args) {
        if (args.length < 3) {
            log.error("Usage: restapi token rotate <name>");
            return;
        }

        RestApiToken token = findToken(args[2]);
        if (token == null) {
            log.error("Token not found: {}", args[2]);
            return;
        }

        String value = generateToken();
        token.token(value);
        persistAndReload();

        log.info("Token '{}' rotated.", token.name());
        log.info("New token value: {}", value);
    }

    private void removeToken(String[] args) {
        if (args.length < 3) {
            log.error("Usage: restapi token remove <name>");
            return;
        }

        RestApiToken token = findToken(args[2]);
        if (token == null) {
            log.error("Token not found: {}", args[2]);
            return;
        }

        getServer().getConfig().restApi().tokens().remove(token);
        persistAndReload();
        log.info("Token '{}' removed.", token.name());
    }

    private void setTokenEnabled(String[] args, boolean enabled) {
        if (args.length < 3) {
            log.error("Usage: restapi token {} <name>", enabled ? "enable" : "disable");
            return;
        }

        RestApiToken token = findToken(args[2]);
        if (token == null) {
            log.error("Token not found: {}", args[2]);
            return;
        }

        token.enabled(enabled);
        persistAndReload();
        log.info("Token '{}' {}.", token.name(), enabled ? "enabled" : "disabled");
    }

    private void showToken(String[] args) {
        if (args.length < 3) {
            log.error("Usage: restapi token show <name>");
            return;
        }

        RestApiToken token = findToken(args[2]);
        if (token == null) {
            log.error("Token not found: {}", args[2]);
            return;
        }

        log.info("Token '{}':", token.name());
        log.info("  enabled: {}", token.enabled());
        log.info("  permissions: {}", token.permissions());
        log.info("  value: {}", token.token());
    }

    private RestApiToken findToken(String name) {
        return getServer().getConfig().restApi().tokens().stream()
                .filter(token -> token.name() != null && token.name().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    private void persistAndReload() {
        getServer().getConfig().save();
        if (getServer().getRestApiService() != null) {
            getServer().getRestApiService().reloadTokens();
        }
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

}
