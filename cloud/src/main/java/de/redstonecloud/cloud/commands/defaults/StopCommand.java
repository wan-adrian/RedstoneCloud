package de.redstonecloud.cloud.commands.defaults;

import de.redstonecloud.api.util.EmptyArrays;
import de.redstonecloud.cloud.RedstoneCloud;
import de.redstonecloud.cloud.commands.Command;
import de.redstonecloud.cloud.server.Server;
import lombok.extern.log4j.Log4j2;

import java.util.Arrays;

@Log4j2
public class StopCommand extends Command {
    public int argCount = 1;

    public StopCommand(String cmd) {
        super(cmd);
    }

    @Override
    protected void onCommand(String[] args) {
        if (args.length == 0) {
            log.error("Usage: stop <server>");
            return;
        }

        Server server = RedstoneCloud.getInstance().getServerManager().getServer(args[0]);
        if (server == null) {
            if (args[0].endsWith("*")) {
                Server[] servers = RedstoneCloud.getInstance().getServerManager().getServers().values().toArray(new Server[0]);
                Server[] affectedServers = Arrays.stream(servers).filter(s -> s.getName().startsWith(args[0].substring(0, args[0].length() - 1))).toArray(Server[]::new);

                for (Server s : affectedServers) {
                    s.stop();
                }

                log.info("Stopped " + affectedServers.length + " servers");
                return;
            }


            log.error("Server not found.");
            return;
        }

        server.stop();
    }

    @Override
    public String[] getArgs() {
        return getServer().getServerManager().getServers().keySet().toArray(EmptyArrays.STRING);
    }
}