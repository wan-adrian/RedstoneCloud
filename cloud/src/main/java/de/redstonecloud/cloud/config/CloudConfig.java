package de.redstonecloud.cloud.config;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import de.redstonecloud.cloud.RedstoneCloud;
import de.redstonecloud.cloud.config.entry.ClusterServerEntry;
import de.redstonecloud.cloud.config.entry.NodesEntry;
import de.redstonecloud.cloud.config.entry.RedisEntry;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class CloudConfig {
    private static JsonObject cfg;
    private static List<NodesEntry> nodes = new ArrayList<>();

    static {
        JsonObject cfg = getCfg();
        if (cfg.has("nodes"))
            for (int i = 0; i < cfg.getAsJsonArray("nodes").size(); i++) {
                JsonObject node = cfg.getAsJsonArray("nodes").get(i).getAsJsonObject();
                nodes.add(new NodesEntry(
                        node.get("name").getAsString(),
                        node.get("id").getAsString()
                ));
            }
    }

    public static boolean hasNodes() {
        return !nodes.isEmpty();
    }

    public static JsonObject getCfg() {
        return getCfg(false);
    }

    public static JsonObject getCfg(boolean reload) {
        if (reload || cfg == null) {
            try {
                cfg = new Gson().fromJson(Files.readString(Paths.get(RedstoneCloud.workingDir + "/cloud.json")), JsonObject.class);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return cfg;
    }

    public static RedisEntry getRedis() {
        JsonObject cfg = getCfg();
        return new RedisEntry(
                cfg.get("redis_bind").getAsString(),
                cfg.get("redis_port").getAsString(),
                !cfg.get("custom_redis").getAsBoolean(),
                !cfg.has("redis_db") ? 0 : cfg.get("redis_db").getAsInt()
        );
    }

    public static ClusterServerEntry getClusterServer() {
        JsonObject cfg = getCfg();
        return new ClusterServerEntry(
                cfg.get("cluster_server_bind").getAsString(),
                cfg.get("cluster_server_port").getAsInt()
        );
    }
}
