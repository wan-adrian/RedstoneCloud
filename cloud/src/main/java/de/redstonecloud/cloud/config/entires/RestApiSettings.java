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
public class RestApiSettings extends OkaeriConfig {
    @Comment("Enable optional REST API")
    boolean enabled = false;

    @Comment("Bind address for REST API")
    String host = "127.0.0.1";

    @Comment("Bind port for REST API")
    int port = 8080;

    @Comment("Configured API tokens for access control")
    List<RestApiToken> tokens = new ArrayList<>(List.of(new RestApiToken()));
}
