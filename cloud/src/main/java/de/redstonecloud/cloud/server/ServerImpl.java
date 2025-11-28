package de.redstonecloud.cloud.server;

import de.redstonecloud.api.redis.broker.packet.defaults.server.RemoveServerPacket;
import de.redstonecloud.cloud.RedstoneCloud;
import de.redstonecloud.cloud.cluster.grpc.RCNode;
import de.redstonecloud.cloud.events.defaults.ServerExitEvent;
import de.redstonecloud.shared.server.Server;
import de.redstonecloud.shared.server.ServerType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;

@Slf4j
@SuperBuilder
public class ServerImpl extends Server {
    @Override
    protected void writeConsoleRemove(String command) {
        new RCNode(nodeId).executeCommand(name, command);
    }

    @Override
    public void initName(Integer forceId) {
        String candidateName;

        if (forceId != null) {
            candidateName = getTemplate().getName() + "-" + forceId;
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

    @Override
    protected void prepareRemote() {
        new RCNode(nodeId).prepareServer(template.getName(), name);
    }

    @Override
    protected void proxyNotify() {
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

    @Override
    protected void finalizeShutdown() {
        ServerManager.getInstance().remove(this);
        RedstoneCloud.getInstance().getEventManager().callEvent(new ServerExitEvent(this));
    }

    @Override
    protected void startRemote() {
        new RCNode(nodeId).startServer(name);
    }

    @Override
    protected void killRemote() {
        new RCNode(nodeId).stopServer(name, true);
    }

    @Override
    protected void stopRemote() {
        new RCNode(nodeId).stopServer(name, false);
    }
}
