package de.redstonecloud.api.components;

import com.google.common.net.HostAndPort;

import java.util.UUID;

public interface ICloudPlayer extends Nameable {

    HostAndPort getAddress();

    ICloudServer getConnectedNetwork();

    ICloudServer getConnectedServer();

    UUID getUUID();

    void sendMessage(String message);

    void connect(String server);

    default void disconnect() {
        this.disconnect(null);
    }

    void disconnect(String reason);
}
