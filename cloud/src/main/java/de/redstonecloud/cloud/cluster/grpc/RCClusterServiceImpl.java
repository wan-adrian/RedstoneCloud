package de.redstonecloud.cloud.cluster.grpc;

import de.redstonecloud.api.RCClusteringProto;
import de.redstonecloud.api.RCClusteringProto.*;
import de.redstonecloud.api.RCClusteringServiceGrpc;
import de.redstonecloud.api.components.ServerStatus;
import de.redstonecloud.cloud.RedstoneCloud;
import de.redstonecloud.cloud.cluster.ClusterManager;
import de.redstonecloud.cloud.cluster.ClusterNode;
import de.redstonecloud.cloud.server.ServerImpl;
import de.redstonecloud.cloud.server.ServerManager;
import de.redstonecloud.shared.server.Server;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class RCClusterServiceImpl extends RCClusteringServiceGrpc.RCClusteringServiceImplBase {

    @Override
    public StreamObserver<Payload> communicate(StreamObserver<Payload> outbound) {

        return new StreamObserver<>() {
            private String nodeId;

            @Override
            public void onNext(Payload msg) {
                switch (msg.getPayloadCase()) {

                    case REGISTERNODE -> {
                        nodeId = msg.getRegisterNode().getNodeId();
                        ClusterNode node = ClusterManager.getInstance().getNodeById(nodeId);
                        node.setStream(outbound);
                        node.setShuttingDown(false);

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

                    case SERVERSTATUSCHANGE -> {
                        String serverName = msg.getServerStatusChange().getServer();
                        ServerImpl server = (ServerImpl) ServerManager.getInstance().getServer(serverName);
                        if (server == null) {
                            log.warn("Received SERVERSTATUSCHANGE for unknown server: {}", serverName);
                            return;
                        }

                        ServerStatus newStatus = ServerStatus.valueOf(msg.getServerStatusChange().getStatus());
                        log.info("Received SERVERSTATUSCHANGE for server: {} to status {}", serverName, newStatus);
                        server.setStatusLocally(newStatus);
                    }

                    case NODESHUTDOWN -> {
                        ClusterNode node = ClusterManager.getInstance().getNodeById(nodeId);
                        String name = ClusterManager.getInstance().getNodeNameById(nodeId);
                        log.info("Cluster {} is shutting down.", name);

                        node.setShuttingDown(true);
                    }

                    default -> {}
                }
            }

            @Override
            public void onError(Throwable t) {
                log.warn("Cluster node connection error: {}", t.getMessage());
            }

            @Override
            public void onCompleted() {

            }

            private void disconnect() {
                if (nodeId != null) {
                    ClusterManager.getInstance().cleanupNode(nodeId);
                    log.info("Node disconnected: {} ({})", ClusterManager.getInstance().getNodeNameById(nodeId), nodeId);
                    //complete the stream
                    outbound.onCompleted();
                }
            }
        };
    }
}
