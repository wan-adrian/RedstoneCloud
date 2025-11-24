package de.redstonecloud.cloud.commands.defaults;

import de.redstonecloud.api.util.EmptyArrays;
import de.redstonecloud.cloud.RedstoneCloud;
import de.redstonecloud.cloud.commands.Command;
import de.redstonecloud.cloud.server.Server;
import de.redstonecloud.cloud.server.reader.ServerOutReader;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class ConsoleCommand extends Command {
    public int argCount = 1;

    public ConsoleCommand(String cmd) {
        super(cmd);
    }

    @Override
    protected void onCommand(String[] args) {
        if (args.length == 0) {
            log.error("Usage: console <server>");
            return;
        }

        Server server = RedstoneCloud.getInstance().getServerManager().getServer(args[0]);
        if (server == null) {
            log.error("Server not found.");
            return;
        }

        ServerOutReader logger = server.getLogger();

        log.info("Console set to " + server.getName());
        logger.enableConsoleLogging();
        RedstoneCloud.getInstance().setCurrentLogServer(logger);
    }

    @Override
    public String[] getArgs() {
        return getServer().getServerManager().getServers().keySet().toArray(EmptyArrays.STRING);
    }
}