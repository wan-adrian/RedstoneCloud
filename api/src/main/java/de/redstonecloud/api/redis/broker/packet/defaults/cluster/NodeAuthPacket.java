package de.redstonecloud.api.redis.broker.packet.defaults.cluster;

import com.google.gson.JsonArray;
import de.redstonecloud.api.redis.broker.packet.Packet;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.UUID;

@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
public class NodeAuthPacket extends Packet {
    public static int NETWORK_ID = 50;

    protected String nodeName;
    protected UUID nodeId;

    @Override
    public int packetId() {
        return NETWORK_ID;
    }

    @Override
    public void serialize(JsonArray data) {
        data.add(this.nodeName);
        data.add(this.nodeId.toString());
    }

    @Override
    public void deserialize(JsonArray data) {
        this.nodeName = data.get(0).getAsString();
        this.nodeId = UUID.fromString(data.get(1).getAsString());
    }
}
