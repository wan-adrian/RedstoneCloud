package de.redstonecloud.node;

import de.redstonecloud.api.util.Keys;
import de.redstonecloud.node.cluster.ClusterClient;
import de.redstonecloud.node.config.NodeConfig;
import de.redstonecloud.node.config.entry.RedisEntry;
import de.redstonecloud.node.server.ServerManager;
import de.redstonecloud.shared.utils.Directories;
import lombok.Getter;
import org.apache.commons.io.FileUtils;

@Getter
public class RedstoneNode {
    private ServerManager serverManager;
    private ClusterClient clusterClient;

    @Getter protected static String workingDir;

    protected RedstoneNode() {
        this.serverManager = ServerManager.getInstance();

        clusterClient = ClusterClient.getInstance();
        clusterClient.start();
    }

    public void shutdown() {
        this.serverManager.stopAll();
        this.clusterClient.shutdown();
        //remove tmp files
        try {
            FileUtils.deleteDirectory(Directories.TMP_STORAGE_DIR);
        } catch (Exception ignored) {
        }
    }
}