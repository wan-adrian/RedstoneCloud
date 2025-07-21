package de.redstonecloud.cloud.commands.defaults;

import de.redstonecloud.api.util.EmptyArrays;
import de.redstonecloud.cloud.RedstoneCloud;
import de.redstonecloud.cloud.commands.Command;
import de.redstonecloud.cloud.server.Server;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class InfoCommand extends Command {
    public int argCount = 1;

    public InfoCommand(String cmd) {
        super(cmd);
    }

    @Override
    protected void onCommand(String[] args) {
        if (args.length == 0) {
            log.error("Usage: info <server>");
            return;
        }

        Server server = RedstoneCloud.getInstance().getServerManager().getServer(args[0]);
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

    @Override
    public String[] getArgs() {
        return getServer().getServerManager().getServers().keySet().toArray(EmptyArrays.STRING);
    }
}