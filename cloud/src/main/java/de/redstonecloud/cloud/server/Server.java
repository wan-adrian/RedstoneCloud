package de.redstonecloud.cloud.server;

import com.google.common.net.HostAndPort;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import de.redstonecloud.api.components.ICloudServer;
import de.redstonecloud.api.components.ServerStatus;
import de.redstonecloud.api.components.cache.ServerData;
import de.redstonecloud.api.redis.broker.packet.defaults.server.RemoveServerPacket;
import de.redstonecloud.api.redis.cache.Cacheable;
import de.redstonecloud.api.util.Keys;
import de.redstonecloud.cloud.RedstoneCloud;
import de.redstonecloud.cloud.config.CloudConfig;
import de.redstonecloud.cloud.config.entry.RedisEntry;
import de.redstonecloud.cloud.events.defaults.ServerExitEvent;
import de.redstonecloud.cloud.scheduler.task.Task;
import de.redstonecloud.cloud.server.reader.ServerOutReader;
import de.redstonecloud.cloud.utils.Directories;
import de.redstonecloud.cloud.utils.Translator;
import lombok.*;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Represents a cloud server instance with lifecycle management capabilities.
 * Handles server preparation, starting, stopping, and cleanup operations.
 */
@Builder
@Getter
@Log4j2
public class Server implements ICloudServer, Cacheable {
    private static final Gson GSON = new Gson();

    private final Template template;
    private String name;
    private final int port;
    private final UUID uuid;
    private final ServerType type;
    private final long createdAt;
    private String directory;

    @Builder.Default
    private final List<UUID> players = new CopyOnWriteArrayList<>();

    @Builder.Default
    @Getter(AccessLevel.NONE)
    private final AtomicReference<ServerStatus> statusRef = new AtomicReference<>(ServerStatus.NONE);

    @Builder.Default
    @Setter
    private volatile long lastPlayerUpdate = System.currentTimeMillis();

    @Setter
    private volatile ServerOutReader logger;

