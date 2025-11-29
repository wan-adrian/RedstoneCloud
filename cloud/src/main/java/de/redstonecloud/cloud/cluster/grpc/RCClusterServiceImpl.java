package de.redstonecloud.cloud.cluster.grpc;

import de.redstonecloud.api.RCClusteringProto;
import de.redstonecloud.api.RCClusteringProto.*;
import de.redstonecloud.api.RCClusteringServiceGrpc;
import de.redstonecloud.cloud.RedstoneCloud;
import de.redstonecloud.cloud.cluster.ClusterManager;
import de.redstonecloud.cloud.server.ServerImpl;
import de.redstonecloud.cloud.server.ServerManager;
import de.redstonecloud.shared.server.Server;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class RCClusterServiceImpl extends RCClusteringServiceGrpc.RCClusteringServiceImplBase {

    private final Map<String, StreamObserver<RCClusteringProto.Payload>> nodes = new ConcurrentHashMap<>();

    @Override
    public StreamObserver<Payload> communicate(StreamObserver<Payload> outbound) {

        return new StreamObserver<>() {
            private String nodeId;

            @Override
            public void onNext(Payload msg) {
                switch (msg.getPayloadCase()) {

                    case REGISTERNODE -> {
                        nodeId = msg.getRegisterNode().getNodeId();
                        nodes.put(nodeId, outbound);
                        ClusterManager.getInstance().getNodeById(nodeId).setStream(outbound);

                        log.info("Node connected: {} ({})", ClusterManager.getInstance().getNodeNameById(nodeId), nodeId);
                    }

                    case SERVERDIED -> {
                        String serverName = msg.getServerDied().getServer();
                        Server server = ServerManager.getInstance().getServer(serverName);
                        if (server == null) {
                            log.warn("Received SERVERDIED for unknown server: {}", serverName);
                            return;
                        }

                        log.info("Received SERVERDIED for server: {}", serverName);
                        server.onExit();
                    }

                    case SERVERPORTSET -> {
                        String serverName = msg.getServerPortSet().getServer();
                        int port = msg.getServerPortSet().getPort();
                        ServerImpl server = (ServerImpl) ServerManager.getInstance().getServer(serverName);
                        if (server == null) {
                            log.warn("Received SERVERPORTSET for unknown server: {}", serverName);
                            return;
                        }

                        log.info("Received SERVERPORTSET for server: {} with port {}", serverName, port);
                        server.setPort(port);


                    }

                    default -> {}
                }
            }

            @Override
            public void onError(Throwable t) {
                disconnect();
            }

            @Override
            public void onCompleted() {
                disconnect();
            }

            private void disconnect() {
                //TODO: Check if this fixes node disconnection issues after sending 1 message
                /*if (nodeId != null) {
                    nodes.remove(nodeId);
                    System.out.println("Node disconnected: " + nodeId);
                }
                try { outbound.onCompleted(); } catch (Exception ignore) {}*/
            }
        };
    }

    /* ================================================================
       Sending messages TO specific nodes
       ================================================================ */

    public boolean sendToNode(String nodeId, Payload env) {
        StreamObserver<Payload> node = nodes.get(nodeId);
        if (node == null) return false;

        try {
            node.onNext(env);
            return true;
        } catch (Exception e) {
            nodes.remove(nodeId);
            return false;
        }
    }
}
