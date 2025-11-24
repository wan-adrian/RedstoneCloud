package de.redstonecloud.cloud.cluster.grpc;

import de.redstonecloud.api.RCServerProto.*;
import de.redstonecloud.api.RCServerServiceGrpc;
import io.grpc.stub.StreamObserver;
import lombok.extern.log4j.Log4j2;

import java.util.concurrent.ConcurrentHashMap;

@Log4j2
public class RCServerServiceImpl extends RCServerServiceGrpc.RCServerServiceImplBase {
    private final ConcurrentHashMap<String, StreamObserver<ServerMessage>> connectedNodes = new ConcurrentHashMap<>();

    @Override
    public StreamObserver<ServerMessage> serverCommandStream(StreamObserver<ServerMessage> nodeStream) {

        return new StreamObserver<>() {
            String nodeId;

            @Override
            public void onNext(ServerMessage msg) {
                if(msg.hasJoinChannel()) {
                    nodeId = msg.getToken();
                    connectedNodes.put(nodeId, nodeStream);
                    log.info("Node connected: {}", nodeId);
                }

                if (msg.hasServerGone()) {
                    ServerGone gone = msg.getServerGone();
                    log.info("Node {} reports server gone: {}", nodeId, gone.getServerId());
                }
            }

            @Override
            public void onError(Throwable t) {
                log.warn("Node {} disconnected: {}", nodeId, t.getMessage());
                connectedNodes.remove(nodeId);
            }

            @Override
            public void onCompleted() {
                log.info("Node {} completed stream", nodeId);
                connectedNodes.remove(nodeId);
            }
        };
    }

    // Utility methods to send commands
    public void sendStartServer(String nodeId, String serverId, String amount, String forcedNumber) {
        StreamObserver<ServerMessage> stream = connectedNodes.get(nodeId);
        if (stream != null) {
            CommandStartServer cmd = CommandStartServer.newBuilder()
                    .setServerId(serverId)
                    .setAmount(amount)
                    .setForcedNumber(forcedNumber)
                    .build();
            ServerMessage msg = ServerMessage.newBuilder().setStartServer(cmd).build();
            stream.onNext(msg);
            log.info("Sent StartServer command to node {}", nodeId);
        }
    }

    public void sendStopServer(String nodeId, String serverId, boolean kill) {
        StreamObserver<ServerMessage> stream = connectedNodes.get(nodeId);
        if (stream != null) {
            CommandStopServer cmd = CommandStopServer.newBuilder()
                    .setServerId(serverId)
                    .setKill(kill)
                    .build();
            ServerMessage msg = ServerMessage.newBuilder().setStopServer(cmd).build();
            stream.onNext(msg);
            log.info("Sent StopServer command to node {}", nodeId);
        }
    }

    public void sendMasterShutdown() {
        ServerMessage msg = ServerMessage.newBuilder().setMasterShutDown(MasterShutDown.newBuilder().build()).build();
        for (String nodeId : connectedNodes.keySet()) {
            StreamObserver<ServerMessage> stream = connectedNodes.get(nodeId);
            if (stream != null) {
                stream.onNext(msg);
                log.debug("Sent MasterShutdown command to node {}", nodeId);
            }
        }
    }
}

