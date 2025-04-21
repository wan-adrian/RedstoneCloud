package de.redstonecloud.api.redis.cache;

public interface Cacheable {
    default void updateCache() {
        new Cache().set(cacheKey(), this.toString(), expireSeconds());
    }

    default void resetCache() {
        new Cache().delete(cacheKey());
    }

    default long expireSeconds() {
        return -1L;
    }

    String cacheKey();
}
