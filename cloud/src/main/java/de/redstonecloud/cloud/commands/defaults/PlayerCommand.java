package de.redstonecloud.cloud.commands.defaults;

import de.redstonecloud.api.util.EmptyArrays;
import de.redstonecloud.cloud.RedstoneCloud;
import de.redstonecloud.cloud.commands.Command;
import de.redstonecloud.cloud.logger.Logger;
import de.redstonecloud.cloud.player.CloudPlayer;
import de.redstonecloud.cloud.server.Server;

import java.util.ArrayList;
import java.util.List;

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
        Logger.getInstance().error("Usage: player <help|list|playername>");
    }

    private void showHelp() {
        Logger.getInstance().info("Usage: player <help|list|playername> <action>");
        Logger.getInstance().info("Actions:");
        Logger.getInstance().info("  info - Show player information");
        Logger.getInstance().info("  connect <server> - Connect player to a server");
        Logger.getInstance().info("  kick <reason> - Kick player with a reason");
    }

    private void listPlayers() {
        Logger.getInstance().info("Connected Players:");
        getServer().getPlayerManager().getPlayersByName().entrySet().forEach(data -> {
            String playerInfo = String.format(" - %s (%s | %s)",
                    data.getKey(),
                    data.getValue().getConnectedNetwork().getName(),
                    data.getValue().getConnectedServer().getName());
            Logger.getInstance().info(playerInfo);
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
            Logger.getInstance().error("Player not found: " + playerName);
        }
        return player;
    }

    private void showPlayerActionUsage() {
        Logger.getInstance().error("Usage: player <name> <action>");
        Logger.getInstance().info("Available actions: info, connect, kick");
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
        Logger.getInstance().info("Player Information:");
        Logger.getInstance().info("Name: " + player.getName());
        Logger.getInstance().info("UUID: " + player.getUUID());
        Logger.getInstance().info("Address: " + player.getAddress());
        Logger.getInstance().info("Server: " + getServerName(player));
        Logger.getInstance().info("Proxy: " + getProxyName(player));
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
        Logger.getInstance().info("Not implemented yet.");
    }

    private void showUnknownAction(String action) {
        Logger.getInstance().error("Unknown action: " + action);
        Logger.getInstance().info("Available actions: info, connect, kick");
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