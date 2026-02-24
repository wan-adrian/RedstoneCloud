package de.redstonecloud.node.commands.defaults;

import de.redstonecloud.node.RedstoneNode;
import de.redstonecloud.node.commands.Command;
import de.redstonecloud.shared.server.Server;
import de.redstonecloud.shared.utils.CurrentInstance;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class ConsoleCommand extends Command {
    public ConsoleCommand(String cmd) {
        super(cmd);
    }

    @Override
    protected void onCommand(String[] args) {
        if (args.length == 0) {
            log.error("Usage: console <server>");
            return;
        }

        Server server = RedstoneNode.getInstance().getServerManager().getServer(args[0]);
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

}
