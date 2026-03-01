package de.redstonecloud.shared.server;

import de.redstonecloud.api.components.ServerStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.List;

@SuperBuilder
@Getter
public abstract class Template {
    private String name;
    private ServerType type;
    private int maxPlayers;
    private int minServers;
    private int maxServers;
    private boolean staticServer;
    private String raw;

    @Builder.Default
    @Setter
    public int runningServers = 0;

    @Setter
    @Builder.Default
    public boolean stopOnEmpty = false;

    @Setter
    @Builder.Default
    public int shutdownTimeMs = 5000;

    private static final long IDLE_TIMEOUT_MS = 5 * 60 * 1000; // 5 minutes

    @Builder.Default
    private String seperator = "-";

    @Builder.Default
    private long maxBootTimeMs = 60 * 1000; // 1 minute

    @Builder.Default
    private List<String> nodes = List.of();

    public void checkServers() {
        Server[] servers = getServers();
        runningServers = servers.length;

        handleIdleServers(servers);

        if (shouldCreateNewServer(servers)) {
            createNewServer();
        }
    }

    protected abstract Server[] getServers();

    private void handleIdleServers(Server[] servers) {
        if (!stopOnEmpty) return;

        for (Server server : servers) {
            if (isServerIdle(server)) {
                server.kill();
            }
        }
    }

    private boolean isServerIdle(Server server) {
        boolean hasNoPlayers = server.getPlayers().isEmpty();
        boolean exceedsIdleTime = (System.currentTimeMillis() - server.getLastPlayerUpdate()) > IDLE_TIMEOUT_MS;
        return hasNoPlayers && exceedsIdleTime;
    }

    private boolean shouldCreateNewServer(Server[] servers) {
        return (needsMoreServers() || allServersBlocked(servers)) && canCreateMoreServers();
    }

    private boolean needsMoreServers() {
        return minServers > 0 && runningServers < minServers;
    }

    private boolean allServersBlocked(Server[] servers) {
        if (minServers <= 0 || servers.length == 0) return false;

        return countBlockedServers(servers) == servers.length;
    }

    private int countBlockedServers(Server[] servers) {
        int blocked = 0;
        for (Server server : servers) {
            if (isServerBlocked(server)) {
                blocked++;
            }
        }
        return blocked;
    }

    private boolean isServerBlocked(Server server) {
        ServerStatus status = server.getStatus();
        return status != ServerStatus.RUNNING &&
                status != ServerStatus.STARTING &&
                status != ServerStatus.PREPARED;
    }

    private boolean canCreateMoreServers() {
        if (maxServers <= 0) {
            return true;
        }
        return runningServers < maxServers;
    }

    protected abstract void createNewServer();

    public Template merge(Template other) {
        this.name = other.name;
        this.type = other.type;
        this.maxPlayers = other.maxPlayers;
        this.minServers = other.minServers;
        this.maxServers = other.maxServers;
        this.staticServer = other.staticServer;
        this.raw = other.raw;
        this.seperator = other.seperator;
        this.nodes = other.nodes;
        this.stopOnEmpty = other.stopOnEmpty;
        this.shutdownTimeMs = other.shutdownTimeMs;
        this.maxBootTimeMs = other.maxBootTimeMs;
        return this;
    }
}
