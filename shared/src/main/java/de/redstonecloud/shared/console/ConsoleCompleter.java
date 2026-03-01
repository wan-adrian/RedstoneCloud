package de.redstonecloud.shared.console;

import de.redstonecloud.shared.commands.AbstractCommand;
import de.redstonecloud.shared.commands.CommandCompletion;
import de.redstonecloud.shared.commands.CommandManager;
import de.redstonecloud.shared.utils.CurrentInstance;
import org.jline.reader.*;

import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Consumer;

public class ConsoleCompleter implements Completer {

    @Override
    public void complete(LineReader lineReader, ParsedLine parsedLine, List<Candidate> candidates) {
        if(CurrentInstance.currentLogServer == null) {
            if (parsedLine.wordIndex() == 0) {
                if (parsedLine.word().isEmpty()) {
                    addCandidates(s -> candidates.add(new Candidate(s)));
                    return;
                }
                SortedSet<String> names = new TreeSet<>();
                addCandidates(names::add);
                for (String match : names) {
                    if (!match.toLowerCase().startsWith(parsedLine.word().toLowerCase())) {
                        continue;
                    }

                    candidates.add(new Candidate(match));
                }
            } else {
                String command = parsedLine.words().getFirst();
                AbstractCommand cmd = CommandManager.getInstance().getCommand(command);
                if (cmd == null) {
                    return;
                }

                int wordIndex = parsedLine.wordIndex();
                int argIndex = wordIndex - 1;

                List<String> words = parsedLine.words();
                List<String> args = words.size() > 1 ? words.subList(1, words.size()) : List.of();

                if (argIndex < 0) {
                    return;
                }

                String currentToken = parsedLine.word();
                String[] argsArray = args.toArray(String[]::new);
                CommandCompletion completion = cmd.getCompletions();
                if (completion == null) {
                    return;
                }
                for (String arg : completion.complete(argsArray, argIndex, currentToken)) {
                    if (arg != null && !arg.isEmpty()) {
                        candidates.add(new Candidate(arg));
                    }
                }
            }
        } else {
            if (parsedLine.wordIndex() == 0) {
                if (parsedLine.word().isEmpty()) {
                    candidates.add(new Candidate("_exit"));
                }
            }
        }
    }

    private void addCandidates(Consumer<String> commandConsumer) {
        for (String command : CommandManager.getInstance().getCommandMap().keySet()) {
            if (!command.contains(":")) {
                commandConsumer.accept(command);
            }
        }
    }
}
