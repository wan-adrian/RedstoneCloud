package de.redstonecloud.shared.commands;

import de.redstonecloud.api.util.EmptyArrays;

public abstract class AbstractCommand {
    public String cmd;
    public int argCount = 0;

    public AbstractCommand(String cmd) {
        this.cmd = cmd;
    }

    protected abstract void onCommand(String[] args);

    public String getCommand() {
        return this.cmd;
    }

    public String[] getArgs() {
        return EmptyArrays.STRING;
    }
}