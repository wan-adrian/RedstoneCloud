package de.redstonecloud.shared.utils;

import de.redstonecloud.shared.server.Server;
import lombok.Getter;
import lombok.Setter;

public class CurrentInstance {
    @Getter
    @Setter
    public static String NODE_ID = "";

    @Getter
    @Setter
    public static Server currentLogServer = null;
}
