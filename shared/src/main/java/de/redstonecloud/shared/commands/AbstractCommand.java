package de.redstonecloud.shared.commands;

import de.redstonecloud.shared.commands.CommandCompletion;

public abstract class AbstractCommand {
    public String cmd;
    private CommandCompletion completions;

    public AbstractCommand(String cmd) {
        this.cmd = cmd;
    }

    protected abstract void onCommand(String[] args);

    public String getCommand() {
        return this.cmd;
    }

    public CommandCompletion getCompletions() {
        return completions;
    }

    protected final void setCompletions(CommandCompletion completions) {
        this.completions = completions;
    }

    protected final CommandArgs parseArgs(String[] args) {
        return CommandArgs.parse(args);
    }
}
