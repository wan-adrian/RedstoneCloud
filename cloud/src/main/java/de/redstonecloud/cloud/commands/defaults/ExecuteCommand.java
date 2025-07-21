package de.redstonecloud.cloud.commands.defaults;

import de.redstonecloud.api.util.EmptyArrays;
import de.redstonecloud.cloud.RedstoneCloud;
import de.redstonecloud.cloud.commands.Command;
import de.redstonecloud.cloud.server.Server;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class ExecuteCommand extends Command {
    public int argCount = 1;

    public ExecuteCommand(String cmd) {
        super(cmd);
    }

    @Override
    protected void onCommand(String[] args) {
        if (args.length < 2) {
            log.error("Usage: execute <server> <command>");
            return;
        }

        Server server = RedstoneCloud.getInstance().getServerManager().getServer(args[0]);
        if (server == null) {
            log.error("Server not found.");
            return;
        }

        String command = String.join(" ", args)
                .substring(args[0].length() + 1)
                .trim();

        if (command.isEmpty()) {
            log.error("No command provided to execute.");
            return;
        }

        server.writeConsole(command);
        log.info("Command executed on server " + server.getName() + ": " + command);
    }

    @Override
    public String[] getArgs() {
        return getServer().getServerManager().getServers().keySet().toArray(EmptyArrays.STRING);
    }
}