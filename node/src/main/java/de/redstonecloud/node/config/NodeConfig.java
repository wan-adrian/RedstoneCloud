package de.redstonecloud.node.config;

import de.redstonecloud.node.config.entires.MasterSettings;
import de.redstonecloud.node.config.entires.NodeSettings;
import de.redstonecloud.node.config.entires.RedisSettings;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Accessors;

@EqualsAndHashCode(callSuper = true)
@Getter
@Accessors(fluent = true)
public final class NodeConfig extends OkaeriConfig {
    @Comment("Node settings")
    private NodeSettings node = new NodeSettings();

    @Comment("Master settings")
    private MasterSettings master = new MasterSettings();

    @Comment("Redis settings")
    private RedisSettings redis = new RedisSettings();

    @Comment("Enable or disable debug logging")
    private boolean debug = false;
}