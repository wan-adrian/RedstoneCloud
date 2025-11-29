package de.redstonecloud.node.server;

import de.redstonecloud.shared.server.Server;
import de.redstonecloud.shared.server.Template;
import lombok.experimental.SuperBuilder;

@SuperBuilder
public class TemplateImpl extends Template {
    @Override
    protected Server[] getServers() {
        return null;
    }

    @Override
    protected void createNewServer() {

    }
}
