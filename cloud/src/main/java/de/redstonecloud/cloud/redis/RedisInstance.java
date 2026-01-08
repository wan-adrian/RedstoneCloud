package de.redstonecloud.cloud.redis;

import de.redstonecloud.cloud.RedstoneCloud;
import lombok.Getter;
import redis.embedded.RedisServer;

public class RedisInstance {
    @Getter
    public static boolean running = false;
    private static RedisServer redisServer;

    public static RedisServer getRedisServer() {
        return redisServer;
    }

    public RedisInstance() {
        if(redisServer != null) return;
        try {
            redisServer = RedisServer.builder()
                    .port(RedstoneCloud.getConfig().redis().port())
                    .setting("bind " + RedstoneCloud.getConfig().redis().ip())
                    .build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        redisServer.start();
    }

    public void shutdown() {
        if (redisServer != null) {
            redisServer.stop();
        }
    }
}
