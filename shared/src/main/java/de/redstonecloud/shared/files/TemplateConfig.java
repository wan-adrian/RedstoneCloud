package de.redstonecloud.shared.files;

import de.redstonecloud.shared.files.template.TemplateBehavior;
import de.redstonecloud.shared.files.template.TemplateInfo;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Accessors;

@EqualsAndHashCode(callSuper = true)
@Getter
@Accessors(fluent = true)
public final class TemplateConfig extends OkaeriConfig {
    @Comment("Generic info")
    TemplateInfo info;

    @Comment("Behavior")
    TemplateBehavior behavior;
}
