package de.redstonecloud.cloud.commands;

import de.redstonecloud.cloud.RedstoneCloud;
import de.redstonecloud.shared.commands.AbstractCommand;

public abstract class Command extends AbstractCommand {
    public Command(String cmd) {
        super(cmd);
    }

    public RedstoneCloud getServer() {
        return RedstoneCloud.getInstance();
    }
}
