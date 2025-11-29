package de.redstonecloud.node;

import de.redstonecloud.api.util.Keys;
import de.redstonecloud.node.config.NodeConfig;
import de.redstonecloud.node.config.entry.RedisEntry;

public class JarStart {
    public static void main(String[] args) {
        RedstoneNode.workingDir = System.getProperty("user.dir");

        RedisEntry redisCfg = NodeConfig.getRedis();

        System.setProperty(Keys.PROPERTY_REDIS_PORT, redisCfg.port());
        System.setProperty(Keys.PROPERTY_REDIS_IP, redisCfg.ip());
        System.setProperty(Keys.PROPERTY_REDIS_DB, String.valueOf(redisCfg.db()));

        RedstoneNode node = new RedstoneNode();

        Runtime.getRuntime().addShutdownHook(new Thread(node::shutdown));
    }
}
