package de.redstonecloud.cloud.cluster.grpc;

import de.redstonecloud.api.RCClusteringProto;
import de.redstonecloud.api.RCClusteringProto.*;
import de.redstonecloud.api.RCClusteringServiceGrpc;
import de.redstonecloud.cloud.RedstoneCloud;
import de.redstonecloud.cloud.cluster.ClusterManager;
import de.redstonecloud.cloud.cluster.ClusterNode;
import de.redstonecloud.cloud.server.ServerImpl;
import de.redstonecloud.cloud.server.ServerManager;
import de.redstonecloud.cloud.scheduler.TaskScheduler;
import de.redstonecloud.shared.server.Server;
import de.redstonecloud.shared.server.Template;
import de.redstonecloud.api.components.ServerStatus;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
public class RCClusterServiceImpl extends RCClusteringServiceGrpc.RCClusteringServiceImplBase {

    @Override
    public StreamObserver<Payload> communicate(StreamObserver<Payload> outbound) {

        return new StreamObserver<>() {
            private String nodeId;
            private String disconnectReason = "unknown";

            @Override
            public void onNext(Payload msg) {
                try {
                    log.info("Cluster message received: {} (nodeId={})", msg.getPayloadCase(), nodeId);
                    markSeen(nodeId, tokenFrom(msg));
                    switch (msg.getPayloadCase()) {

                        case REGISTERNODE -> {
                            nodeId = msg.getRegisterNode().getNodeId();
                            ClusterNode node = ClusterManager.getInstance().getNodeById(nodeId);
                            if (node == null) {
                                log.warn("Unknown node tried to register: {}", nodeId);
                                outbound.onError(new StatusRuntimeException(Status.UNAUTHENTICATED));
                                return;
                            }
                            if (!isTokenValid(node, msg.getRegisterNode().getToken())) {
                                log.warn("Node {} provided invalid token during register", nodeId);
                                outbound.onError(new StatusRuntimeException(Status.UNAUTHENTICATED));
                                return;
                            }

                            node.setStream(outbound);
                            node.setShuttingDown(false);
                            node.touch();

                            log.info("Node connected: {} ({})", ClusterManager.getInstance().getNodeNameById(nodeId), nodeId);
                            TaskScheduler scheduler = TaskScheduler.getInstance();
                            if (scheduler != null) {
                                scheduler.scheduleTask(() ->
                                        ServerManager.getInstance().getTemplates().values().forEach(Template::checkServers));
                            } else {
                                new Thread(() ->
                                        ServerManager.getInstance().getTemplates().values().forEach(Template::checkServers),
                                        "Cluster-Template-Check").start();
                            }
                        }

                        case SERVERDIED -> {
                            if (!isTokenValid(nodeId, msg.getServerDied().getToken())) {
                                return;
                            }
                            String serverName = msg.getServerDied().getServer();
                            Server server = ServerManager.getInstance().getServer(serverName);
                            if (server == null) {
                                log.warn("Received SERVERDIED for unknown server: {}", serverName);
                                return;
                            }

                            log.info("Received SERVERDIED for server: {}", serverName);
                            Template template = server.getTemplate();
                            server.onExit();
                            if (template != null) {
                                template.checkServers();
                            }
                        }

                        case SERVERPORTSET -> {
                            if (!isTokenValid(nodeId, msg.getServerPortSet().getToken())) {
                                return;
                            }
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
                            if (!isTokenValid(nodeId, msg.getServerStatusChange().getToken())) {
                                return;
                            }
                            String serverName = msg.getServerStatusChange().getServer();
                            ServerImpl server = (ServerImpl) ServerManager.getInstance().getServer(serverName);
                            if (server == null) {
                                log.warn("Received SERVERSTATUSCHANGE for unknown server: {}", serverName);
                                return;
                            }

                            ServerStatus newStatus = ServerStatus.valueOf(msg.getServerStatusChange().getStatus());
                            log.info("Received SERVERSTATUSCHANGE for server: {} to status {}", serverName, newStatus);
                            server.setStatusLocally(newStatus);

                            if (newStatus == ServerStatus.PREPARED && !server.isLocal()) {
                                if (server.requestStart()) {
                                    server.start();
                                }
                            }
                        }

                        case NODESHUTDOWN -> {
                            ClusterNode node = ClusterManager.getInstance().getNodeById(nodeId);
                            if (node == null) {
                                return;
                            }
                            String name = ClusterManager.getInstance().getNodeNameById(nodeId);
                            log.info("Cluster {} is shutting down.", name);

                            node.setShuttingDown(true);
                        }

                        case NODESYNC -> {
                            if (!isTokenValid(nodeId, msg.getNodeSync().getToken())) {
                                return;
                            }
                            handleNodeSync(msg.getNodeSync(), nodeId);
                            markSeen(nodeId, msg.getNodeSync().getToken());
                        }

                    case HEARTBEAT -> {
                        if (!isTokenValid(nodeId, msg.getHeartbeat().getToken())) {
                            return;
                        }
                        markSeen(nodeId, msg.getHeartbeat().getToken());
                    }

                    case PONG -> {
                        if (!isTokenValid(nodeId, msg.getPong().getToken())) {
                            return;
                        }
                        markSeen(nodeId, msg.getPong().getToken());
                    }

                    default -> {}
                }
            } catch (Exception e) {
                log.error("Cluster message handling failed (nodeId={}): {}", nodeId, e.getMessage());
            }
            }

            @Override
            public void onError(Throwable t) {
                disconnectReason = "error: " + (t != null ? t.getMessage() : "null");
                log.warn("Cluster node connection error (nodeId={}): {}", nodeId, disconnectReason);
                disconnect();
            }

            @Override
            public void onCompleted() {
                disconnectReason = "completed";
                log.info("Cluster node stream completed (nodeId={})", nodeId);
                disconnect();
            }

            private void disconnect() {
                if (nodeId != null) {
                    log.info("Disconnecting nodeId={} reason={}", nodeId, disconnectReason);
                    ClusterManager.getInstance().cleanupNode(nodeId);
                    log.info("Node disconnected: {} ({})", ClusterManager.getInstance().getNodeNameById(nodeId), nodeId);
                    //complete the stream
                    outbound.onCompleted();
                }
            }

            private boolean isTokenValid(String nodeId, String token) {
                ClusterNode node = null;
                if (nodeId != null) {
                    node = ClusterManager.getInstance().getNodeById(nodeId);
                }
                if (node == null && token != null && !token.isBlank()) {
                    node = ClusterManager.getInstance().getNodeByToken(token);
                    if (node != null && this.nodeId == null) {
                        this.nodeId = node.getId();
                    }
                }
                return isTokenValid(node, token);
            }

            private boolean isTokenValid(ClusterNode node, String token) {
                if (node == null) {
                    return false;
                }
                if (node.getToken() == null || token == null || token.isBlank()) {
                    log.warn("Missing token for node {}", node.getId());
                    return false;
                }
                if (!node.getToken().equals(token)) {
                    log.warn("Invalid token for node {}", node.getId());
                    return false;
                }
                return true;
            }

            private void markSeen(String nodeId, String token) {
                ClusterNode node = null;
                if (nodeId != null && !nodeId.isBlank()) {
                    node = ClusterManager.getInstance().getNodeById(nodeId);
                }
                if (node == null && token != null && !token.isBlank()) {
                    node = ClusterManager.getInstance().getNodeByToken(token);
                    if (node != null && this.nodeId == null) {
                        this.nodeId = node.getId();
                    }
                }
                if (node != null) {
                    node.touch();
                }
            }

            private String tokenFrom(Payload msg) {
                return switch (msg.getPayloadCase()) {
                    case REGISTERNODE -> msg.getRegisterNode().getToken();
                    case SERVERDIED -> msg.getServerDied().getToken();
                    case SERVERPORTSET -> msg.getServerPortSet().getToken();
                    case SERVERSTATUSCHANGE -> msg.getServerStatusChange().getToken();
                    case NODESYNC -> msg.getNodeSync().getToken();
                    case HEARTBEAT -> msg.getHeartbeat().getToken();
                    case PONG -> msg.getPong().getToken();
                    default -> null;
                };
            }
        };
    }

