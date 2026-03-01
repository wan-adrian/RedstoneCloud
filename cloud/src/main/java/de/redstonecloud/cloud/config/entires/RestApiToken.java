package de.redstonecloud.cloud.config.entires;

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
public class RestApiToken extends OkaeriConfig {
    @Comment("Name for this token (for logs/identification)")
    String name = "default-admin";

    @Comment("API token value. Use a long random secret in production")
    String token = "CHANGE_ME";

    @Comment("Permissions for this token. Use '*' for full access")
    List<String> permissions = new ArrayList<>(List.of("*"));

    @Comment("Whether this token is enabled")
    boolean enabled = true;
}
