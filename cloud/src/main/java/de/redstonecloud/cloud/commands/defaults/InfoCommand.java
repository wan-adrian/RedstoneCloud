package de.redstonecloud.cloud.commands.defaults;

import de.redstonecloud.cloud.RedstoneCloud;
import de.redstonecloud.cloud.commands.Command;
import de.redstonecloud.shared.commands.CommandCompletion;
import de.redstonecloud.shared.commands.CommandExecution;
import de.redstonecloud.shared.server.Server;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class InfoCommand extends Command {
    public InfoCommand(String cmd) {
        super(cmd);
        setCompletions(CommandCompletion.root()
                .add(CommandCompletion.param(CommandCompletion.ParamType.SERVER, "server")));
    }

    @Override
    public void onCommand(CommandExecution execution) {
        if (execution.positionals().isEmpty()) {
            log.error("Usage: info <server>");
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

        log.info("== SERVER INFO: " + server.getName() + " ==");
        log.info("Server Name: " + server.getName());
        log.info("Server Template: " + server.getTemplate().getName());
        log.info("Server Type: " + server.getType().name());
        log.info("Server Status: " + server.getStatus());
        log.info("Server Port: " + server.getPort());
    }

}
