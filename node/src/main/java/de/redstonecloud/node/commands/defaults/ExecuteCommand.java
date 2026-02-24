package de.redstonecloud.node.commands.defaults;

import de.redstonecloud.node.RedstoneNode;
import de.redstonecloud.node.commands.Command;
import de.redstonecloud.shared.server.Server;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ExecuteCommand extends Command {
    public ExecuteCommand(String cmd) {
        super(cmd);
    }

    @Override
    protected void onCommand(String[] args) {
        if (args.length < 2) {
            log.error("Usage: execute <server> <command>");
            return;
        }

        Server server = RedstoneNode.getInstance().getServerManager().getServer(args[0]);
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

}
