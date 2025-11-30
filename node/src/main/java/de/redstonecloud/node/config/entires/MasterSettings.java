package de.redstonecloud.node.config.entires;

import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(fluent = true)
public class MasterSettings extends OkaeriConfig {
    @Comment("IP address of this master node")
    private String ip = "192.168.2.xxx";
    @Comment("Port of this master node")
    private int port = 6854;
}