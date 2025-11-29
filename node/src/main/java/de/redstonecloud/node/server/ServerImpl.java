package de.redstonecloud.node.server;

import de.redstonecloud.api.components.ServerStatus;
import de.redstonecloud.api.redis.broker.packet.defaults.server.RemoveServerPacket;
import de.redstonecloud.node.cluster.grpc.RCMaster;
import de.redstonecloud.shared.server.Server;
import de.redstonecloud.shared.server.ServerType;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;

@Slf4j
@SuperBuilder
public class ServerImpl extends Server {
    public void setName(String name) {
        this.name = name;
    }

    @Override
    protected void writeConsoleRemove(String command) {}

    @Override
    public void initName(Integer forceId) {}

    @Override
    protected void prepareRemote() {}

    @Override
    protected void proxyNotify() {}

    @Override
    protected void finalizeShutdown() {
        ServerManager.getInstance().remove(this);
    }

    @Override
    protected void startRemote() {}

    @Override
    protected void killRemote() {}

    @Override
    protected void stopRemote() {}

    @Override
    protected void sendStatusRemote(ServerStatus newStatus) {
        //TODO: add
        //RCMaster.statusChange(name, newStatus.name());
    }
}
