package net.momirealms.craftengine.proxy.common.network.packet;

import net.momirealms.craftengine.proxy.common.network.protocol.ConnectionState;
import net.momirealms.craftengine.proxy.common.network.protocol.PacketSide;
import net.momirealms.craftengine.proxy.common.network.protocol.packettype.PacketTypeCommon;
import net.momirealms.craftengine.proxy.common.network.protocol.player.ClientVersion;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public record PacketRoute(
        PacketSide side,
        ConnectionState state,
        @Nullable PacketTypeCommon packetType,
        int packetId
) {

    public PacketRoute {
        Objects.requireNonNull(side, "side");
        Objects.requireNonNull(state, "state");
        if (packetType == null && packetId < 0) {
            throw new IllegalArgumentException("Raw packet routes require a non-negative packet id");
        }
    }

    public static PacketRoute typed(ConnectionState state, PacketTypeCommon packetType) {
        Objects.requireNonNull(packetType, "packetType");
        return new PacketRoute(packetType.getSide(), state, packetType, -1);
    }

    public static PacketRoute raw(PacketSide side, ConnectionState state, int packetId) {
        return new PacketRoute(side, state, null, packetId);
    }

    public boolean typed() {
        return this.packetType != null;
    }

    public int packetId(ClientVersion version) {
        if (this.packetType == null) {
            return this.packetId;
        }
        return this.packetType.getId(version);
    }
}
