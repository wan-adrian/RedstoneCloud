package de.redstonecloud.cloud.commands.defaults;

import de.redstonecloud.cloud.commands.Command;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class EndCommand extends Command {
    public EndCommand(String cmd) {
        super(cmd);
    }

    @Override
    protected void onCommand(String[] args) {
        log.info("Stopping Cloud using command...");
        this.getServer().stop();
    }
}