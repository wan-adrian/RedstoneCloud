package de.redstonecloud.shared.files.type;

import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(fluent = true)
public class TypeDownloads extends OkaeriConfig {
    String software = "";
    String bridge = "https://github.com/RedstoneCloud/CloudBridge/releases/download/snapshot/cloudbridge.jar";
}