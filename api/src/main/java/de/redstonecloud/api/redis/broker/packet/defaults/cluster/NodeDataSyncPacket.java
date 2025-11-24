package de.redstonecloud.api.redis.broker.packet.defaults.cluster;

import com.google.gson.JsonArray;
import de.redstonecloud.api.redis.broker.packet.Packet;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
public class NodeDataSyncPacket extends Packet {
    public static int NETWORK_ID = 52;

    protected UUID nodeId;
    protected List<String> types = new ArrayList<>();
    protected List<String> templates = new ArrayList();

    @Override
    public int packetId() {
        return NETWORK_ID;
    }

    @Override
    public void serialize(JsonArray data) {
        data.add(this.nodeId.toString());
        data.add(this.types.size());
        for (String type : this.types) {
            data.add(type);
        }

        data.add(this.templates.size());
        for (String template : this.templates) {
            data.add(template);
        }
    }

    @Override
    public void deserialize(JsonArray data) {
        this.nodeId = UUID.fromString(data.get(0).getAsString());
        int typesSize = data.get(1).getAsInt();
        for (int i = 0; i < typesSize; i++) {
            this.types.add(data.get(2 + i).getAsString());
        }

        int templatesStartIndex = 2 + typesSize;
        int templatesSize = data.get(templatesStartIndex).getAsInt();
        for (int i = 0; i < templatesSize; i++) {
            this.templates.add(data.get(templatesStartIndex + 1 + i).getAsString());
        }
    }
}
