package de.redstonecloud.cloud.config.types;

import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(fluent = true)
public class Node extends OkaeriConfig {
    @Comment("Name of node")
    String name = "RC Node";

    @Comment("ID of node")
    String id = UUID.randomUUID().toString();
}