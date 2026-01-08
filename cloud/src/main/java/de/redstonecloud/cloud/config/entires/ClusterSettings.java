package de.redstonecloud.cloud.config.entires;

import de.redstonecloud.cloud.config.types.Node;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(fluent = true)
public class ClusterSettings extends OkaeriConfig {
    @Comment("Port for cluster management communications")
    int port = 6854;

    @Comment("Registered clusters")
    List<Node> nodes = new ArrayList();
}