package de.redstonecloud.cloud.commands.defaults;

import de.redstonecloud.cloud.RedstoneCloud;
import de.redstonecloud.cloud.commands.Command;
import de.redstonecloud.shared.commands.CommandCompletion;
import de.redstonecloud.shared.commands.CommandExecution;
import de.redstonecloud.shared.server.Server;
import lombok.extern.log4j.Log4j2;

import java.util.Arrays;

@Log4j2
public class KillCommand extends Command {
    public KillCommand(String cmd) {
        super(cmd);
        setCompletions(CommandCompletion.root()
                .add(CommandCompletion.param(CommandCompletion.ParamType.SERVER, "server")));
    }

    @Override
    public void onCommand(CommandExecution execution) {
        if (execution.positionals().isEmpty()) {
            log.error("Usage: kill <server>");
            return;
        }

        String serverN = execution.value("server");
        if (serverN == null || serverN.isBlank()) {
            serverN = execution.positional(0);
        }
        final String serverName = serverN;
        Server server = RedstoneCloud.getInstance().getServerManager().getServer(serverName);
        if (server == null) {
            if (serverName.endsWith("*")) {
                Server[] servers = RedstoneCloud.getInstance().getServerManager().getServers().values().toArray(new Server[0]);
                Server[] affectedServers = Arrays.stream(servers).filter(s -> s.getName().startsWith(serverName.substring(0, serverName.length() - 1))).toArray(Server[]::new);

                for (Server s : affectedServers) {
                    s.kill();
                }

                log.info("Stopped " + affectedServers.length + " servers");
                return;
            }


            log.error("Server not found.");
            return;
        }

        server.kill();
    }

}
