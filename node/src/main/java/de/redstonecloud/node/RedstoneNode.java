package de.redstonecloud.node;

import de.redstonecloud.node.cluster.ClusterClient;
import de.redstonecloud.node.cluster.grpc.RCMaster;
import de.redstonecloud.node.commands.defaults.*;
import de.redstonecloud.node.config.NodeConfig;
import de.redstonecloud.node.server.NodeServerManager;
import de.redstonecloud.shared.commands.CommandCompletion;
import de.redstonecloud.shared.commands.CommandManager;
import de.redstonecloud.shared.console.Console;
import de.redstonecloud.shared.console.ConsoleThread;
import de.redstonecloud.shared.utils.CurrentInstance;
import de.redstonecloud.shared.utils.Directories;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import java.util.List;

@Getter
@Slf4j
public class RedstoneNode {
    private NodeServerManager serverManager;
    private ClusterClient clusterClient;

    private Console console;
    private ConsoleThread consoleThread;
    private CommandManager commandManager;

    @Getter protected static String workingDir;
    @Getter protected static NodeConfig config;
    @Getter private static RedstoneNode instance;

    protected RedstoneNode() {
        instance = this;
        this.serverManager = NodeServerManager.getInstance();

        CurrentInstance.setNODE_ID(config.node().id());

        log.debug("[BOOT] Starting CommandManager & Console");
        this.commandManager = CommandManager.getInstance();
        this.console = Console.getInstance();
        this.consoleThread = new ConsoleThread();
        this.consoleThread.start();
        registerCompletionResolvers();
        loadCommands();

        clusterClient = ClusterClient.getInstance();
        clusterClient.start();
    }

    private void loadCommands() {
        //load node commands
        commandManager.addCommand(new EndCommand("end"));
        commandManager.addCommand(new InfoCommand("info"));
        commandManager.addCommand(new ListCommand("list"));
        commandManager.addCommand(new ExecuteCommand("execute"));
        commandManager.addCommand(new ConsoleCommand("console"));
    }

    public void shutdown() {
        RCMaster.shutdownNode();
        this.serverManager.stopAll();
        this.clusterClient.shutdown();
        //remove tmp files
        try {
            FileUtils.deleteDirectory(Directories.TMP_STORAGE_DIR);
        } catch (Exception ignored) {
        }

        System.exit(0);
    }

    private void registerCompletionResolvers() {
        CommandCompletion.registerResolver(CommandCompletion.ParamType.SERVER, () ->
                getServerManager().getServers().values().stream()
                        .map(server -> server.getName())
                        .sorted(String.CASE_INSENSITIVE_ORDER)
                        .toList()
        );
        CommandCompletion.registerResolver(CommandCompletion.ParamType.SERVER_LOCAL, () ->
                getServerManager().getServers().values().stream()
                        .filter(server -> server.isLocal())
                        .map(server -> server.getName())
                        .sorted(String.CASE_INSENSITIVE_ORDER)
                        .toList()
        );
        CommandCompletion.registerResolver(CommandCompletion.ParamType.TEMPLATE, () ->
                getServerManager().getTemplates().values().stream()
                        .map(template -> template.getName())
                        .sorted(String.CASE_INSENSITIVE_ORDER)
                        .toList()
        );
        CommandCompletion.registerResolver(CommandCompletion.ParamType.TYPE, () ->
                getServerManager().getTypes().values().stream()
                        .map(type -> type.name())
                        .sorted(String.CASE_INSENSITIVE_ORDER)
                        .toList()
        );
    }
}