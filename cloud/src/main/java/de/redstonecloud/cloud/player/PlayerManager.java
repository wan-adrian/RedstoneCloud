package de.redstonecloud.cloud.player;

import de.redstonecloud.api.redis.cache.Cache;
import de.redstonecloud.api.util.Keys;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerManager {
    @Getter
    private static PlayerManager instance;
    @Getter
    public Map<UUID, CloudPlayer> players = new HashMap<>();
    @Getter
    public Map<String, CloudPlayer> playersByName = new HashMap<>();

    public PlayerManager() {
        instance = this;
    }

    public void addPlayer(CloudPlayer player) {
        players.put(player.getUUID(), player);
        playersByName.put(player.getName(), player);

        player.updateCache();
    }

    public void removePlayer(String uuid) {
        CloudPlayer p = players.remove(uuid);
        if (p != null) {
            playersByName.remove(p.getName());
            p.resetCache();
        } else {
            new Cache().delete(Keys.CACHE_PREFIX_PLAYER + uuid);
        }
    }

    public CloudPlayer getPlayer(UUID uuid) {
        return players.get(uuid);
    }

    public CloudPlayer getPlayer(String name) {
        return playersByName.get(name);
    }
}