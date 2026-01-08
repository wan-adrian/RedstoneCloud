package de.redstonecloud.node.commands;

import de.redstonecloud.node.RedstoneNode;
import de.redstonecloud.shared.commands.AbstractCommand;

public abstract class Command extends AbstractCommand {
    public Command(String cmd) {
        super(cmd);
    }

    public RedstoneNode getNode() {
        return RedstoneNode.getInstance();
    }
}
