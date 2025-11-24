package de.redstonecloud.api.redis.broker;

import de.redstonecloud.api.redis.broker.packet.defaults.communication.ClientAuthPacket;
import de.redstonecloud.api.redis.broker.packet.defaults.player.PlayerConnectPacket;
import de.redstonecloud.api.redis.broker.packet.defaults.player.PlayerDisconnectPacket;
import de.redstonecloud.api.redis.broker.packet.defaults.server.RemoveServerPacket;
import de.redstonecloud.api.redis.broker.packet.defaults.server.ServerActionPacket;
import de.redstonecloud.api.redis.broker.packet.defaults.server.ServerChangeStatusPacket;
import de.redstonecloud.api.redis.broker.packet.defaults.template.BestTemplateResultPacket;
import de.redstonecloud.api.redis.broker.packet.defaults.template.GetBestTemplatePacket;
import de.redstonecloud.api.redis.broker.packet.defaults.template.ServerStartedPacket;
import de.redstonecloud.api.redis.broker.packet.defaults.template.StartServerPacket;
import de.redstonecloud.api.redis.broker.packet.PacketRegistry;
import lombok.Getter;

public class BrokerHelper {
    public static class Holder {
        @Getter
        private static PacketRegistry registry;
    }

    public static PacketRegistry constructRegistry() {
        if (Holder.registry != null) {
            return Holder.registry;
        }

        PacketRegistry registry = new PacketRegistry();
        Holder.registry = registry;

        try {
            registry.register(ClientAuthPacket.NETWORK_ID, ClientAuthPacket::new);

            registry.register(GetBestTemplatePacket.NETWORK_ID, GetBestTemplatePacket::new);
            registry.register(BestTemplateResultPacket.NETWORK_ID, BestTemplateResultPacket::new);
            registry.register(StartServerPacket.NETWORK_ID, StartServerPacket::new);
            registry.register(ServerStartedPacket.NETWORK_ID, ServerStartedPacket::new);

            registry.register(PlayerConnectPacket.NETWORK_ID, PlayerConnectPacket::new);
            registry.register(PlayerDisconnectPacket.NETWORK_ID, PlayerDisconnectPacket::new);

            registry.register(RemoveServerPacket.NETWORK_ID, RemoveServerPacket::new);
            registry.register(ServerActionPacket.NETWORK_ID, ServerActionPacket::new);
            registry.register(ServerChangeStatusPacket.NETWORK_ID, ServerChangeStatusPacket::new);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return registry;
    }
}
