package de.redstonecloud.shared.files;

import de.redstonecloud.shared.files.template.TemplateBehavior;
import de.redstonecloud.shared.files.template.TemplateInfo;
import de.redstonecloud.shared.files.type.TypeDownloads;
import de.redstonecloud.shared.files.type.TypeInfo;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Accessors;

@EqualsAndHashCode(callSuper = true)
@Getter
@Accessors(fluent = true)
public final class TypeConfig extends OkaeriConfig {
    @Comment("Generic info")
    TypeInfo info;

    @Comment("Download URLs of certain modules")
    TypeDownloads downloads;
}
