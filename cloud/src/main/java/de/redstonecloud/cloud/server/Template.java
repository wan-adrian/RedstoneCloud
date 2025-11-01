package de.redstonecloud.cloud.server;

import de.redstonecloud.api.components.ServerStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
public class Template {
    private String name;
    private ServerType type;
    private int maxPlayers;
    private int minServers;
    private int maxServers;
    private boolean staticServer;

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

    public void checkServers() {
        Server[] servers = getServers();
        runningServers = servers.length;

        handleIdleServers(servers);

        if (shouldCreateNewServer(servers)) {
            createNewServer();
        }
    }

    private Server[] getServers() {
        return ServerManager.getInstance().getServersByTemplate(this);
    }

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
        return runningServers <= maxServers;
    }

    private void createNewServer() {
        ServerManager.getInstance().startServer(this);
    }
}