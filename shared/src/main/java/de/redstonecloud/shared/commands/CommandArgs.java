package de.redstonecloud.shared.commands;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class CommandArgs {
    private final List<String> positionals;
    private final Map<String, String> flags;

    private CommandArgs(List<String> positionals, Map<String, String> flags) {
        this.positionals = positionals;
        this.flags = flags;
    }

    public static CommandArgs parse(String[] args) {
        List<String> positionals = new ArrayList<>();
        Map<String, String> flags = new HashMap<>();

        if (args == null || args.length == 0) {
            return new CommandArgs(positionals, flags);
        }

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg == null || arg.isBlank()) {
                continue;
            }

            if (arg.startsWith("--")) {
                String key = arg.substring(2).toLowerCase(Locale.ROOT);
                if (key.isEmpty()) {
                    continue;
                }
                String value = null;
                if (i + 1 < args.length && !isFlag(args[i + 1])) {
                    value = args[++i];
                }
                flags.put(key, value);
                continue;
            }

            if (arg.startsWith("-") && arg.length() > 1) {
                String key = arg.substring(1).toLowerCase(Locale.ROOT);
                String value = null;
                if (i + 1 < args.length && !isFlag(args[i + 1])) {
                    value = args[++i];
                }
                flags.put(key, value);
                continue;
            }

            positionals.add(arg);
        }

        return new CommandArgs(positionals, flags);
    }

    private static boolean isFlag(String value) {
        return value != null && value.startsWith("-");
    }

    public List<String> positionals() {
        return positionals;
    }

    public Map<String, String> flags() {
        return flags;
    }

    public boolean hasFlag(String key) {
        if (key == null) {
            return false;
        }
        return flags.containsKey(key.toLowerCase(Locale.ROOT));
    }

    public String flagValue(String key) {
        if (key == null) {
            return null;
        }
        return flags.get(key.toLowerCase(Locale.ROOT));
    }
}
