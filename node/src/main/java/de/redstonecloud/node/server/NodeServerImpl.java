package de.redstonecloud.node.server;

import de.redstonecloud.api.components.ServerStatus;
import de.redstonecloud.node.cluster.grpc.RCMaster;
import de.redstonecloud.shared.server.Server;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@SuperBuilder
public class NodeServerImpl extends Server {
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
        RCMaster.serverDied(name);
        NodeServerManager.getInstance().remove(this);
    }

    @Override
    protected void startRemote() {}

    @Override
    protected void killRemote() {}

    @Override
    protected void stopRemote() {}

    @Override
    protected void sendStatusRemote(ServerStatus newStatus) {
        RCMaster.statusChange(name, newStatus.name());
    }
}
