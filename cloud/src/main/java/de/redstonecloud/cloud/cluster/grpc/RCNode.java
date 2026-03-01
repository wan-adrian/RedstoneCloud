package de.redstonecloud.cloud.cluster.grpc;

import de.redstonecloud.api.RCClusteringProto;
import de.redstonecloud.api.RCGenericProto;
import de.redstonecloud.cloud.cluster.ClusterManager;
import de.redstonecloud.cloud.cluster.ClusterNode;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
public class RCNode {
    private String nodeId;

    public RCNode(String nodeId) {
        this.nodeId = nodeId;
    }

    public void prepareServer(String template, String name, Map<String, String> env) {
        ClusterNode node = getNode();
        if (node == null || node.getToken() == null || node.getStream() == null || node.isShuttingDown()) {
            return;
        }
        List<RCGenericProto.KeyValuePair> envList = env.entrySet().stream()
                .map(entry -> RCGenericProto.KeyValuePair.newBuilder()
                        .setKey(entry.getKey())
                        .setValue(entry.getValue())
                        .build())
                .toList();

        ClusterManager.getInstance().getNodeById(nodeId).send(RCClusteringProto.Payload.newBuilder()
                        .setPrepareServer(RCClusteringProto.PrepareServer.newBuilder()
                                .setToken(node.getToken())
                                .setTemplate(template)
                                .setName(name)
                                .addAllEnv(envList)
                                .build())
                .build());
    }

    public void startServer(String server) {
        ClusterNode node = getNode();
        if (node == null || node.getToken() == null || node.getStream() == null || node.isShuttingDown()) {
            return;
        }
        ClusterManager.getInstance().getNodeById(nodeId).send(RCClusteringProto.Payload.newBuilder()
                        .setStartServer(RCClusteringProto.StartServer.newBuilder()
                                .setToken(node.getToken())
                                .setServer(server)
                                .build())
                .build());
    }

    public void stopServer(String server, boolean kill) {
        ClusterNode node = getNode();
        if (node == null || node.getToken() == null || node.getStream() == null || node.isShuttingDown()) {
            return;
        }
        ClusterManager.getInstance().getNodeById(nodeId).send(RCClusteringProto.Payload.newBuilder()
                        .setStopServer(RCClusteringProto.StopServer.newBuilder()
                                .setToken(node.getToken())
                                .setServer(server)
                                .setKill(kill)
                                .build())
                .build());
    }

    public void executeCommand(String server, String command) {
        ClusterNode node = getNode();
        if (node == null || node.getToken() == null || node.getStream() == null || node.isShuttingDown()) {
            return;
        }
        ClusterManager.getInstance().getNodeById(nodeId).send(RCClusteringProto.Payload.newBuilder()
                        .setExecuteCommand(RCClusteringProto.ExecuteCommand.newBuilder()
                                .setToken(node.getToken())
                                .setServer(server)
                                .setCommand(command)
                                .build())
                .build());
    }

    public void updateServerStatus(String name, String status) {
        ClusterNode node = getNode();
        if (node == null || node.getToken() == null || node.getStream() == null || node.isShuttingDown()) {
            return;
        }
        ClusterManager.getInstance().getNodeById(nodeId).send(RCClusteringProto.Payload.newBuilder()
                        .setServerStatusChange(RCClusteringProto.ServerStatusChange.newBuilder()
                                .setToken(node.getToken())
                                .setServer(name)
                                .setStatus(status)
                                .build())
                .build());
    }

    private ClusterNode getNode() {
        return ClusterManager.getInstance().getNodeById(nodeId);
    }
}
