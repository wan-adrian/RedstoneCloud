package de.redstonecloud.cloud.commands.defaults;

import de.redstonecloud.cloud.commands.Command;
import de.redstonecloud.cloud.player.CloudPlayer;
import de.redstonecloud.shared.commands.CommandCompletion;
import de.redstonecloud.shared.commands.CommandExecution;
import lombok.extern.log4j.Log4j2;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Log4j2
public class PlayerCommand extends Command {
    public PlayerCommand(String cmd) {
        super(cmd);
        CommandCompletion completion = CommandCompletion.root();
        completion.add(CommandCompletion.literal("help"));
        completion.add(CommandCompletion.literal("list")
                .then(CommandCompletion.param(CommandCompletion.ParamType.SERVER)));

        CommandCompletion.Node player = CommandCompletion.param(CommandCompletion.ParamType.PLAYER);
        player.then(CommandCompletion.literal("info"));
        player.then(CommandCompletion.literal("connect")
                .then(CommandCompletion.param(CommandCompletion.ParamType.SERVER)));
        player.then(CommandCompletion.literal("kick")
                .then(CommandCompletion.param(CommandCompletion.ParamType.ANY)));
        completion.add(player);

        setCompletions(completion);
    }

    @Override
    public void onCommand(CommandExecution execution) {
        String[] args = execution.args();
        if (args.length < 1) {
            showUsage();
            return;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "help" -> showHelp();
            case "list" -> {
                if (args.length >= 2) {
                    listPlayers(args[1]);
                } else {
                    listPlayers(null);
                }
            }
            default -> handlePlayerAction(args);
        }
    }

    private void showUsage() {
        log.error("Usage: player <help|list|playername>");
    }

    private void showHelp() {
        log.info("Usage: player <help|list|playername> <action>");
        log.info("Usage: player list [server]");
        log.info("Actions:");
        log.info("  info - Show player information");
        log.info("  connect <server> - Connect player to a server");
        log.info("  kick <reason> - Kick player with a reason");
    }

    private void listPlayers(String serverFilter) {
        String filter = serverFilter == null ? "" : serverFilter.trim().toLowerCase(Locale.ROOT);
        List<CloudPlayer> players = getServer().getPlayerManager().getPlayersByName().values().stream()
                .filter(player -> {
                    if (filter.isEmpty()) {
                        return true;
                    }
                    String serverName = player.getConnectedServer() != null ? player.getConnectedServer().getName() : "";
                    String proxyName = player.getConnectedNetwork() != null ? player.getConnectedNetwork().getName() : "";
                    return serverName.equalsIgnoreCase(filter) || proxyName.equalsIgnoreCase(filter);
                })
                .toList();

        if (players.isEmpty()) {
            log.info(filter.isEmpty() ? "No connected players." : "No players found for server/proxy: {}", serverFilter);
            return;
        }

        log.info("Connected Players:");
        for (CloudPlayer player : players) {
            String playerInfo = String.format(" - %s (%s | %s)",
                    player.getName(),
                    player.getConnectedNetwork() != null ? player.getConnectedNetwork().getName() : "---",
                    player.getConnectedServer() != null ? player.getConnectedServer().getName() : "---");
            log.info(playerInfo);
        }
    }

    private void handlePlayerAction(String[] args) {
        CloudPlayer player = findPlayer(args[0]);
        if (player == null) {
            return;
        }

        if (args.length < 2) {
            showPlayerActionUsage();
            return;
        }

        executePlayerAction(player, args);
    }

    private CloudPlayer findPlayer(String playerName) {
        CloudPlayer player = getServer().getPlayerManager().getPlayersByName().get(playerName);
        if (player == null) {
            log.error("Player not found: " + playerName);
        }
        return player;
    }

    private void showPlayerActionUsage() {
        log.error("Usage: player <name> <action>");
        log.info("Available actions: info, connect, kick");
    }

    private void executePlayerAction(CloudPlayer player, String[] args) {
        String action = args[1].toLowerCase();

        switch (action) {
            case "info" -> showPlayerInfo(player);
            case "connect" -> {
                if (args.length < 3) {
                    log.error("Usage: player <name> connect <server>");
                    return;
                }
                String serverName = args[2];
                connectPlayer(player, serverName);
            }

            case "kick" -> {
                if (args.length < 3) {
                    log.error("Usage: player <name> kick <reason>");
                    return;
                }
                String reason = String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length));
                kickPlayer(player, reason);
            }
            default -> showUnknownAction(action);
        }
    }

    private void connectPlayer(CloudPlayer player, String serverName) {        // Placeholder for actual connect logic
        log.info("Connecting player " + player.getName() + " to server " + serverName);
        player.connect(serverName);
    }

    private void kickPlayer(CloudPlayer player, String reason) {        // Placeholder for actual kick logic
        log.info("Kicking player " + player.getName() + " for reason: " + reason);
        player.disconnect(reason);
    }

    private void showPlayerInfo(CloudPlayer player) {
        log.info("Player Information:");
        log.info("Name: " + player.getName());
        log.info("UUID: " + player.getUUID());
        log.info("Address: " + player.getAddress());
        log.info("Server: " + getServerName(player));
        log.info("Proxy: " + getProxyName(player));
    }

    private String getServerName(CloudPlayer player) {
        return player.getConnectedServer() != null ?
                player.getConnectedServer().getName() : "---";
    }

    private String getProxyName(CloudPlayer player) {
        return player.getConnectedNetwork() != null ?
                player.getConnectedNetwork().getName() : "---";
    }

    private void showNotImplemented() {
        log.info("Not implemented yet.");
    }

    private void showUnknownAction(String action) {
        log.error("Unknown action: " + action);
        log.info("Available actions: info, connect, kick");
    }

}
