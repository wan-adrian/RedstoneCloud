package de.redstonecloud.api.redis.broker.packet.defaults.cluster;

import com.google.gson.JsonArray;
import de.redstonecloud.api.redis.broker.packet.Packet;
import de.redstonecloud.api.util.NodeNotifyType;
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
public class NodeNotifyPacket extends Packet {
    public static int NETWORK_ID = 53;

    protected UUID nodeId;
    protected NodeNotifyType type;
    protected JsonArray data = new JsonArray();

    @Override
    public int packetId() {
        return NETWORK_ID;
    }

    @Override
    public void serialize(JsonArray data) {
        data.add(this.nodeId.toString());
        data.add(this.type.name());
        data.add(this.data);
    }

    @Override
    public void deserialize(JsonArray data) {
        this.nodeId = UUID.fromString(data.get(0).getAsString());
        this.type = NodeNotifyType.valueOf(data.get(1).getAsString());
        this.data = data.get(2).getAsJsonArray();
    }
}
