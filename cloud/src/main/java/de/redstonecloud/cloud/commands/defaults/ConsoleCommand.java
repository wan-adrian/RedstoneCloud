package de.redstonecloud.cloud.commands.defaults;

import de.redstonecloud.api.util.EmptyArrays;
import de.redstonecloud.cloud.RedstoneCloud;
import de.redstonecloud.cloud.commands.Command;
import de.redstonecloud.shared.server.Server;
import de.redstonecloud.shared.startmethods.impl.subprocess.reader.ServerOutReader;
import de.redstonecloud.shared.utils.CurrentInstance;
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

        if(!server.isLocal()) {
            log.error("You can only view the console of local servers.");
            return;
        }

        log.info("Set console to server " + server.getName() + ".");
        server.getStartMethod().enableLogging();
        CurrentInstance.currentLogServer = server;
    }

    @Override
    public String[] getArgs() {
        return getServer().getServerManager().getServers().values().stream().filter(Server::isLocal).map(Server::getName).toArray(String[]::new);
    }
}