package de.redstonecloud.cloud.cluster;

import de.redstonecloud.cloud.cluster.grpc.RCBootServiceImpl;
import de.redstonecloud.cloud.cluster.grpc.RCClusterServiceImpl;
import de.redstonecloud.cloud.cluster.grpc.RCNode;
import de.redstonecloud.cloud.config.CloudConfig;
import de.redstonecloud.cloud.config.entry.ClusterServerEntry;
import lombok.Getter;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Log4j2
@Getter
public class ClusterManager {
    private Server server;
    private static ClusterManager INSTANCE = null;
    private static List<ClusterNode> nodes = new ArrayList<>();
    private static Map<String, ClusterNode> tokenNodes = new HashMap<>();
    private static Map<String, ClusterNode> idNodes = new HashMap<>();

    private ClusterManager() {

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

    public void startServer() {
        ClusterServerEntry cfg = CloudConfig.getClusterServer();
        try {
            server = ServerBuilder.forPort(cfg.port())
                    .addService(new RCBootServiceImpl())
                    .addService(new RCClusterServiceImpl())
                    //.intercept(new TokenCheck("TOKEN HERE"))
                    .build()
                    .start();

            log.info("Node server is listening on {}", cfg.port());

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
}
