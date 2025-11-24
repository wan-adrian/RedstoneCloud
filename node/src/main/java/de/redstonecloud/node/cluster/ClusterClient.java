package de.redstonecloud.node.cluster;

import de.redstonecloud.api.RCBootProto;
import de.redstonecloud.api.RCBootProto.Status;
import de.redstonecloud.api.RCServerProto.*;
import de.redstonecloud.api.RCBootServiceGrpc;
import de.redstonecloud.api.RCServerServiceGrpc;
import de.redstonecloud.node.config.NodeConfig;
import de.redstonecloud.node.config.entry.MasterEntry;
import io.grpc.ConnectivityState;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import java.util.concurrent.TimeUnit;

@Log4j2
@Getter
public class ClusterClient {

    private volatile String token;
    private final MasterEntry masterEntry;
    private ManagedChannel channel;

    private volatile boolean running = true;
    private volatile boolean loggedIn = false;

    private StreamObserver<ServerMessage> outgoing; // send events to master

    public ClusterClient() {
        this.masterEntry = NodeConfig.getMasterSettings();
    }

    public void start() {
        connect();
        new Thread(this::connectionMonitorLoop, "Cluster-Reconnect-Thread").start();
    }

    private void connect() {
        try {
            if (channel != null && !channel.isShutdown()) {
                channel.shutdownNow();
            }

            log.info("Connecting to master {}:{}", masterEntry.ip(), masterEntry.port());

            channel = ManagedChannelBuilder
                    .forAddress(masterEntry.ip(), masterEntry.port())
                    .usePlaintext()
                    .build();

            doLogin();

            openCommandStream();

        } catch (Exception e) {
            log.error("Connection to master failed: {}", e.getMessage());
            loggedIn = false;
        }
    }

    private void doLogin() {
        try {
            RCBootServiceGrpc.RCBootServiceBlockingStub stub =
                    RCBootServiceGrpc.newBlockingStub(channel);

            RCBootProto.AuthRequest req = RCBootProto.AuthRequest.newBuilder()
                    .setNodeId(NodeConfig.getNodeId())
                    .build();

            RCBootProto.LoginResponse res = stub.login(req);

            if (res.getStatus() != Status.SUCCESS) {
                log.error("Login failed: {}", res.getMessage());
                loggedIn = false;
                return;
            }

            this.token = res.getToken();
            this.loggedIn = true;
            log.info("Successfully logged into master. Token={}", token);
        } catch (Exception e) {
            log.error("Login error: {}", e.getMessage());
            loggedIn = false;
        }
    }

    /**
     * Opens bidirectional command/event stream to master
     */
    private void openCommandStream() {
        RCServerServiceGrpc.RCServerServiceStub stub =
                RCServerServiceGrpc.newStub(channel);

        // Incoming messages from master
        StreamObserver<ServerMessage> incoming = new StreamObserver<>() {
            @Override
            public void onNext(ServerMessage msg) {
                handleServerMessage(msg);
            }

            @Override
            public void onError(Throwable t) {
                log.error("Command stream error: {}", t.getMessage());
                // Auto-reconnect handled in connectionMonitorLoop
            }

            @Override
            public void onCompleted() {
                log.warn("Master closed the command stream");
            }
        };

        // Open bidirectional stream
        outgoing = stub.serverCommandStream(incoming);
        log.info("Command stream opened to master");

        sendEvent(ServerMessage.newBuilder().setToken(token).setJoinChannel(JoinChannel.newBuilder().build()).build());
    }

    private void handleServerMessage(ServerMessage msg) {
        System.out.println("Received message from master: " + msg.toString());
        if (msg.hasMasterShutDown()) {
            log.warn("Master is shutting down, stopping node...");
            //TODO: Stop node
            channel.shutdown();
            System.exit(0);
            return;
        }

        //TODO: Implement more
    }

    /**
     * Send event to master (e.g., ServerGone)
     */
    public void sendEvent(ServerMessage msg) {
        if (outgoing != null) {
            try {
                outgoing.onNext(msg);
            } catch (Exception e) {
                log.warn("Failed to send event to master: {}", e.getMessage());
            }
        }
    }

    private void connectionMonitorLoop() {
        int retryDelay = 1;

        while (running) {
            try {
                ConnectivityState state = channel.getState(true);

                if (state == ConnectivityState.READY) {
                    retryDelay = 1; // reset backoff
                    Thread.sleep(1000);
                    continue;
                }

                if (state == ConnectivityState.TRANSIENT_FAILURE ||
                        state == ConnectivityState.SHUTDOWN ||
                        state == ConnectivityState.IDLE) {

                    log.warn("Master connection lost (state={}). Reconnecting...", state);

                    connect();

                    if (loggedIn) {
                        log.info("Reconnected successfully!");
                    } else {
                        log.warn("Reconnect + login failed. Retrying in {}s...", retryDelay);
                    }

                    Thread.sleep(retryDelay * 1000L);
                    retryDelay = Math.min(retryDelay * 2, 30); // exponential backoff
                }

                Thread.sleep(500);

            } catch (Exception e) {
                log.error("Cluster monitor error: {}", e.getMessage());
            }
        }
    }

    public void shutdown() {
        running = false;

        if (channel != null && !channel.isShutdown()) {
            channel.shutdown();
            try {
                channel.awaitTermination(3, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {
            }
        }

        log.info("Cluster client shut down.");
    }
}
