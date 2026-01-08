package de.redstonecloud.node.cluster.grpc;

import de.redstonecloud.node.cluster.ClusterClient;

public class RCMaster {
    public static void serverDied(String serverName) {
        ClusterClient.getInstance().sendServerDied(serverName);
    }
    public static void port(String serverName, int port) {
        ClusterClient.getInstance().sendServerPort(serverName, port);
    }
    public static void statusChange(String serverName, String status) {
        ClusterClient.getInstance().sendStatus(serverName, status);
    }
    public static void shutdownNode() {
        ClusterClient.getInstance().sendShutdownNode();
    }
}
