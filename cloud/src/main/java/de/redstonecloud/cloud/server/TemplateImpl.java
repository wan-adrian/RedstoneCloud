package de.redstonecloud.cloud.server;

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
        try {
            ServerManager.getInstance().startServer(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
