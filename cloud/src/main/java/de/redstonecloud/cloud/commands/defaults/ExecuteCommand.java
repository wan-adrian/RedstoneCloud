package de.redstonecloud.cloud.commands.defaults;

import de.redstonecloud.cloud.RedstoneCloud;
import de.redstonecloud.cloud.commands.Command;
import de.redstonecloud.shared.commands.CommandCompletion;
import de.redstonecloud.shared.commands.CommandExecution;
import de.redstonecloud.shared.server.Server;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ExecuteCommand extends Command {
    public ExecuteCommand(String cmd) {
        super(cmd);
        setCompletions(CommandCompletion.root()
                .add(CommandCompletion.param(CommandCompletion.ParamType.SERVER, "server")));
    }

    @Override
    public void onCommand(CommandExecution execution) {
        if (execution.positionals().size() < 2) {
            log.error("Usage: execute <server> <command>");
            return;
        }

        String serverName = execution.value("server");
        if (serverName == null || serverName.isBlank()) {
            serverName = execution.positional(0);
        }
        Server server = RedstoneCloud.getInstance().getServerManager().getServer(serverName);
        if (server == null) {
            log.error("Server not found.");
            return;
        }

        String command = execution.joinFromRaw(1).trim();

        if (command.isEmpty()) {
            log.error("No command provided to execute.");
            return;
        }

        server.writeConsole(command);
        log.info("Command executed on server " + server.getName() + ": " + command);
    }

}
