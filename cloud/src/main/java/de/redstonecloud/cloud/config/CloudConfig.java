package de.redstonecloud.cloud.config;

import de.redstonecloud.cloud.config.entires.BridgeSettings;
import de.redstonecloud.cloud.config.entires.ClusterSettings;
import de.redstonecloud.cloud.config.entires.RedisSettings;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import eu.okaeri.configs.annotation.CustomKey;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Accessors;

@EqualsAndHashCode(callSuper = true)
@Getter
@Accessors(fluent = true)
public final class CloudConfig extends OkaeriConfig {
    @Comment("Redis Settings")
    private RedisSettings redis = new RedisSettings();

    @Comment("Bridge Settings")
    private BridgeSettings bridge = new BridgeSettings();

    @Comment("Cluster settings")
    private ClusterSettings cluster = new ClusterSettings();
}