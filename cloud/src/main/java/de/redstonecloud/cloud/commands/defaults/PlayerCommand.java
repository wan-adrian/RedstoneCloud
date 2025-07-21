package de.redstonecloud.cloud.commands.defaults;

import de.redstonecloud.cloud.commands.Command;
import de.redstonecloud.cloud.player.CloudPlayer;
import lombok.extern.log4j.Log4j2;

import java.util.ArrayList;
import java.util.List;

@Log4j2
public class PlayerCommand extends Command {
    public int argCount = 1;

    public PlayerCommand(String cmd) {
        super(cmd);
    }

    @Override
    protected void onCommand(String[] args) {
        if (args.length < 1) {
            showUsage();
            return;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "help" -> showHelp();
            case "list" -> listPlayers();
            default -> handlePlayerAction(args);
        }
    }

    private void showUsage() {
        log.error("Usage: player <help|list|playername>");
    }

    private void showHelp() {
        log.info("Usage: player <help|list|playername> <action>");
        log.info("Actions:");
        log.info("  info - Show player information");
        log.info("  connect <server> - Connect player to a server");
        log.info("  kick <reason> - Kick player with a reason");
    }

    private void listPlayers() {
        log.info("Connected Players:");
        getServer().getPlayerManager().getPlayersByName().entrySet().forEach(data -> {
            String playerInfo = String.format(" - %s (%s | %s)",
                    data.getKey(),
                    data.getValue().getConnectedNetwork().getName(),
                    data.getValue().getConnectedServer().getName());
            log.info(playerInfo);
        });
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
            case "connect", "kick" -> showNotImplemented();
            default -> showUnknownAction(action);
        }
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

    @Override
    public String[] getArgs() {
        List<String> players = getServer().getPlayerManager().getPlayers()
                .entrySet().stream()
                .map(entry -> entry.getValue().getName())
                .toList();

        List<String> cmds = new ArrayList<>();
        cmds.add("help");
        cmds.add("list");
        cmds.addAll(players);

        return cmds.toArray(String[]::new);
    }
}