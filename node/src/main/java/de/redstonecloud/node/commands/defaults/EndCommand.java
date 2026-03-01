package de.redstonecloud.node.commands.defaults;

import de.redstonecloud.node.commands.Command;
import de.redstonecloud.shared.commands.CommandCompletion;
import de.redstonecloud.shared.commands.CommandExecution;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class EndCommand extends Command {
    public EndCommand(String cmd) {
        super(cmd);
        setCompletions(CommandCompletion.root());
    }

    @Override
    public void onCommand(CommandExecution execution) {
        log.info("Stopping Node using command...");
        this.getNode().shutdown();
    }
}
