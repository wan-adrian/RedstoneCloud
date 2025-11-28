package de.redstonecloud.shared.commands;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
public class CommandManager {
    private static CommandManager instance;

    public static CommandManager getInstance() {
        if (instance == null) {
            instance = new CommandManager();
        }
        return instance;
    }

    @Getter
    private Map<String, AbstractCommand> commandMap = new HashMap<>();
    private final Set<AbstractCommand> commands;

    private CommandManager() {
        this.commands = new HashSet<>();
    }

    public void addCommand(AbstractCommand cmd) {
        commands.add(cmd);
        commandMap.put(cmd.getCommand(), cmd);
    }

    public void executeCommand(String command, String[] args) {
        AbstractCommand cmd = getCommand(command);

        if (cmd != null) {
            try {
                log.info("Executing {}", cmd.getCommand());
                cmd.onCommand(args);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            if(!command.isEmpty()) {
                log.info("This command does not exist!");
            }
        }
    }

    public AbstractCommand getCommand(String name) {
        for (AbstractCommand command : this.commands) {
            if (command.getCommand().equalsIgnoreCase(name)) {
                return command;
            }
        }
        return null;
    }

    public AbstractCommand getCommand(Class<? extends AbstractCommand> name) {
        for (AbstractCommand command : this.commands) {
            if (command.getClass().equals(name)) {
                return command;
            }
        }
        return null;
    }
}