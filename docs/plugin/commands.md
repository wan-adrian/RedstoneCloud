# Commands

This page documents how cloud plugins register and structure commands, and how command completion works.

## Registering Commands

Commands are registered through the cloud command manager. Plugins should construct command instances during enable.

Guidelines:

- Keep command names lowercase and stable.
- Use subcommands for complex actions instead of long argument lists.
- Prefer named parameters in completions so handlers can access values by name.

## Command Execution

Commands are invoked via `onCommand(CommandExecution execution)` when completions are enabled.

`CommandExecution` provides:

- Raw args: `execution.args()`
- Positional args: `execution.positionals()` / `execution.positional(index)`
- Named values: `execution.value("name")`
- Presence checks: `execution.has("name")`

When a command has a completion tree, names come from the completion definitions. If no names exist or no match is found, positional access still works.

## Legacy (Non-Completion) Commands

Commands without completion trees keep the classic `onCommand(String[] args)` handler.

Recommended approach:

- Put argument parsing in `onCommand`.
- Use the raw `args` array for manual parsing.
- Do not rely on named values or flags unless completions are set.

Example:

```java
public class SimpleCommand extends Command {
    public SimpleCommand(String cmd) {
        super(cmd);
    }

    @Override
    protected void onCommand(String[] args) {
        if (args.length == 0) {
            return;
        }
        // manual parsing
    }
}
```

## Completion Trees

Completions are defined using `CommandCompletion`. Common patterns:

- Literal branches: `CommandCompletion.literal("sub")`
- Param branches: `CommandCompletion.param(ParamType.SERVER, "server")`
- Flags: `CommandCompletion.flag("amount", ParamType.ANY, "--amount", "-a")`
- Switch flags: `CommandCompletion.flagSwitch("reboot", "--reboot")`
- Any-order flags: `CommandCompletion.anyOrder(mainNode, flags...)`

Example:

```java
public class StartCommand extends Command {
    public StartCommand(String cmd) {
        super(cmd);
        CommandCompletion.Node template = CommandCompletion.param(CommandCompletion.ParamType.TEMPLATE, "template");
        CommandCompletion.Flag id = CommandCompletion.flag("id", CommandCompletion.ParamType.ANY, "--id", "-i");
        CommandCompletion.Flag amount = CommandCompletion.flag("amount", CommandCompletion.ParamType.ANY, "--amount", "-a");
        setCompletions(CommandCompletion.anyOrder(template, id, amount).restrictRootTo(template));
    }

    @Override
    public void onCommand(CommandExecution execution) {
        String template = execution.value("template");
        String id = execution.value("id");
        String amount = execution.value("amount");
    }
}
```

## Resolver Types

`ParamType` values map to dynamic resolvers registered in cloud startup. Common types:

- `SERVER`, `SERVER_LOCAL`
- `TEMPLATE`
- `PLAYER`
- `TYPE`
- `REST_TOKEN`, `PERMISSION`
- `ANY`

Plugins can rely on these resolvers for autocomplete when targeting cloud-only commands.
