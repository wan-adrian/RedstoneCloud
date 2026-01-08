package de.redstonecloud.cloud.events.defaults;

import de.redstonecloud.cloud.events.CancellableEvent;
import de.redstonecloud.cloud.events.Event;
import de.redstonecloud.shared.server.Server;
import lombok.Getter;

@Getter
public class ServerCreateEvent extends Event implements CancellableEvent {
    private final Server server;

    public ServerCreateEvent(Server srv) {
        this.server = srv;
    }
}