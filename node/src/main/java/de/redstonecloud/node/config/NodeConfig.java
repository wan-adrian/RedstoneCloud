package de.redstonecloud.node.config;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import de.redstonecloud.node.RedstoneNode;
import de.redstonecloud.node.config.entry.MasterEntry;
import de.redstonecloud.node.config.entry.RedisEntry;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class NodeConfig {
    private static JsonObject cfg;

    public static JsonObject getCfg() {
        return getCfg(false);
    }

    public static JsonObject getCfg(boolean reload) {
        if (reload || cfg == null) {
            try {
                cfg = new Gson().fromJson(Files.readString(Paths.get(RedstoneNode.workingDir + "/cloud.json")), JsonObject.class);
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
                !cfg.has("redis_db") ? 0 : cfg.get("redis_db").getAsInt()
        );
    }

    public static MasterEntry getMasterSettings() {
        JsonObject cfg = getCfg();
        return new MasterEntry(
                cfg.get("master_address").getAsString(),
                cfg.get("master_port").getAsInt()
        );
    }

    public static String getNodeId() {
        JsonObject cfg = getCfg();

        if (!cfg.has("node_id")) {
            String nodeId = java.util.UUID.randomUUID().toString();
            setNodeId(nodeId);
            return nodeId;
        }
        return cfg.get("node_id").getAsString();
    }

    private static void setNodeId(String nodeId) {
        JsonObject cfg = getCfg();
        cfg.addProperty("node_id", nodeId);
        try {
            Files.writeString(Paths.get(RedstoneNode.workingDir + "/cloud.json"), cfg.toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
