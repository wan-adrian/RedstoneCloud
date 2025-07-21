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
            Logger.getInstance().error("Usage: player <help|list|playername>");
            return;
        }

        if(args[0].equalsIgnoreCase("help")) {
            Logger.getInstance().info("Usage: player <help|list|playername> <action>");
            Logger.getInstance().info("Actions:");
            Logger.getInstance().info("  info - Show player information");
            Logger.getInstance().info("  connect <server> - Connect player to a server");
            Logger.getInstance().info("  kick <reason> - Kick player with a reason");
            return;
        }

        if(args[0].equalsIgnoreCase("list")) {
            Logger.getInstance().info("Connected Players:");
            getServer().getPlayerManager().getPlayersByName().entrySet().forEach(data -> {
                Logger.getInstance().info(" - " + data.getKey() + " (" + data.getValue().getConnectedNetwork().getName() + " | " + data.getValue().getConnectedServer().getName() + ")");
            });
            return;
        }

        CloudPlayer player = getServer().getPlayerManager().getPlayersByName().get(args[0]);
        if (player == null) {
            Logger.getInstance().error("Player not found: " + args[0]);
            return;
        }

        if (args.length < 2) {
            Logger.getInstance().error("Usage: player <name> <action>");
            Logger.getInstance().info("Available actions: info, connect, kick");
            return;
        }

        switch(args[1].toLowerCase()) {
            case "info" -> {
                Logger.getInstance().info("Player Information:");
                Logger.getInstance().info("Name: " + player.getName());
                Logger.getInstance().info("UUID: " + player.getUUID());
                Logger.getInstance().info("Address: " + player.getAddress());
                Logger.getInstance().info("Server: " + (player.getConnectedServer() != null ? player.getConnectedServer().getName() : "---"));
                Logger.getInstance().info("Proxy: " + (player.getConnectedNetwork() != null ? player.getConnectedNetwork().getName() : "---"));
            }

            /*
            case "connect" -> {
                if(args.length < 3) {
                    Logger.getInstance().error("Usage: player <name> connect <server>");
                    return;
                }

                player.connect(args[2]);
            }

            case "kick" -> {
                if(args.length < 3) {
                    Logger.getInstance().error("Usage: player <name> kick <reason>");
                    return;
                }

                String reason = String.join(" ", args)
                        .substring(args[0].length() + args[1].length() + 2)
                        .trim();

                player.disconnect(reason);
            }*/

            case "connect", "kick" -> {
                Logger.getInstance().info("Not implemented yet.");
            }

            default -> {
                Logger.getInstance().error("Unknown action: " + args[1]);
                Logger.getInstance().info("Available actions: info, connect, kick");
            }
        }
    }

    @Override
    public String[] getArgs() {
        List<String> players = getServer().getPlayerManager().getPlayers().entrySet().stream().map(m -> m.getValue().getName()).toList();
        List<String> cmds = new ArrayList();
        cmds.add("help");
        cmds.add("list");
        cmds.addAll(players);

        return cmds.toArray(String[]::new);
    }
}