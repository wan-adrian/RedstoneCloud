package de.redstonecloud.node.cluster;

import de.redstonecloud.api.*;
import de.redstonecloud.api.RCBootProto.Status;
import de.redstonecloud.api.RCBootServiceGrpc;
import de.redstonecloud.api.RCClusteringProto.*;

import de.redstonecloud.node.RedstoneNode;
import de.redstonecloud.node.config.entires.MasterSettings;
import de.redstonecloud.node.config.entires.RedisSettings;
import de.redstonecloud.node.server.NodeServerManager;
import de.redstonecloud.shared.server.Server;
import de.redstonecloud.api.util.Keys;
import io.grpc.ConnectivityState;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Log4j2
@Getter
public class ClusterClient {
    private static ClusterClient INSTANCE;

    private final MasterSettings masterEntry;
    private ManagedChannel channel;

    private StreamObserver<RCClusteringProto.Payload> outbound;   // Master → Node stream
    private volatile boolean running = true;
    private volatile boolean loggedIn = false;

    private volatile String token;
    @Setter
    private volatile boolean streamActive = false;

    private volatile ConnectivityState lastState = null;
    private volatile long lastStreamAttemptMs = 0L;

    private final Queue<RCClusteringProto.Payload> pending = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean draining = new AtomicBoolean(false);
    private final ExecutorService sendExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Cluster-Stream-Sender");
        t.setDaemon(true);
        return t;
    });
    private final ScheduledExecutorService heartbeatExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "Cluster-Heartbeat");
        t.setDaemon(true);
        return t;
    });

    public static ClusterClient getInstance() {
        if (INSTANCE == null) INSTANCE = new ClusterClient();
        return INSTANCE;
    }

    private ClusterClient() {
        this.masterEntry = RedstoneNode.getConfig().master();
    }

    /* ====================================================================
       START
       ==================================================================== */

    public void start() {
        connect();
        new Thread(this::connectionMonitorLoop, "Cluster-Reconnect-Thread").start();
        heartbeatExecutor.scheduleAtFixedRate(this::sendHeartbeat, 10, 10, TimeUnit.SECONDS);
        heartbeatExecutor.scheduleAtFixedRate(this::sendNodeSync, 15, 30, TimeUnit.SECONDS);
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
                    .keepAliveWithoutCalls(true)
                    .keepAliveTime(10, TimeUnit.SECONDS)
                    .keepAliveTimeout(3, TimeUnit.SECONDS)
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
                    .setNodeId(RedstoneNode.getConfig().node().id())
                    .setHostname(RedstoneNode.getConfig().node().address())
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
            applyRedisSettings(res);

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
        lastStreamAttemptMs = System.currentTimeMillis();

        outbound = RCClusteringServiceGrpc.newStub(channel)
                .communicate(new InboundHandler());

        // Register node (queued to avoid concurrent onNext)
        sendOrQueue(
                RCClusteringProto.Payload.newBuilder()
                        .setRegisterNode(
                                RCClusteringProto.RegisterNode.newBuilder()
                                        .setNodeId(RedstoneNode.getConfig().node().id())
                                        .setToken(token)
                        )
                        .build()
        );

        onStreamReady();
        sendHeartbeat();
        log.info("Opened bidirectional stream to master.");
    }

    private void applyRedisSettings(RCBootProto.LoginResponse res) {
        String redisIp = res.getRedisIp();
        int redisPort = res.getRedisPort();
        int redisDb = res.getRedisDb();

        if (redisIp == null || redisIp.isBlank() || redisPort <= 0) {
            return;
        }

        RedisSettings redis = RedstoneNode.getConfig().redis();
        boolean changed = !redisIp.equals(redis.ip()) || redisPort != redis.port() || redisDb != redis.dbId();
        if (!changed) {
            return;
        }

        log.warn("Updating node redis settings to match master: {}:{} (db={})", redisIp, redisPort, redisDb);
        redis.ip(redisIp);
        redis.port(redisPort);
        redis.dbId(redisDb);
        RedstoneNode.getConfig().save();

        System.setProperty(Keys.PROPERTY_REDIS_PORT, String.valueOf(redisPort));
        System.setProperty(Keys.PROPERTY_REDIS_IP, redis.connectIp());
        System.setProperty(Keys.PROPERTY_REDIS_DB, String.valueOf(redisDb));
    }

    /* ====================================================================
       CONNECTION MONITOR
       ==================================================================== */

    private void connectionMonitorLoop() {
        int retryDelay = 1;

        while (running) {
            try {
                ConnectivityState state = channel.getState(true);

                if (state == ConnectivityState.READY) {
                    if (streamActive) {
                        lastState = ConnectivityState.READY;
                        retryDelay = 1;
                        Thread.sleep(1000);
                        continue;
                    }
                    if (loggedIn) {
                        long now = System.currentTimeMillis();
                        if (now - lastStreamAttemptMs >= 10_000L) {
                            log.info("Cluster stream inactive, reopening.");
                            openStream();
                        }
                        Thread.sleep(1000);
                        continue;
                    }
                    doLogin();
                    Thread.sleep(1000);
                    continue;
                }

                if (state != ConnectivityState.READY) {
                    if (lastState != state) {
                        log.warn("Master connection lost ({}). Reconnecting...", state);
                        lastState = state;
                    }

                    connect();

                    if (loggedIn && streamActive) {
                        log.info("Reconnected!");
                    } else if (lastState != ConnectivityState.READY) {
                        log.warn("Reconnect failed. Retrying in {}s", retryDelay);
                    }

                    Thread.sleep(10_000L);
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

        sendExecutor.shutdownNow();
        heartbeatExecutor.shutdownNow();
        log.info("Cluster client shut down.");
    }

    public void sendServerDied(String server) {
        sendOrQueue(
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
        sendOrQueue(
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
        sendOrQueue(
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

    public void sendShutdownNode() {
        sendOrQueue(
                Payload.newBuilder()
                        .setNodeShutdown(
                                NodeShutdown.newBuilder().build()
                        )
                        .build()
        );
    }

    public void onStreamReady() {
        if (streamActive) {
            return;
        }
        streamActive = true;
        sendNodeSync();
        flushPending();
    }

    private void sendHeartbeat() {
        if (!streamActive || token == null || token.isBlank()) {
            return;
        }
        sendOrQueue(Payload.newBuilder()
                .setHeartbeat(Heartbeat.newBuilder()
                        .setNodeId(RedstoneNode.getConfig().node().id())
                        .setToken(token)
                        .build())
                .build());
    }

    public void sendPong() {
        if (token == null || token.isBlank()) {
            return;
        }
        sendOrQueue(Payload.newBuilder()
                .setPong(Pong.newBuilder()
                        .setToken(token)
                        .build())
                .build());
    }

    private void sendNodeSync() {
        if (!streamActive || token == null || token.isBlank()) {
            return;
        }
        NodeSync.Builder sync = NodeSync.newBuilder()
                .setNodeId(RedstoneNode.getConfig().node().id())
                .setToken(token);

        for (Server server : NodeServerManager.getInstance().getServers().values()) {
            if (server == null) {
                continue;
            }
            NodeServerState state = NodeServerState.newBuilder()
                    .setName(server.getName())
                    .setTemplate(server.getTemplate().getName())
                    .setType(server.getType().name())
                    .setUuid(server.getUUID().toString())
                    .setStatus(server.getStatus().name())
                    .setPort(server.getPort())
                    .setAddress(server.getAddress().getHost())
                    .build();
            sync.addServers(state);
        }

        sendOrQueue(Payload.newBuilder().setNodeSync(sync).build());
    }

    public void requestNodeSync() {
        sendNodeSync();
    }

    private void sendOrQueue(RCClusteringProto.Payload payload) {
        if (payload == null) {
            return;
        }
        if (pending.size() >= 500) {
            pending.poll();
        }
        pending.add(payload);
        drainPending();
    }

    private void flushPending() {
        drainPending();
    }

    private void drainPending() {
        if (!streamActive || outbound == null) {
            return;
        }
        if (!draining.compareAndSet(false, true)) {
            return;
        }
        sendExecutor.execute(() -> {
            try {
                while (streamActive && outbound != null) {
                    RCClusteringProto.Payload payload = pending.poll();
                    if (payload == null) {
                        break;
                    }
                    outbound.onNext(payload);
                }
            } catch (Exception e) {
                streamActive = false;
                log.warn("Cluster stream send failed: {}", e.getMessage());
            } finally {
                draining.set(false);
                if (streamActive && outbound != null && !pending.isEmpty()) {
                    drainPending();
                }
            }
        });
    }
}
