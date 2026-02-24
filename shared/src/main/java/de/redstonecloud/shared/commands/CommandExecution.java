package de.redstonecloud.shared.commands;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class CommandExecution {
    private final String[] args;
    private final List<String> positionals;
    private final Map<String, String> params;
    private final Map<String, String> flags;

    private CommandExecution(String[] args,
                             List<String> positionals,
                             Map<String, String> params,
                             Map<String, String> flags) {
        this.args = args == null ? new String[0] : args;
        this.positionals = positionals == null ? List.of() : positionals;
        this.params = params == null ? Map.of() : params;
        this.flags = flags == null ? Map.of() : flags;
    }

    public static CommandExecution from(AbstractCommand command, String[] args) {
        CommandArgs parsed = CommandArgs.parse(args);
        CommandCompletion completion = command != null ? command.getCompletions() : null;

        Map<String, String> params = new HashMap<>();
        Map<String, String> flags = new HashMap<>();

        if (completion != null) {
            CommandCompletion.MatchResult match = completion.match(args);
            params.putAll(match.params());

            for (Map.Entry<String, String> entry : parsed.flags().entrySet()) {
                String canonical = completion.canonicalFlagName(entry.getKey());
                flags.put(canonical, entry.getValue());
            }
        } else {
            flags.putAll(parsed.flags());
        }

        return new CommandExecution(args, parsed.positionals(), params, flags);
    }

    public String[] args() {
        return args;
    }

    public List<String> positionals() {
        return Collections.unmodifiableList(positionals);
    }

    public String positional(int index) {
        if (index < 0 || index >= positionals.size()) {
            return null;
        }
        return positionals.get(index);
    }

    public String value(String name) {
        if (name == null) {
            return null;
        }
        String val = params.get(name);
        if (val != null) {
            return val;
        }
        return flags.get(name);
    }

    public boolean has(String name) {
        if (name == null) {
            return false;
        }
        return params.containsKey(name) || flags.containsKey(name);
    }

    public Map<String, String> flags() {
        return Collections.unmodifiableMap(flags);
    }

    public Map<String, String> params() {
        return Collections.unmodifiableMap(params);
    }

    public String joinFromRaw(int startIndex) {
        if (startIndex < 0 || startIndex >= args.length) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = startIndex; i < args.length; i++) {
            if (i > startIndex) {
                builder.append(' ');
            }
            builder.append(args[i]);
        }
        return builder.toString();
    }
}
