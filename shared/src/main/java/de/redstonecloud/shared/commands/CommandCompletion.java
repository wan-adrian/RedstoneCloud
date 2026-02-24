package de.redstonecloud.shared.commands;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public final class CommandCompletion {
    public enum ParamType {
        SERVER,
        SERVER_LOCAL,
        TEMPLATE,
        TYPE,
        PLAYER,
        REST_TOKEN,
        PERMISSION,
        ANY
    }

    public static final class Flag {
        private final List<Node> flagNodes;
        private final Node valueNode;

        private Flag(List<Node> flagNodes, Node valueNode) {
            this.flagNodes = flagNodes;
            this.valueNode = valueNode;
        }

        public List<Node> flagNodes() {
            return flagNodes;
        }

        public Node valueNode() {
            return valueNode;
        }
    }

    public sealed interface Node permits LiteralNode, ParamNode {
        List<Node> children();
        boolean matches(String token);
        List<String> candidates(String currentToken);

        default Node then(Node child) {
            children().add(child);
            return this;
        }
    }

    public static final class LiteralNode implements Node {
        private final String value;
        private final List<Node> children = new ArrayList<>();

        public LiteralNode(String value) {
            this.value = value;
        }

        @Override
        public List<Node> children() {
            return children;
        }

        @Override
        public boolean matches(String token) {
            if (token == null) {
                return false;
            }
            return value.equalsIgnoreCase(token);
        }

        @Override
        public List<String> candidates(String currentToken) {
            if (currentToken == null || currentToken.isEmpty()) {
                return List.of(value);
            }
            if (value.toLowerCase(Locale.ROOT).startsWith(currentToken.toLowerCase(Locale.ROOT))) {
                return List.of(value);
            }
            return List.of();
        }
    }

    public static final class ParamNode implements Node {
        private final ParamType type;
        private final List<Node> children = new ArrayList<>();

        public ParamNode(ParamType type) {
            this.type = type;
        }

        public ParamType type() {
            return type;
        }

        @Override
        public List<Node> children() {
            return children;
        }

        @Override
        public boolean matches(String token) {
            return token != null && !token.isEmpty();
        }

        @Override
        public List<String> candidates(String currentToken) {
            Collection<String> values = resolve(type);
            if (values == null || values.isEmpty()) {
                return List.of();
            }
            if (currentToken == null || currentToken.isEmpty()) {
                return values.stream().filter(v -> v != null && !v.isEmpty()).toList();
            }
            String needle = currentToken.toLowerCase(Locale.ROOT);
            return values.stream()
                    .filter(v -> v != null && v.toLowerCase(Locale.ROOT).startsWith(needle))
                    .toList();
        }
    }

    private static final Map<ParamType, Supplier<Collection<String>>> RESOLVERS = new ConcurrentHashMap<>();

    private final List<Node> roots = new ArrayList<>();

    public static CommandCompletion root() {
        return new CommandCompletion();
    }

    public static LiteralNode literal(String value) {
        return new LiteralNode(value);
    }

    public static ParamNode param(ParamType type) {
        return new ParamNode(type);
    }

    public static Flag flag(ParamType valueType, String... names) {
        List<Node> flags = new ArrayList<>();
        ParamNode valueNode = param(valueType);
        for (String name : names) {
            LiteralNode flag = literal(name);
            flag.then(valueNode);
            flags.add(flag);
        }
        return new Flag(flags, valueNode);
    }

    public static Flag flag(String... names) {
        return flag(ParamType.ANY, names);
    }

    public static Flag flagSwitch(String... names) {
        List<Node> flags = new ArrayList<>();
        for (String name : names) {
            flags.add(literal(name));
        }
        return new Flag(flags, null);
    }

    public static CommandCompletion anyOrder(Node main, Flag... flags) {
        CommandCompletion completion = root();
        if (main != null) {
            completion.add(main);
        }

        List<Node> allFlags = new ArrayList<>();
        for (Flag flag : flags) {
            if (flag == null) {
                continue;
            }
            for (Node node : flag.flagNodes()) {
                if (node != null) {
                    completion.add(node);
                    allFlags.add(node);
                }
            }
        }

        if (main != null) {
            for (Node flagNode : allFlags) {
                main.then(flagNode);
            }
        }

        for (Flag flag : flags) {
            if (flag == null) {
                continue;
            }
            Node tail = flag.valueNode() != null ? flag.valueNode() : null;
            for (Node flagNode : flag.flagNodes()) {
                if (tail == null) {
                    tail = flagNode;
                    break;
                }
            }
            if (tail == null) {
                continue;
            }
            if (main != null) {
                tail.then(main);
            }
            for (Node otherFlag : allFlags) {
                tail.then(otherFlag);
            }
        }

        return completion;
    }

    public CommandCompletion add(Node node) {
        if (node != null) {
            roots.add(node);
        }
        return this;
    }

    public CommandCompletion restrictRootTo(Node node) {
        roots.clear();
        if (node != null) {
            roots.add(node);
        }
        return this;
    }

    public List<String> complete(String[] args, int argIndex, String currentToken) {
        List<Node> frontier = new ArrayList<>(roots);
        if (frontier.isEmpty()) {
            return List.of();
        }

        if (argIndex > 0 && args != null) {
            int max = Math.min(argIndex, args.length);
            for (int i = 0; i < max; i++) {
                String token = args[i];
                List<Node> next = new ArrayList<>();
                for (Node node : frontier) {
                    if (node.matches(token)) {
                        next.addAll(node.children());
                    }
                }
                frontier = next;
                if (frontier.isEmpty()) {
                    return List.of();
                }
            }
        }

        List<String> candidates = new ArrayList<>();
        for (Node node : frontier) {
            candidates.addAll(node.candidates(currentToken));
        }
        return candidates;
    }

    public static void registerResolver(ParamType type, Supplier<Collection<String>> supplier) {
        if (type == null || supplier == null) {
            return;
        }
        RESOLVERS.put(type, supplier);
    }

    public static Collection<String> resolve(ParamType type) {
        Supplier<Collection<String>> supplier = RESOLVERS.get(type);
        if (supplier == null) {
            return List.of();
        }
        try {
            Collection<String> values = supplier.get();
            return values == null ? List.of() : values;
        } catch (Exception ignored) {
            return List.of();
        }
    }
}
