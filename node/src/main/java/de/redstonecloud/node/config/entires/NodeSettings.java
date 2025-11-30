package de.redstonecloud.node.config.entires;

import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(fluent = true)
public class NodeSettings extends OkaeriConfig {
    @Comment("IP address of this node")
    private String address = "192.168.2.xxx";
    @Comment("ID of this node")
    private String id = "UUIDHERE";
}