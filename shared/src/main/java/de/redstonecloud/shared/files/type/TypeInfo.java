package de.redstonecloud.shared.files.type;

import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.checkerframework.checker.units.qual.C;

@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(fluent = true)
public class TypeInfo extends OkaeriConfig {
    @Comment("Name of the type")
    String name;

    @Comment("Start command")
    String startCommand = "java -jar server.jar";

    @Comment("Relative path to servers log file")
    String logFile = "logs/server.log";

    @Comment("Is proxy?")
    boolean isProxy = false;

    @Comment("Command to stop the server")
    String stopCommand = "stop";

    @Comment("File to set port in")
    String portFile = "server.properties";

    @Comment("Port placeholder (gets replaced with actual port)")
    String portPlaceholder = "[port]";
}