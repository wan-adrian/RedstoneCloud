package de.redstonecloud.node;

import de.redstonecloud.api.util.Keys;
import de.redstonecloud.node.config.NodeConfig;
import de.redstonecloud.node.config.entires.RedisSettings;
import de.redstonecloud.node.utils.Setup;
import de.redstonecloud.shared.config.SnakeYamlConfig;
import de.redstonecloud.shared.utils.SharedUtils;
import eu.okaeri.configs.ConfigManager;
import lombok.extern.slf4j.Slf4j;

import java.io.File;

@Slf4j
public class JarStart {
    public static void main(String[] args) {
        RedstoneNode.workingDir = System.getProperty("user.dir");

        log.debug("[JARSTART] Loading config");
        File configFile = new File("./config.yml");

        if (!configFile.exists()) {
            log.debug("[JARSTART] No config found, starting setup...");
            new Setup().run();
        }

        RedstoneNode.config = ConfigManager.create(NodeConfig.class, it -> {
            it.withConfigurer(new SnakeYamlConfig());
            it.withBindFile(configFile);
            it.withRemoveOrphans(true);
            it.saveDefaults();
            it.load(true);
        });

        if(RedstoneNode.config.debug()) SharedUtils.enableDebug();

        RedisSettings redisCfg = RedstoneNode.getConfig().redis();
        System.setProperty(Keys.PROPERTY_REDIS_PORT, String.valueOf(redisCfg.port()));
        System.setProperty(Keys.PROPERTY_REDIS_IP, redisCfg.connectIp());
        System.setProperty(Keys.PROPERTY_REDIS_DB, String.valueOf(redisCfg.dbId()));

        RedstoneNode node = new RedstoneNode();

        Runtime.getRuntime().addShutdownHook(new Thread(node::shutdown));
    }
}
