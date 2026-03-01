package de.redstonecloud.cloud.cluster;

import de.redstonecloud.cloud.RedstoneCloud;
import de.redstonecloud.cloud.cluster.grpc.RCBootServiceImpl;
import de.redstonecloud.cloud.cluster.grpc.RCClusterServiceImpl;
import de.redstonecloud.cloud.config.entires.ClusterSettings;
import de.redstonecloud.cloud.config.types.Node;
import lombok.Getter;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeUnit;

@Log4j2
@Getter
public class ClusterManager {
    private Server server;
    private static ClusterManager INSTANCE = null;
    private static Map<String, ClusterNode> idNodes = new ConcurrentHashMap<>();
    private static Map<String, String> idNameMap = new ConcurrentHashMap<>();
    private final ScheduledExecutorService heartbeatExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "Cluster-Heartbeat-Monitor");
        t.setDaemon(true);
        return t;
    });
    private static final long HEARTBEAT_TIMEOUT_MS = 15_000L;

    private ClusterManager() {
        log.warn("Experimental cluster mode is enabled. This feature is not stable yet and may cause data loss or other issues.");
        for (Node node : RedstoneCloud.getConfig().cluster().nodes()) {
            idNameMap.put(node.id(), node.name());
        }
        heartbeatExecutor.scheduleAtFixedRate(this::checkHeartbeats, 10, 10, TimeUnit.SECONDS);
        heartbeatExecutor.scheduleAtFixedRate(this::sendPing, 10, 10, TimeUnit.SECONDS);
    }

    public static boolean isCluster() {
        return INSTANCE != null;
    }

    public static ClusterManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ClusterManager();
        }
        return INSTANCE;
    }

    public void addNode(ClusterNode node) {
        if (node == null || node.getId() == null) {
            return;
        }
        idNodes.put(node.getId(), node);
    }

    public boolean hasNode(String id) {
        return idNodes.containsKey(id);
    }

    public boolean isAllowedNode(String id) {
        return idNameMap.containsKey(id);
    }

    public ClusterNode getNodeById(String id) {
        return idNodes.get(id);
    }

    public ClusterNode getNodeByToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        for (ClusterNode node : idNodes.values()) {
            if (node != null && token.equals(node.getToken())) {
                return node;
            }
        }
        return null;
    }

    public List<ClusterNode> getNodes() {
        return new ArrayList<>(idNodes.values());
    }

    public void startServer() {
        ClusterSettings clusterSettings = RedstoneCloud.getConfig().cluster();
        try {
            server = ServerBuilder.forPort(clusterSettings.port())
                    .permitKeepAliveWithoutCalls(true)
                    .permitKeepAliveTime(10, TimeUnit.SECONDS)
                    .keepAliveTime(20, TimeUnit.SECONDS)
                    .keepAliveTimeout(10, TimeUnit.SECONDS)
                    .addService(new RCBootServiceImpl())
                    .addService(new RCClusterServiceImpl())
                    //.intercept(new TokenCheck("TOKEN HERE"))
                    .build()
                    .start();

            log.info("Node server is listening on {}", clusterSettings.port());

        } catch (IOException e) {
            log.error("Failed to start gRPC server", e);
        }
    }

    public void stopServer() {
        for (ClusterNode node : idNodes.values()) {
            if (node == null) {
                continue;
            }
            node.setShuttingDown(true);
        }
        heartbeatExecutor.shutdownNow();
        if (server != null && !server.isShutdown()) {
            server.shutdown();
        }
    }

    public void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    public String getNodeNameById(String id) {
        return idNameMap.get(id);
    }

    public void cleanupNode(String nodeId) {
        ClusterNode node = idNodes.get(nodeId);
        if (node != null) {
            node.setStream(null);
            node.setShuttingDown(true);
            log.info("Marked node {} as disconnected", nodeId);
        }
    }

    private void checkHeartbeats() {
        long now = System.currentTimeMillis();
        for (ClusterNode node : idNodes.values()) {
            if (node == null) {
                continue;
            }
            if (node.getStream() == null || node.isShuttingDown()) {
                continue;
            }
            if (now - node.getLastSeen() > HEARTBEAT_TIMEOUT_MS) {
                log.warn("Heartbeat timeout for node {} ({}), marking as disconnected", node.getId(), node.getName());
                cleanupNode(node.getId());
            }
        }
    }

    private void sendPing() {
        for (ClusterNode node : idNodes.values()) {
            if (node == null) {
                continue;
            }
            if (node.getStream() == null || node.isShuttingDown()) {
                continue;
            }
            if (node.getToken() == null || node.getToken().isBlank()) {
                continue;
            }
            node.send(de.redstonecloud.api.RCClusteringProto.Payload.newBuilder()
                    .setPing(de.redstonecloud.api.RCClusteringProto.Ping.newBuilder()
                            .setToken(node.getToken())
                            .build())
                    .build());
        }
    }
}
