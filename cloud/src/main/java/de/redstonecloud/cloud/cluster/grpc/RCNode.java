package de.redstonecloud.cloud.cluster.grpc;

import de.redstonecloud.api.RCClusteringProto;
import de.redstonecloud.cloud.cluster.ClusterManager;
import lombok.Getter;

@Getter
public class RCNode {
    private String nodeId;

    public RCNode(String nodeId) {
        this.nodeId = nodeId;
    }

    public void prepareServer(String template, String name) {
        ClusterManager.getInstance().getNodeById(nodeId).send(RCClusteringProto.Payload.newBuilder()
                        .setPrepareServer(RCClusteringProto.PrepareServer.newBuilder()
                                .setTemplate(template)
                                .setName(name)
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
}