    private static void handleNodeSync(RCClusteringProto.NodeSync sync, String streamNodeId) {
        if (sync == null) {
            return;
        }
        String nodeId = streamNodeId != null ? streamNodeId : sync.getNodeId();
        if (nodeId == null || nodeId.isBlank()) {
            return;
        }
        if (sync.getNodeId() != null && !sync.getNodeId().isBlank() && !nodeId.equals(sync.getNodeId())) {
            log.warn("NodeSync nodeId mismatch: stream={} payload={}", nodeId, sync.getNodeId());
        }

        ClusterNode node = ClusterManager.getInstance().getNodeById(nodeId);
        if (node == null) {
            return;
        }

        Set<String> seen = new HashSet<>();

        for (RCClusteringProto.NodeServerState state : sync.getServersList()) {
            if (state.getName() == null || state.getName().isBlank()) {
                continue;
            }
            String serverName = state.getName();
            seen.add(serverName.toUpperCase());

            Template template = ServerManager.getInstance().getTemplate(state.getTemplate());
            if (template == null) {
                log.warn("NodeSync ignored server {} due to missing template {}", serverName, state.getTemplate());
                continue;
            }

            Server existing = ServerManager.getInstance().getServer(serverName);
            if (existing == null) {
                UUID uuid = parseUuid(state.getUuid());
                String address = (state.getAddress() == null || state.getAddress().isBlank()) ? node.getAddress() : state.getAddress();

                ServerImpl server = ServerImpl.builder()
                        .name(serverName)
                        .template(template)
                        .uuid(uuid)
                        .createdAt(System.currentTimeMillis())
                        .type(template.getType())
                        .port(state.getPort())
                        .nodeId(nodeId)
                        .address(address)
                        .env(Map.of())
                        .selectedMethod(RedstoneCloud.getConfig().startMethod())
                        .build();

                ServerStatus status = parseStatus(state.getStatus());
                if (status != null) {
                    server.setStatusLocally(status);
                }

                ServerManager.getInstance().add(server);
                continue;
            }

            if (existing instanceof ServerImpl impl) {
                impl.setPort(state.getPort());
            }
            existing.setNodeId(nodeId);
            if (state.getAddress() != null && !state.getAddress().isBlank()) {
                existing.setAddress(state.getAddress());
            }
            ServerStatus status = parseStatus(state.getStatus());
            if (status != null) {
                existing.setStatusLocally(status);
                if (status == ServerStatus.PREPARED && !existing.isLocal()) {
                    if (existing.requestStart()) {
                        existing.start();
                    }
                }
            }
        }

        for (Server server : ServerManager.getInstance().getServers().values().stream().toList()) {
            if (server.getNodeId() == null || !server.getNodeId().equals(nodeId)) {
                continue;
            }
            if (!seen.contains(server.getName().toUpperCase())) {
                server.resetCache();
                ServerManager.getInstance().remove(server);
                log.info("Removed stale server {} for node {}", server.getName(), nodeId);
            }
        }
    }

    private static UUID parseUuid(String raw) {
        if (raw == null || raw.isBlank()) {
            return UUID.randomUUID();
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException e) {
            return UUID.randomUUID();
        }
    }

    private static ServerStatus parseStatus(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return ServerStatus.valueOf(raw);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
