package de.redstonecloud.shared.files.template;

import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(fluent = true)
public class TemplateInfo extends OkaeriConfig {
    @Comment("Name of the template")
    String name;

    @Comment("Type of the template")
    String type;

    @Comment("Whether or not this server is static (not-temporary)")
    boolean isStatic = false;

    @Comment("Seperator between template name and number (e.g. Lobby-1)")
    String seperator = "-";

    @Comment("Node to run the template on (empty = master node)")
    String node = "";
}