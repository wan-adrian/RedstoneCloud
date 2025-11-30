package de.redstonecloud.node.cluster;

import de.redstonecloud.api.*;
import de.redstonecloud.api.RCBootProto.Status;
import de.redstonecloud.api.RCBootServiceGrpc;
import de.redstonecloud.api.RCClusteringProto.*;

import de.redstonecloud.node.config.NodeConfig;
import de.redstonecloud.node.config.entry.MasterEntry;

import de.redstonecloud.node.server.NodeServerManager;
import io.grpc.ConnectivityState;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import java.util.concurrent.TimeUnit;

@Log4j2
@Getter
public class ClusterClient {
    private static ClusterClient INSTANCE;

    private final MasterEntry masterEntry;
    private ManagedChannel channel;

    private StreamObserver<RCClusteringProto.Payload> outbound;   // Master → Node stream
    private volatile boolean running = true;
    private volatile boolean loggedIn = false;

    private volatile String token;
    @Setter
    private volatile boolean streamActive = false;

    public static ClusterClient getInstance() {
        if (INSTANCE == null) INSTANCE = new ClusterClient();
        return INSTANCE;
    }

    private ClusterClient() {
        this.masterEntry = NodeConfig.getMasterSettings();
    }

    /* ====================================================================
       START
       ==================================================================== */

    public void start() {
        connect();
        new Thread(this::connectionMonitorLoop, "Cluster-Reconnect-Thread").start();
    }

    /* ====================================================================
       CONNECT + LOGIN + STREAM
       ==================================================================== */

    private void connect() {
        try {
            if (channel != null && !channel.isShutdown()) channel.shutdownNow();

            log.info("Connecting to master {}:{} ...", masterEntry.ip(), masterEntry.port());

            channel = ManagedChannelBuilder
                    .forAddress(masterEntry.ip(), masterEntry.port())
                    .usePlaintext()
                    .build();

            doLogin();
        } catch (Exception e) {
            log.error("Connection failed: {}", e.getMessage());
            loggedIn = false;
        }
    }

    /* ---------------------------- LOGIN (existing) ---------------------------- */

    private void doLogin() {
        try {
            RCBootServiceGrpc.RCBootServiceBlockingStub stub =
                    RCBootServiceGrpc.newBlockingStub(channel);

            RCBootProto.AuthRequest req = RCBootProto.AuthRequest.newBuilder()
                    .setNodeId(NodeConfig.getNodeId())
                    .setHostname(NodeConfig.getHost())
                    .build();

            RCBootProto.LoginResponse res = stub.login(req);

            if (res.getStatus() != Status.SUCCESS) {
                log.error("Login failed: {}", res.getMessage());
                loggedIn = false;
                return;
            }

            this.token = res.getToken();
            this.loggedIn = true;

            NodeServerManager.getInstance().reloadServerTypes(res.getTypesList().stream().map(t -> t.getConfig()).toList());
            NodeServerManager.getInstance().reloadTemplates(res.getTemplatesList().stream().map(t -> t.getData()).toList());

            log.info("Logged into master successfully. Token={}", token);

            openStream();
        } catch (Exception e) {
            log.error("Login error: {}", e.getMessage());
            loggedIn = false;
        }
    }

    /* ---------------------------- OPEN STREAM ---------------------------- */

    private void openStream() {
        streamActive = false;

        outbound = RCClusteringServiceGrpc.newStub(channel)
                .communicate(new InboundHandler());

        // Register node
        outbound.onNext(
                RCClusteringProto.Payload.newBuilder()
                        .setRegisterNode(
                                RCClusteringProto.RegisterNode.newBuilder()
                                        .setNodeId(NodeConfig.getNodeId())
                                        .setToken(token)
                        )
                        .build()
        );

        log.info("Opened bidirectional stream to master.");
        streamActive = true;
    }

    /* ====================================================================
       CONNECTION MONITOR
       ==================================================================== */

    private void connectionMonitorLoop() {
        int retryDelay = 1;

        while (running) {
            try {
                ConnectivityState state = channel.getState(true);

                if (state == ConnectivityState.READY && streamActive) {
                    retryDelay = 1;
                    Thread.sleep(1000);
                    continue;
                }

                if (state != ConnectivityState.READY) {
                    log.warn("Master connection lost ({}). Reconnecting...", state);

                    connect();

                    if (loggedIn && streamActive) log.info("Reconnected!");
                    else log.warn("Reconnect failed. Retrying in {}s", retryDelay);

                    Thread.sleep(retryDelay * 1000L);
                    retryDelay = Math.min(retryDelay * 2, 30);
                }

                Thread.sleep(500);

            } catch (Exception e) {
                log.error("Cluster monitor error: {}", e.getMessage());
            }
        }
    }

    /* ====================================================================
       DISCONNECT / SHUTDOWN
       ==================================================================== */

    public void shutdown() {
        running = false;

        if (outbound != null) {
            try { outbound.onCompleted(); } catch (Exception ignored) {}
        }

        if (channel != null && !channel.isShutdown()) {
            channel.shutdown();
            try { channel.awaitTermination(3, TimeUnit.SECONDS); }
            catch (InterruptedException ignored) {}
        }

        log.info("Cluster client shut down.");
    }

    public void sendServerDied(String server) {
        if (!streamActive) {
            System.out.println("Stream not active, cannot send serverDied for " + server);
            return;
        }

        outbound.onNext(
                Payload.newBuilder()
                        .setServerDied(
                                ServerDied.newBuilder()
                                        .setToken(token)
                                        .setServer(server)
                        )
                        .build()
        );
    }

    public void sendServerPort(String server, int port) {
        if (!streamActive) {
            System.out.println("Stream not active, cannot send serverDied for " + server);
            return;
        }

        outbound.onNext(
                Payload.newBuilder()
                        .setServerPortSet(
                                ServerPortSet.newBuilder()
                                        .setToken(token)
                                        .setServer(server)
                                        .setPort(port)
                        )
                        .build()
        );
    }

    public void sendStatus(String server, String status) {
        if (!streamActive) return;

        outbound.onNext(
                Payload.newBuilder()
                        .setServerStatusChange(
                                ServerStatusChange.newBuilder()
                                        .setToken(token)
                                        .setServer(server)
                                        .setStatus(status)
                        )
                        .build()
        );
    }
}
