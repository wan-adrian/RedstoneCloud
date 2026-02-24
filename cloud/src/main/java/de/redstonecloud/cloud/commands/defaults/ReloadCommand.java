package de.redstonecloud.cloud.commands.defaults;

import de.redstonecloud.cloud.RedstoneCloud;
import de.redstonecloud.cloud.commands.Command;
import de.redstonecloud.shared.commands.CommandCompletion;
import de.redstonecloud.shared.server.Server;
import lombok.extern.log4j.Log4j2;

import java.util.Arrays;

@Log4j2
public class ReloadCommand extends Command {
    public ReloadCommand(String cmd) {
        super(cmd);
        setCompletions(CommandCompletion.root()
                .add(CommandCompletion.literal("templates"))
                .add(CommandCompletion.literal("types")));
    }

    @Override
    protected void onCommand(String[] args) {
        if (args.length == 0) {
            log.error("Usage: reload <templates>");
            return;
        }

        switch(args[0].toLowerCase()) {
            case "templates":
                RedstoneCloud.getInstance().getServerManager().reloadTemplates();
                log.info("Templates reloaded.");
                break;
            case "types":
                RedstoneCloud.getInstance().getServerManager().reloadServerTypes();
                log.info("Types reloaded.");
                break;
            default:
                log.error("Unknown reload target: " + args[0]);
                log.error("Usage: reload <templates|types>");
                break;
        }
    }

}
