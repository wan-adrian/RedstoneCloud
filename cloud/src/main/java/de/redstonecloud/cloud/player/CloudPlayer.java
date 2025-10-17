package de.redstonecloud.cloud.player;

import com.google.common.net.HostAndPort;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.redstonecloud.api.components.ICloudPlayer;
import de.redstonecloud.api.components.ICloudServer;
import de.redstonecloud.api.components.ServerActions;
import de.redstonecloud.api.components.cache.PlayerData;
import de.redstonecloud.api.redis.broker.packet.Packet;
import de.redstonecloud.api.redis.broker.packet.defaults.server.ServerActionPacket;
import de.redstonecloud.api.redis.cache.Cacheable;
import de.redstonecloud.api.util.Keys;
import de.redstonecloud.cloud.server.Server;
import lombok.Builder;
import lombok.Setter;
import org.json.JSONObject;

import java.util.UUID;

@Setter
@Builder
public class CloudPlayer implements ICloudPlayer, Cacheable {
    protected String name;
    protected HostAndPort address;
    private Server network;
    private Server server;
    protected UUID uuid;
    @Builder.Default
    public JsonObject extraData = new JsonObject();

    @Override
    public String toString() {
        JsonObject obj = new PlayerData(
                name,
                uuid,
                address.toString(),
                network != null ? network.getName() : null,
                server != null ? server.getName() : null,
                extraData
        ).toJson();

        return obj.toString();
    }


    @Override
    public HostAndPort getAddress() {
        return address;
    }

    @Override
    public ICloudServer getConnectedNetwork() {
        return network;
    }

    @Override
    public ICloudServer getConnectedServer() {
        return server;
    }

    public void setConnectedServer(Server srv) {
        long updateMS = System.currentTimeMillis();

        if(server != null) {
            server.getPlayers().remove(uuid);
            server.updateCache();
            server.setLastPlayerUpdate(updateMS);
        }
        server = srv;
        updateCache();
        if(srv != null && !srv.getPlayers().contains(uuid)) {
            srv.getPlayers().add(uuid);
            srv.updateCache();
            server.setLastPlayerUpdate(updateMS);
        }
    }

    public void setConnectedNetwork(Server srv) {
        long updateMS = System.currentTimeMillis();
        if(network != null) {
            network.getPlayers().remove(uuid);
            network.updateCache();
            network.setLastPlayerUpdate(updateMS);
        }
        network = srv;
        updateCache();
        if(srv != null && !srv.getPlayers().contains(uuid)) {
            srv.getPlayers().add(uuid);
            srv.updateCache();
            network.setLastPlayerUpdate(updateMS);
        }
    }

    @Override
    public UUID getUUID() {
        return uuid;
    }

    @Override
    public void sendMessage(String message) {
        JsonObject extraData = new JsonObject();
        extraData.addProperty("message", message);

        new ServerActionPacket()
                .setAction(ServerActions.PLAYER_SEND_MESSAGE.name())
                .setPlayerUuid(uuid.toString())
                .setExtraData(extraData)
                .setTo(network.getName())
                .send();
    }

    @Override
    public void connect(String server) {
        JsonObject extraData = new JsonObject();
        extraData.addProperty("server", server);

        new ServerActionPacket()
                .setAction(ServerActions.PLAYER_CONNECT.name())
                .setPlayerUuid(uuid.toString())
                .setExtraData(extraData)
                .setTo(network.getName())
                .send();
    }

    @Override
    public void disconnect(String reason) {
        JsonObject extraData = new JsonObject();
        extraData.addProperty("reason", reason);

        new ServerActionPacket()
                .setAction(ServerActions.PLAYER_KICK.name())
                .setPlayerUuid(uuid.toString())
                .setExtraData(extraData)
                .setTo(network.getName()).send();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String cacheKey() {
        return Keys.CACHE_PREFIX_PLAYER + uuid;
    }
}
