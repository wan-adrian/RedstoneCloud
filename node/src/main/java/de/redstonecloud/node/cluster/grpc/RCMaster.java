package de.redstonecloud.node.cluster.grpc;

import de.redstonecloud.node.cluster.ClusterClient;

public class RCMaster {
    public static void serverDied(String serverName) {
        ClusterClient.getInstance().sendServerDied(serverName);
    }
}
