package de.redstonecloud.cloud.cluster;

import de.redstonecloud.api.RCClusteringProto;
import io.grpc.stub.StreamObserver;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

@AllArgsConstructor
@Getter
public class ClusterNode {
    private String name;
    private String id;
    private String token;
    @Setter private StreamObserver<RCClusteringProto.Payload> stream;
    @Setter private boolean shuttingDown;
    @Setter private String address;
    @Setter private volatile long lastSeen = System.currentTimeMillis();
    private final Queue<RCClusteringProto.Payload> pending = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean draining = new AtomicBoolean(false);
    private final ExecutorService sendExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "ClusterNode-Sender-" + id);
        t.setDaemon(true);
        return t;
    });

    public ClusterNode(String name,
                       String id,
                       String token,
                       StreamObserver<RCClusteringProto.Payload> stream,
                       boolean shuttingDown,
                       String address) {
        this(name, id, token, stream, shuttingDown, address, System.currentTimeMillis());
    }

    public void send(RCClusteringProto.Payload envelope) {
        if (stream == null || shuttingDown) {
            return;
        }

        if (envelope == null) {
            return;
        }

        if (pending.size() >= 500) {
            pending.poll();
        }
        pending.add(envelope);
        drainPending();
    }

    public void touch() {
        lastSeen = System.currentTimeMillis();
    }

    private void drainPending() {
        if (stream == null || shuttingDown) {
            return;
        }
        if (!draining.compareAndSet(false, true)) {
            return;
        }
        sendExecutor.execute(() -> {
            try {
                while (stream != null && !shuttingDown) {
                    RCClusteringProto.Payload payload = pending.poll();
                    if (payload == null) {
                        break;
                    }
                    stream.onNext(payload);
                }
            } catch (Exception e) {
                stream = null;
                System.err.println("Failed to send to node " + id + ": " + e.getMessage());
            } finally {
                draining.set(false);
                if (stream != null && !shuttingDown && !pending.isEmpty()) {
                    drainPending();
                }
            }
        });
    }
}