    @Getter(AccessLevel.PUBLIC)
    @Setter(AccessLevel.PRIVATE)
    private volatile Process process;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.PRIVATE)
    private volatile ProcessBuilder processBuilder;

    @Override
    public String toString() {
        JsonObject obj = new ServerData(
                name,
                uuid,
                template.getName(),
                getStatus().name(),
                type.name(),
                port,
                type.isProxy(),
                GSON.fromJson(GSON.toJson(players), JsonArray.class),
                new JsonObject()
        ).toJson();

        return obj.toString();
    }

    @Override
    public String cacheKey() {
        return Keys.CACHE_PREFIX_SERVER + name.toUpperCase();
    }

    @Override
    public void setStatus(ServerStatus newStatus) {
        ServerStatus oldStatus = statusRef.getAndSet(newStatus);
        if (oldStatus != newStatus) {
            updateCache();
        }
    }

    public ServerStatus getStatus() {
        return statusRef.get();
    }

    /**
     * Writes a command to the server console.
     * Only works if the server is in an active state.
     *
     * @param command The command to execute
     */
    public void writeConsole(String command) {
        ServerStatus currentStatus = getStatus();
        if (currentStatus != ServerStatus.STARTING &&
                currentStatus != ServerStatus.RUNNING &&
                currentStatus != ServerStatus.STOPPING) {
            log.warn("Attempted to write console command to server {} in invalid state: {}", name, currentStatus);
            return;
        }

        if (process == null || !process.isAlive()) {
            log.warn("Cannot write to console for server {}: process not alive", name);
            return;
        }

        try (PrintWriter stdin = new PrintWriter(
                new BufferedWriter(
                        new OutputStreamWriter(process.getOutputStream())), true)) {
            stdin.println(command);
        } catch (Exception e) {
            log.error("Failed to write console command to server {}", name, e);
        }
    }

    /**
     * Initializes the server name, optionally forcing a specific ID.
     *
     * @param forceId Optional forced server ID, null for auto-generation
     */
    public void initName(Integer forceId) {
        String candidateName;

        if (forceId != null) {
            candidateName = template.getName() + "-" + forceId;
            if (ServerManager.getInstance().getServer(candidateName) == null) {
                return; // Name is available
            }
            log.warn("Server with name {} already exists, generating new name", candidateName);
        }

        int serverId = findAvailableServerId();

        name = template.getName() + template.getSeperator() + serverId;
        // This would need to be set via builder or a mutable field approach
        log.info("Generated server name: {}-{}", template.getName(), serverId);
    }

    private int findAvailableServerId() {
        int serverId = 1;
        String baseName = template.getName();
        while (ServerManager.getInstance().getServer(baseName + "-" + serverId) != null) {
            serverId++;
        }
        return serverId;
    }

    /**
     * Prepares the server by copying template files and configuring ports.
     */
    public void prepare() {
        if (getStatus().ordinal() >= ServerStatus.PREPARED.ordinal()) {
            log.debug("Server {} already prepared, skipping", name);
            return;
        }

        log.info(Translator.translate("cloud.server.prepare", name));

        try {
            Path serverDir = createServerDirectory();
            copyTemplateFiles(serverDir);
            configureServerPort(serverDir);
            setStatus(ServerStatus.PREPARED);
            log.info("Server {} prepared successfully", name);
        } catch (Exception e) {
            log.error("Failed to prepare server {}", name, e);
            throw new RuntimeException("Server preparation failed", e);
        }
    }

    private Path createServerDirectory() throws IOException {
        String basePath = (template.isStaticServer() ? Directories.SERVERS_DIR : Directories.TEMP_DIR).getAbsolutePath();
        Path serverPath = Path.of(basePath, name);
        Files.createDirectories(serverPath);
        directory = serverPath.toString();
        return serverPath;
    }

    private void copyTemplateFiles(Path destination) throws IOException {
        Path templatePath = Path.of(Directories.TEMPLATES_DIR.getAbsolutePath(), template.getName());

        if (!Files.exists(templatePath)) {
            throw new IOException("Template directory not found: " + templatePath);
        }

        FileUtils.copyDirectory(templatePath.toFile(), destination.toFile());
    }

    private void configureServerPort(Path serverDir) throws IOException {
        Path portConfigFile = serverDir.resolve(type.portSettingFile());

        if (!Files.exists(portConfigFile)) {
            log.warn("Port configuration file not found: {}", portConfigFile);
            return;
        }

        String content = Files.readString(portConfigFile, StandardCharsets.UTF_8);
        content = content.replace(type.portSettingPlaceholder(), String.valueOf(port));
        Files.writeString(portConfigFile, content, StandardCharsets.UTF_8);
    }

    /**
     * Handles server exit cleanup and notification.
     */
    public void onExit() {
        log.info(Translator.translate("cloud.server.exited", name));

        cleanupLogger();
        cleanupProcess();
        proxyNotify();
        saveLogs();
        cleanupServerDirectory();
        finalizeShutdown();
    }

    private void cleanupLogger() {
        if (logger != null) {
            logger.cancel();
        }
    }

    private void cleanupProcess() {
        if (process != null) {
            process.destroy();
        }
        setStatus(ServerStatus.STOPPED);
    }

    private void proxyNotify() {
        ServerType[] proxyTypes = Arrays.stream(ServerManager.getInstance().getTypes().values().toArray(new ServerType[0]))
                .filter(ServerType::isProxy)
                .toArray(ServerType[]::new);

        RemoveServerPacket packet = new RemoveServerPacket().setServer(this.name);

        for (ServerType proxyType : proxyTypes) {
            for (Server server : ServerManager.getInstance().getServersByType(proxyType)) {
                packet.setTo(server.getName().toLowerCase()).send();
            }
        }
    }

    private void saveLogs() {
        if (template.isStaticServer() || type.logsPath() == null) {
            return;
        }

        Path logSource = Path.of(directory, type.logsPath());
        if (!Files.exists(logSource)) {
            log.debug("No log file found for server {}", name);
            return;
        }

        try {
            String logFileName = String.format("%s_%d.log", name, System.currentTimeMillis());
            Path logDestination = Path.of(Directories.LOGS_DIR.getAbsolutePath(), logFileName);
            Files.createDirectories(logDestination.getParent());
            Files.copy(logSource, logDestination, StandardCopyOption.REPLACE_EXISTING);
            log.info("Saved logs for server {} to {}", name, logDestination);
        } catch (IOException e) {
            log.error("Failed to copy log file for server {}", name, e);
        }
    }

    private void cleanupServerDirectory() {
        if (template.isStaticServer()) {
            return;
        }

        try {
            FileUtils.deleteDirectory(new File(directory));
            log.debug("Deleted temporary directory for server {}", name);
        } catch (IOException e) {
            log.error("Failed to delete server directory for {}", name, e);
        }
    }

    private void finalizeShutdown() {
        resetCache();
        ServerManager.getInstance().remove(this);
        RedstoneCloud.getInstance().getEventManager().callEvent(new ServerExitEvent(this));
    }

    @Override
    public void start() {
        ServerStatus currentStatus = getStatus();
        if (currentStatus != ServerStatus.PREPARED && currentStatus.ordinal() >= ServerStatus.STARTING.ordinal()) {
            log.warn("Cannot start server {} in state {}", name, currentStatus);
            return;
        }

        if(name == null) {
            throw new IllegalStateException("Server name not initialized. Call initName() before starting the server.");
        }

        log.info(Translator.translate("cloud.server.starting", name));
        setStatus(ServerStatus.STARTING);

        try {
            configureProcessBuilder();
            startProcess();
            scheduleStartupTimeout();
        } catch (Exception e) {
            log.error("Failed to start server {}", name, e);
            setStatus(ServerStatus.STOPPED);
        }
    }

    private void configureProcessBuilder() {
        setProcessBuilder(new ProcessBuilder(type.startCommand())
                .directory(new File(directory)));

        RedisEntry redisCfg = CloudConfig.getRedis();
        Map<String, String> env = processBuilder.environment();
        env.put(Keys.ENV_REDIS_IP, redisCfg.ip());
        env.put(Keys.ENV_REDIS_PORT, redisCfg.port());
        env.put(Keys.ENV_REDIS_DB, String.valueOf(redisCfg.db()));
        env.put("BRIDGE_CFG", CloudConfig.getCfg().get("bridge").getAsJsonObject().toString());
    }

    private void startProcess() throws IOException {
        this.logger = ServerOutReader.builder().server(this).build();
        setProcess(processBuilder.start());
        this.logger.start();
        this.process.onExit().thenRun(this::onExit);
    }

    private void scheduleStartupTimeout() {
        RedstoneCloud.getInstance().getScheduler().scheduleDelayedTask(new Task() {
            @Override
            protected void onRun(long currentMillis) {
                if (getStatus().ordinal() <= ServerStatus.STARTING.ordinal()) {
                    log.error("Server {} failed to start within timeout period, killing server", name);
                    kill(true);
                }
            }
        }, template.getMaxBootTimeMs());
    }

    /**
     * Kills the server process gracefully.
     */
    public void kill() {
        kill(false);
    }

    /**
     * Kills the server process.
     *
     * @param silent If true, suppresses logging of forced termination
     */
    public void kill(boolean silent) {
        this.stop();

        RedstoneCloud.getInstance().getScheduler().scheduleDelayedTask(new Task() {
            @Override
            protected void onRun(long currentMillis) {
                if (process != null && process.isAlive()) {
                    process.destroyForcibly();
                    if (!silent) {
                        log.warn("Server {} did not stop gracefully, forcibly terminated", name);
                    }
                }
            }
        }, template.getShutdownTimeMs());
    }

    @Override
    public void stop() {
        log.info(Translator.translate("cloud.server.stopping", name));

        cleanupLogger();

        if (getStatus() != ServerStatus.RUNNING) {
            log.warn("Attempted to stop server {} in invalid state: {}", name, getStatus());
            return;
        }

        writeConsole(type.stopCommand());
        setStatus(ServerStatus.STOPPING);
    }

    @Override
    public UUID getUUID() {
        return uuid;
    }

    @Override
    public HostAndPort getAddress() {
        return HostAndPort.fromParts("0.0.0.0", port);
    }
}