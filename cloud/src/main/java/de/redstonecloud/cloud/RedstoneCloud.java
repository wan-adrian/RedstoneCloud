package de.redstonecloud.cloud;

import de.redstonecloud.api.encryption.KeyManager;
import de.redstonecloud.api.encryption.cache.KeyCache;
import de.redstonecloud.api.redis.broker.BrokerHelper;
import de.redstonecloud.api.util.Keys;
import de.redstonecloud.cloud.config.CloudConfig;
import de.redstonecloud.cloud.config.entry.RedisEntry;
import de.redstonecloud.cloud.events.EventManager;
import de.redstonecloud.cloud.player.PlayerManager;
import de.redstonecloud.cloud.plugin.PluginManager;
import de.redstonecloud.cloud.redis.PacketHandler;
import de.redstonecloud.cloud.redis.RedisInstance;
import de.redstonecloud.cloud.server.reader.ServerOutReader;
import de.redstonecloud.cloud.commands.CommandManager;
import de.redstonecloud.cloud.console.Console;
import de.redstonecloud.cloud.scheduler.TaskScheduler;
import de.redstonecloud.cloud.scheduler.defaults.CheckTemplateTask;
import de.redstonecloud.cloud.server.ServerManager;
import de.redstonecloud.cloud.utils.Directories;
import de.redstonecloud.cloud.utils.Translator;
import de.redstonecloud.cloud.utils.Utils;
import lombok.Getter;
import lombok.Setter;
import de.redstonecloud.api.redis.broker.Broker;
import de.redstonecloud.api.redis.cache.Cache;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

import java.security.PublicKey;
import java.util.concurrent.ScheduledThreadPoolExecutor;

@Getter
@Log4j2
public class RedstoneCloud {
    @Getter
    private static RedstoneCloud instance;
    @Getter
    public static String workingDir;
    @Getter
    public static Cache cache;
    private static RedisInstance redisInstance;
    @Getter
    private static boolean running = false;


    @Getter private static Broker broker;

    @SneakyThrows
    public static void main(String[] args) {
        workingDir = System.getProperty("user.dir");

        if (!Directories.setupCheck.exists()) Utils.setup();

        RedisEntry redisCfg = CloudConfig.getRedis();

        System.setProperty(Keys.PROPERTY_REDIS_PORT, redisCfg.port());
        System.setProperty(Keys.PROPERTY_REDIS_IP, redisCfg.ip());

        if(redisCfg.useInternal()) {
            redisInstance = new RedisInstance();
        }

        Thread.sleep(2000);

        cache = new Cache();

        try {
            log.info(Translator.translate("cloud.startup.redis"));
            broker = new Broker("cloud", BrokerHelper.constructRegistry(), "cloud");

            broker.listen("cloud", PacketHandler::handle);
        } catch (Exception e) {
            log.error(System.getenv(Keys.ENV_REDIS_IP) != null ? System.getenv(Keys.ENV_REDIS_IP) : System.getProperty(Keys.PROPERTY_REDIS_IP));
            log.error(System.getenv(Keys.ENV_REDIS_PORT) != null ? System.getenv(Keys.ENV_REDIS_PORT) : System.getProperty(Keys.PROPERTY_REDIS_PORT));
            throw new RuntimeException("Cannot connect to Redis: " + e);
        }

        System.setProperty("java.net.preferIPv4Stack", "true");
        System.setProperty("log4j.skipJansi", "false");
        System.setProperty("Dterminal.jline", "true");
        System.setProperty("Dterminal.ansi", "true");
        System.setProperty("Djansi.passthrough", "true");

        RedstoneCloud cloud = new RedstoneCloud();

        Runtime.getRuntime().addShutdownHook(new Thread(cloud::stop));
    }

    private ConsoleThread consoleThread;
    @Setter
    protected ServerOutReader currentLogServer = null;
    protected PlayerManager playerManager;
    protected ServerManager serverManager;
    protected CommandManager commandManager;
    protected Console console;
    protected PluginManager pluginManager;
    protected EventManager eventManager;

    protected boolean stopped = false;

    protected TaskScheduler scheduler;
    protected KeyCache keyCache;

    public RedstoneCloud() {
        instance = this;
        boot();
    }

    public void boot() {
        running = true;

        this.scheduler = new TaskScheduler(new ScheduledThreadPoolExecutor(Runtime.getRuntime().availableProcessors()));

        PublicKey publicKey = KeyManager.init();
        this.keyCache = new KeyCache();
        this.keyCache.addKey("cloud", publicKey);

        log.info(Translator.translate("cloud.startup"));

        Utils.createBaseFolders();

        this.playerManager = new PlayerManager();
        this.serverManager = ServerManager.getInstance();
        this.commandManager = new CommandManager();
        commandManager.loadCommands();

        this.eventManager = new EventManager(this);

        this.pluginManager = new PluginManager(this);
        pluginManager.loadAllPlugins();

        this.console = new Console(this);
        this.consoleThread = new ConsoleThread();
        this.consoleThread.start();

        this.scheduler.scheduleRepeatingTask(new CheckTemplateTask(), 3000L);

        this.pluginManager.enableAllPlugins();
    }

    public void stop() {
        if (this.stopped || !running) {
            return;
        }

        this.stopped = true;
        running = false;
        this.scheduler.cancelAll();

        try {
            Thread.sleep(200);
            log.info(Translator.translate("cloud.shutdown.started"));
            boolean a = this.serverManager.stopAll();
            if(a) log.info(Translator.translate("cloud.shutdown.servers"));
            Thread.sleep(500);
            this.pluginManager.disableAllPlugins();
            log.info(Translator.translate("cloud.shutdown.plugins"));
            this.eventManager.getThreadedExecutor().shutdown();
            log.info(Translator.translate("cloud.shutdown.complete"));
            this.scheduler.stopScheduler();

            broker.getPool().getResource().flushAll();
            broker.shutdown();
            if(redisInstance != null) redisInstance.shutdown();
        } catch (InterruptedException e) {
            log.error("Error during shutdown: ", e);
        } catch (Exception e) {
            log.error("Unexpected error during shutdown: ", e);
        }

        System.exit(0);
    }

    private class ConsoleThread extends Thread {
        public ConsoleThread() {
            super("Console Thread");
        }

        @Override
        public void run() {
            if (isRunning()) console.start();
        }
    }
}
