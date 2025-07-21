package de.redstonecloud.cloud.commands.defaults;

import de.redstonecloud.api.util.EmptyArrays;
import de.redstonecloud.cloud.RedstoneCloud;
import de.redstonecloud.cloud.commands.Command;
import de.redstonecloud.cloud.logger.Logger;
import de.redstonecloud.cloud.server.Server;
import de.redstonecloud.cloud.server.ServerLogger;

public class ExecuteCommand extends Command {
    public int argCount = 1;

    public ExecuteCommand(String cmd) {
        super(cmd);
    }

    @Override
    protected void onCommand(String[] args) {
        if (args.length < 2) {
            Logger.getInstance().error("Usage: execute <server> <command>");
            return;
        }

        Server server = RedstoneCloud.getInstance().getServerManager().getServer(args[0]);
        if (server == null) {
            Logger.getInstance().error("Server not found.");
            return;
        }

        String command = String.join(" ", args)
                .substring(args[0].length() + 1)
                .trim();

        if (command.isEmpty()) {
            Logger.getInstance().error("No command provided to execute.");
            return;
        }

        server.writeConsole(command);
        Logger.getInstance().info("Command executed on server " + server.getName() + ": " + command);
    }

    @Override
    public String[] getArgs() {
        return getServer().getServerManager().getServers().keySet().toArray(EmptyArrays.STRING);
    }
}