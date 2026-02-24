package de.redstonecloud.cloud;

import de.redstonecloud.api.encryption.KeyManager;
import de.redstonecloud.api.encryption.cache.KeyCache;
import de.redstonecloud.cloud.cluster.ClusterManager;
import de.redstonecloud.cloud.commands.defaults.*;
import de.redstonecloud.cloud.config.CloudConfig;
import de.redstonecloud.cloud.events.EventManager;
import de.redstonecloud.cloud.player.PlayerManager;
import de.redstonecloud.cloud.plugin.PluginManager;
import de.redstonecloud.cloud.redis.RedisInstance;
import de.redstonecloud.cloud.rest.RestApiService;
import de.redstonecloud.shared.commands.CommandManager;
import de.redstonecloud.shared.config.SnakeYamlConfig;
import de.redstonecloud.shared.console.Console;
import de.redstonecloud.cloud.scheduler.TaskScheduler;
import de.redstonecloud.cloud.scheduler.defaults.CheckTemplateTask;
import de.redstonecloud.cloud.server.ServerManager;
import de.redstonecloud.shared.console.ConsoleThread;
import de.redstonecloud.cloud.utils.Utils;
import eu.okaeri.configs.ConfigManager;
import lombok.Getter;
import de.redstonecloud.api.redis.broker.Broker;
import de.redstonecloud.api.redis.cache.Cache;
import lombok.extern.log4j.Log4j2;

import javax.annotation.Nullable;
import java.io.File;
import java.security.PublicKey;
import java.util.concurrent.ScheduledThreadPoolExecutor;

@Getter
@Log4j2
public class RedstoneCloud {
    @Getter private static RedstoneCloud instance;
    @Getter public static String workingDir;
    @Getter public static Cache cache;
    @Getter private static boolean running = false;
    @Getter protected static Broker broker;
    @Getter protected static CloudConfig config;
    protected static RedisInstance redisInstance;

    @Nullable private ClusterManager clusterManager;
    private ConsoleThread consoleThread;
    private PlayerManager playerManager;
    private ServerManager serverManager;
    private CommandManager commandManager;
    private Console console;
    private PluginManager pluginManager;
    private EventManager eventManager;
    private TaskScheduler scheduler;
    private KeyCache keyCache;
    private RestApiService restApiService;

    protected RedstoneCloud() {
        instance = this;
        boot();
    }

    private void boot() {
        running = true;
        log.info("RedstoneCloud is starting...");

        log.debug("[BOOT] Starting cloud scheduler");
        this.scheduler = new TaskScheduler(new ScheduledThreadPoolExecutor(Runtime.getRuntime().availableProcessors()));

        log.debug("[BOOT] Creating public key");
        PublicKey publicKey = KeyManager.init();
        this.keyCache = new KeyCache();
        this.keyCache.addKey("cloud", publicKey);

        log.debug("[BOOT] Creating folders");
        Utils.createBaseFolders();

        log.debug("[BOOT] Starting PlayerManager");
        this.playerManager = new PlayerManager();
        log.debug("[BOOT] Starting ServerManager");
        this.serverManager = ServerManager.getInstance();
        log.debug("[BOOT] Starting CommandManager");
        this.commandManager = CommandManager.getInstance();
        log.debug("[BOOT] Starting EventManager");
        this.eventManager = new EventManager(this);
        log.debug("[BOOT] Starting PluginManager");
        this.pluginManager = new PluginManager(this);

        log.debug("[BOOT] Loading all plugins");
        pluginManager.loadAllPlugins();

        log.debug("[BOOT] Start console");
        this.console = Console.getInstance();
        this.consoleThread = new ConsoleThread();
        this.consoleThread.start();

        this.scheduler.scheduleRepeatingTask(new CheckTemplateTask(), 3000L);
        log.debug("[BOOT] Enable all plugins");
        this.pluginManager.enableAllPlugins();

        if (config.restApi().enabled()) {
            log.debug("[BOOT] Starting REST API");
            this.restApiService = new RestApiService(config.restApi());
            this.restApiService.start();
        } else {
            log.debug("[BOOT] REST API disabled.");
        }

        if(!config.cluster().nodes().isEmpty()) {
            log.debug("[BOOT] Loading cluster management");
            clusterManager = ClusterManager.getInstance();
            clusterManager.startServer();
        } else log.debug("[BOOT] No clusters connected.");

        initCommands();
    }

    private void initCommands() {
        log.debug("[BOOT] Loading commands");

        commandManager.addCommand(new ConsoleCommand("console"));
        commandManager.addCommand(new EndCommand("end"));
        commandManager.addCommand(new InfoCommand("info"));
        commandManager.addCommand(new StartCommand("start"));
        commandManager.addCommand(new StopCommand("stop"));
        commandManager.addCommand(new ListCommand("list"));
        commandManager.addCommand(new KillCommand("kill"));
        commandManager.addCommand(new ExecuteCommand("execute"));
        commandManager.addCommand(new PlayerCommand("player"));
        commandManager.addCommand(new UpdateCommand("update"));
        commandManager.addCommand(new ReloadCommand("reload"));
        commandManager.addCommand(new RestApiCommand("restapi"));

        log.debug("[BOOT] Registered {} commands", commandManager.getCommandMap().size());
    }

    public void shutdown() {
        if (!running)
            return;

        running = false;

        log.debug("[SHUTDOWN] Cancelling all tasks");
        this.scheduler.cancelAll();

        try {
            Thread.sleep(200);
            log.info("RedstoneCloud is shutting down...");

            log.debug("[SHUTDOWN] Stopping all servers");
            synchronized(this) {
                this.serverManager.stopAll();
            }

            log.debug("[SHUTDOWN] Disable all plugins");
            this.pluginManager.disableAllPlugins();

            log.debug("[SHUTDOWN] Shutting down EventManager");
            this.eventManager.getThreadedExecutor().shutdown();

            log.debug("[SHUTDOWN] Flushing RC Redis");
            broker.getPool().getResource().flushDB();

            log.debug("[SHUTDOWN] Shutdown Broker");
            broker.shutdown();

            log.debug("[SHUTDOWN] Stopping scheduler");
            this.scheduler.stopScheduler();

            if(redisInstance != null) {
                log.debug("[SHUTDOWN] Stopping internal redis");
                redisInstance.shutdown();
            }

            if(clusterManager != null) {
                log.debug("[SHUTDOWN] Stopping cluster management");
                clusterManager.stopServer();
            }

            if (restApiService != null) {
                log.debug("[SHUTDOWN] Stopping REST API");
                restApiService.stop();
            }

            log.info("RedstoneCloud has been shut down.");
        } catch (Exception e) {
            log.error("Unexpected error during shutdown: ", e);
        }

        System.exit(0);
    }
}
