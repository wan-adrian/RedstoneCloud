package de.redstonecloud.node.cluster.grpc;

import de.redstonecloud.node.cluster.ClusterClient;

public class RCMaster {
    public static void serverDied(String serverName) {
        ClusterClient.getInstance().sendServerDied(serverName);
    }
    public static void port(String serverName, int port) {
        ClusterClient.getInstance().sendServerPort(serverName, port);
    }
}
