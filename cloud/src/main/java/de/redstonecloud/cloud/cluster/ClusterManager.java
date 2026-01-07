package de.redstonecloud.cloud.cluster;

import de.redstonecloud.cloud.RedstoneCloud;
import de.redstonecloud.cloud.cluster.grpc.RCBootServiceImpl;
import de.redstonecloud.cloud.cluster.grpc.RCClusterServiceImpl;
import de.redstonecloud.cloud.config.CloudConfig;
import de.redstonecloud.cloud.config.entires.ClusterSettings;
import de.redstonecloud.cloud.config.entires.RedisSettings;
import de.redstonecloud.cloud.config.types.Node;
import de.redstonecloud.cloud.server.ServerManager;
import lombok.Getter;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Log4j2
@Getter
public class ClusterManager {
    private Server server;
    private static ClusterManager INSTANCE = null;
    private static List<ClusterNode> nodes = new ArrayList<>();
    private static Map<String, ClusterNode> tokenNodes = new HashMap<>();
    private static Map<String, ClusterNode> idNodes = new HashMap<>();
    private static Map<String, String> idNameMap = new HashMap<>();

    private ClusterManager() {
        log.warn("Experimental cluster mode is enabled. This feature is not stable yet and may cause data loss or other issues.");
        for (Node node : RedstoneCloud.getConfig().cluster().nodes()) {
            idNameMap.put(node.id(), node.name());
        }
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
        nodes.add(node);
        tokenNodes.put(node.getToken(), node);
        idNodes.put(node.getId(), node);
    }

    public boolean hasNode(String id) {
        return idNodes.containsKey(id);
    }

    public ClusterNode getNodeById(String id) {
        return idNodes.get(id);
    }

    public List<ClusterNode> getNodes() {
        return new ArrayList<>(nodes);
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
        //TODO: send message to all connected nodes

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
            nodes.remove(node);
            tokenNodes.remove(node.getToken());
            idNodes.remove(nodeId);

            log.info("Cleaned up node {}", nodeId);
        }
    }
}
