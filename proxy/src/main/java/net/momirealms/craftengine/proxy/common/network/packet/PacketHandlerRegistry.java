package net.momirealms.craftengine.proxy.common.network.packet;

import net.momirealms.craftengine.proxy.common.network.protocol.ConnectionState;
import net.momirealms.craftengine.proxy.common.network.protocol.PacketSide;
import net.momirealms.craftengine.proxy.common.network.protocol.player.ClientVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Objects;

public final class PacketHandlerRegistry {
    private final PacketHandler[][][][] handlers =
            new PacketHandler[PacketSide.values().length][ConnectionState.values().length][ClientVersion.values().length][];

    private PacketHandlerRegistry() {}

    public static PacketHandlerRegistry create() {
        return new PacketHandlerRegistry();
    }

    public PacketRegistration register(@NotNull PacketRoute route, @NotNull PacketHandler handler) {
        Objects.requireNonNull(route, "route");
        Objects.requireNonNull(handler, "handler");

        synchronized (this) {
            this.ensureAvailable(route);
            this.setPacketHandlers(route, handler);
        }

        return new PacketRegistration(route, handler, () -> this.unregister(route, handler));
    }

    private void ensureAvailable(PacketRoute route) {
        for (ClientVersion version : ClientVersion.values()) {
            if (!version.isRelease()) {
                continue;
            }
            int packetId = route.packetId(version);
            if (packetId >= 0) {
                PacketHandler existing = this.getPacketHandler(route.side(), route.state(), version, packetId);
                if (existing != null) {
                    throw new IllegalStateException("Packet handler already registered for " + route.side() + "/" + route.state() + "/" + version + "/" + packetId);
                }
            }
        }
    }

    private void setPacketHandlers(PacketRoute route, PacketHandler handler) {
        for (ClientVersion version : ClientVersion.values()) {
            if (!version.isRelease()) {
                continue;
            }
            int packetId = route.packetId(version);
            if (packetId >= 0) {
                this.setPacketHandler(route.side(), route.state(), version, packetId, handler);
            }
        }
    }

    private synchronized void unregister(PacketRoute route, PacketHandler handler) {
        for (ClientVersion version : ClientVersion.values()) {
            if (!version.isRelease()) {
                continue;
            }
            int packetId = route.packetId(version);
            if (packetId >= 0 && this.getPacketHandler(route.side(), route.state(), version, packetId) == handler) {
                this.clearPacketHandler(route.side(), route.state(), version, packetId);
            }
        }
    }

    public @Nullable PacketHandler getPacketHandler(PacketSide side, ConnectionState state, ClientVersion version, int packetId) {
        if (side == null || state == null || packetId < 0) {
            return null;
        }
        ClientVersion mappedVersion = version == null || !version.isRelease() ? ClientVersion.getLatest() : version;
        PacketHandler[] packetHandlers = this.handlers[side.ordinal()][state.ordinal()][mappedVersion.ordinal()];
        if (packetHandlers == null || packetId >= packetHandlers.length) {
            return null;
        }
        return packetHandlers[packetId];
    }

    private synchronized void setPacketHandler(PacketSide side, ConnectionState state, ClientVersion version, int packetId, PacketHandler handler) {
        PacketHandler[][][] sideHandlers = this.handlers[side.ordinal()];
        PacketHandler[][] stateHandlers = sideHandlers[state.ordinal()];
        PacketHandler[] versionHandlers = stateHandlers[version.ordinal()];

        if (versionHandlers == null) {
            versionHandlers = new PacketHandler[Math.max(packetId + 1, 4)];
            stateHandlers[version.ordinal()] = versionHandlers;
        } else if (packetId >= versionHandlers.length) {
            versionHandlers = Arrays.copyOf(versionHandlers, packetId + 1);
            stateHandlers[version.ordinal()] = versionHandlers;
        }

        versionHandlers[packetId] = handler;
    }

    private synchronized void clearPacketHandler(PacketSide side, ConnectionState state, ClientVersion version, int packetId) {
        if (side == null || state == null || packetId < 0) {
            return;
        }
        ClientVersion mappedVersion = version == null || !version.isRelease() ? ClientVersion.getLatest() : version;
        PacketHandler[] versionHandlers = this.handlers[side.ordinal()][state.ordinal()][mappedVersion.ordinal()];
        if (versionHandlers == null || packetId >= versionHandlers.length) {
            return;
        }
        versionHandlers[packetId] = null;
    }
}
