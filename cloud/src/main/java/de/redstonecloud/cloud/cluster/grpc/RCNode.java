package de.redstonecloud.cloud.cluster.grpc;

import de.redstonecloud.api.RCClusteringProto;
import de.redstonecloud.api.RCGenericProto;
import de.redstonecloud.cloud.cluster.ClusterManager;
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
        List<RCGenericProto.KeyValuePair> envList = env.entrySet().stream()
                .map(entry -> RCGenericProto.KeyValuePair.newBuilder()
                        .setKey(entry.getKey())
                        .setValue(entry.getValue())
                        .build())
                .toList();

        ClusterManager.getInstance().getNodeById(nodeId).send(RCClusteringProto.Payload.newBuilder()
                        .setPrepareServer(RCClusteringProto.PrepareServer.newBuilder()
                                .setTemplate(template)
                                .setName(name)
                                .addAllEnv(envList)
                                .build())
                .build());
    }

    public void startServer(String server) {
        ClusterManager.getInstance().getNodeById(nodeId).send(RCClusteringProto.Payload.newBuilder()
                        .setStartServer(RCClusteringProto.StartServer.newBuilder()
                                .setServer(server)
                                .build())
                .build());
    }

    public void stopServer(String server, boolean kill) {
        ClusterManager.getInstance().getNodeById(nodeId).send(RCClusteringProto.Payload.newBuilder()
                        .setStopServer(RCClusteringProto.StopServer.newBuilder()
                                .setServer(server)
                                .setKill(kill)
                                .build())
                .build());
    }

    public void executeCommand(String server, String command) {
        ClusterManager.getInstance().getNodeById(nodeId).send(RCClusteringProto.Payload.newBuilder()
                        .setExecuteCommand(RCClusteringProto.ExecuteCommand.newBuilder()
                                .setServer(server)
                                .setCommand(command)
                                .build())
                .build());
    }

    public void updateServerStatus(String name, String status) {
        ClusterManager.getInstance().getNodeById(nodeId).send(RCClusteringProto.Payload.newBuilder()
                        .setServerStatusChange(RCClusteringProto.ServerStatusChange.newBuilder()
                                .setServer(name)
                                .setStatus(status)
                                .build())
                .build());
    }
}
