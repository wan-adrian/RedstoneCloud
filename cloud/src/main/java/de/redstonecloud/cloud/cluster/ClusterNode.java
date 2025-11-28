package de.redstonecloud.cloud.cluster;

import de.redstonecloud.api.RCClusteringProto;
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@AllArgsConstructor
@Getter
public class ClusterNode {
    private String name;
    private String id;
    private String token;
    @Setter
    private StreamObserver<RCClusteringProto.Payload> stream;

    public void send(RCClusteringProto.Payload envelope) {
        if (stream == null) {
            throw new IllegalStateException("Node " + id + " is not connected.");
        }

        try {
            stream.onNext(envelope);
        } catch (Exception e) {
            System.err.println("Failed to send to node " + id + ": " + e.getMessage());
        }
    }
}
