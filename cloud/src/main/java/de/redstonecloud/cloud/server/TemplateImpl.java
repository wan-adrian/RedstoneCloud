package de.redstonecloud.cloud.server;

import de.redstonecloud.cloud.RedstoneCloud;
import de.redstonecloud.cloud.cluster.ClusterManager;
import de.redstonecloud.cloud.cluster.ClusterNode;
import de.redstonecloud.shared.server.Server;
import de.redstonecloud.shared.server.Template;
import lombok.experimental.SuperBuilder;

@SuperBuilder
public class TemplateImpl extends Template {
    @Override
    protected Server[] getServers() {
        return ServerManager.getInstance().getServersByTemplate(this);
    }

    @Override
    protected void createNewServer() {
        if(!RedstoneCloud.isRunning()) return;

        if(ClusterManager.isCluster()) {
            if(!getNodes().isEmpty()) {
                ClusterNode node = ClusterManager.getInstance().getNodeById(getNodes().getFirst());
                if(node == null || node.getStream() == null || node.isShuttingDown()) return;
            }
        }

        try {
            ServerManager.getInstance().startServer(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
