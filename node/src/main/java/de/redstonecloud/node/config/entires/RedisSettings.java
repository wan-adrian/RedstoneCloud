package de.redstonecloud.node.config.entires;

import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(fluent = true)
public class RedisSettings extends OkaeriConfig {
    @Comment("IP Address of Redis server")
    String ip = "127.0.0.1";

    @Comment("Port of Redis server")
    int port = 6379;

    @Comment("Redis Database ID")
    int dbId = 0;
}