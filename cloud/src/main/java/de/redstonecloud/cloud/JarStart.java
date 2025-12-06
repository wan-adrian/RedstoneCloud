package de.redstonecloud.cloud;

import de.redstonecloud.api.redis.broker.Broker;
import de.redstonecloud.api.redis.broker.BrokerHelper;
import de.redstonecloud.api.redis.cache.Cache;
import de.redstonecloud.api.util.Keys;
import de.redstonecloud.cloud.config.CloudConfig;
import de.redstonecloud.cloud.config.entires.RedisSettings;
import de.redstonecloud.cloud.redis.PacketHandler;
import de.redstonecloud.cloud.redis.RedisInstance;
import de.redstonecloud.cloud.utils.Setup;
import de.redstonecloud.cloud.utils.Translator;
import de.redstonecloud.cloud.utils.Utils;
import de.redstonecloud.shared.config.SnakeYamlConfig;
import de.redstonecloud.shared.utils.Directories;
import de.redstonecloud.shared.utils.SharedUtils;
import eu.okaeri.configs.ConfigManager;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.pattern.AbstractStyleNameConverter;

import java.io.File;
import java.io.InputStream;

@Log4j2
public class JarStart {
    @SneakyThrows
    public static void main(String[] args) {
        RedstoneCloud.workingDir = System.getProperty("user.dir");

        System.setProperty("java.net.preferIPv4Stack", "true");
        System.setProperty("log4j.skipJansi", "false");
        System.setProperty("Dterminal.jline", "true");
        System.setProperty("Dterminal.ansi", "true");
        System.setProperty("Djansi.passthrough", "true");

        File configFile = new File("./config.yml");
        if (!configFile.exists()) {
            log.info("[JARSTART] No config found, starting setup...");
            new Setup().run();
        }

        log.debug("[JARSTART] Loading config");
        RedstoneCloud.config = ConfigManager.create(CloudConfig.class, it -> {
            it.withConfigurer(new SnakeYamlConfig());
            it.withBindFile(configFile);
            it.withRemoveOrphans(true);
            it.saveDefaults();
            it.load(true);
        });

        Thread.sleep(2000);

        if(RedstoneCloud.config.debug()) SharedUtils.enableDebug();

        RedisSettings redisCfg = RedstoneCloud.getConfig().redis();
        System.setProperty(Keys.PROPERTY_REDIS_PORT, String.valueOf(redisCfg.port()));
        System.setProperty(Keys.PROPERTY_REDIS_IP, redisCfg.ip());
        System.setProperty(Keys.PROPERTY_REDIS_DB, String.valueOf(redisCfg.dbId()));

        if(redisCfg.internalInstance()) {
            RedstoneCloud.redisInstance = new RedisInstance();
        }

        RedstoneCloud.cache = new Cache();

        try {
            log.info(Translator.translate("cloud.startup.redis"));
            RedstoneCloud.broker = new Broker("cloud", BrokerHelper.constructRegistry(), "cloud");
            RedstoneCloud.broker.listen("cloud", PacketHandler::handle);
        } catch (Exception e) {
            log.error(System.getenv(Keys.ENV_REDIS_IP) != null ? System.getenv(Keys.ENV_REDIS_IP) : System.getProperty(Keys.PROPERTY_REDIS_IP));
            log.error(System.getenv(Keys.ENV_REDIS_PORT) != null ? System.getenv(Keys.ENV_REDIS_PORT) : System.getProperty(Keys.PROPERTY_REDIS_PORT));
            throw new RuntimeException("Cannot connect to Redis: " + e);
        }

        RedstoneCloud cloud = new RedstoneCloud();

        Runtime.getRuntime().addShutdownHook(new Thread(cloud::shutdown));
    }
}
