package de.redstonecloud.node;

import de.redstonecloud.node.cluster.ClusterClient;
import de.redstonecloud.node.commands.defaults.EndCommand;
import de.redstonecloud.node.commands.defaults.InfoCommand;
import de.redstonecloud.node.commands.defaults.ListCommand;
import de.redstonecloud.node.server.NodeServerManager;
import de.redstonecloud.shared.commands.CommandManager;
import de.redstonecloud.shared.console.Console;
import de.redstonecloud.shared.console.ConsoleThread;
import de.redstonecloud.shared.utils.Directories;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

@Getter
@Slf4j
public class RedstoneNode {
    private NodeServerManager serverManager;
    private ClusterClient clusterClient;

    private Console console;
    private ConsoleThread consoleThread;
    private CommandManager commandManager;

    @Getter protected static String workingDir;
    @Getter private static RedstoneNode instance;

    protected RedstoneNode() {
        instance = this;
        this.serverManager = NodeServerManager.getInstance();

        log.debug("[BOOT] Starting CommandManager & Console");
        this.commandManager = CommandManager.getInstance();
        this.console = Console.getInstance();
        this.consoleThread = new ConsoleThread();
        this.consoleThread.start();
        loadCommands();


        clusterClient = ClusterClient.getInstance();
        clusterClient.start();
    }

    private void loadCommands() {
        //load node commands
        commandManager.addCommand(new EndCommand("end"));
        commandManager.addCommand(new InfoCommand("info"));
        commandManager.addCommand(new ListCommand("list"));
    }

    public void shutdown() {
        this.serverManager.stopAll();
        this.clusterClient.shutdown();
        //remove tmp files
        try {
            FileUtils.deleteDirectory(Directories.TMP_STORAGE_DIR);
        } catch (Exception ignored) {
        }
    }
}