package de.redstonecloud.shared.commands;

public abstract class AbstractCommand {
    public String cmd;
    private CommandCompletion completions;

    public AbstractCommand(String cmd) {
        this.cmd = cmd;
    }

    protected void onCommand(String[] args) {}

    public void onCommand(CommandExecution execution) {
        onCommand(execution.args());
    }

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

    public boolean hasCompletions() {
        return completions != null;
    }
}
