package de.redstonecloud.cloud.cluster.grpc;

import de.redstonecloud.api.RCClusteringProto;
import de.redstonecloud.api.RCClusteringProto.*;
import de.redstonecloud.api.RCClusteringServiceGrpc;
import de.redstonecloud.api.RCGenericProto.*;
import de.redstonecloud.cloud.cluster.ClusterManager;
import io.grpc.stub.StreamObserver;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

                        System.out.println("Node connected: " + nodeId);

                        new RCNode(nodeId).prepareServer("aa", "aa-2");
                    }

                    case SERVERDIED -> {
                        String server = msg.getServerDied().getServer();
                        System.out.println("Node " + nodeId + " reports server died: " + server);
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
                if (nodeId != null) {
                    nodes.remove(nodeId);
                    System.out.println("Node disconnected: " + nodeId);
                }
                try { outbound.onCompleted(); } catch (Exception ignore) {}
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
