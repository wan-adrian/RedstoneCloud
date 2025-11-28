package de.redstonecloud.node;

import de.redstonecloud.api.util.Keys;
import de.redstonecloud.node.cluster.ClusterClient;
import de.redstonecloud.node.config.NodeConfig;
import de.redstonecloud.node.config.entry.RedisEntry;

public class RedstoneNode {
    public static String workingDir;

    public static void main(String[] args) {
        workingDir = System.getProperty("user.dir");

        RedisEntry redisCfg = NodeConfig.getRedis();

        System.setProperty(Keys.PROPERTY_REDIS_PORT, redisCfg.port());
        System.setProperty(Keys.PROPERTY_REDIS_IP, redisCfg.ip());
        System.setProperty(Keys.PROPERTY_REDIS_DB, String.valueOf(redisCfg.db()));

        ClusterClient clusterClient = ClusterClient.getInstance();
        clusterClient.start();
    }
}