package net.momirealms.craftengine.proxy.common.network.packet;

import net.momirealms.craftengine.proxy.common.network.protocol.ConnectionState;
import net.momirealms.craftengine.proxy.common.network.protocol.PacketSide;
import net.momirealms.craftengine.proxy.common.network.protocol.player.ClientVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class PacketHandlerRegistry {
    private final PacketHandlerChain[][][][] handlers =
            new PacketHandlerChain[PacketSide.values().length][ConnectionState.values().length][ClientVersion.values().length][];

    private PacketHandlerRegistry() {}

    public static PacketHandlerRegistry create() {
        return new PacketHandlerRegistry();
    }

    public PacketRegistration register(@NotNull PacketRoute route, @NotNull PacketHandler handler) {
        List<PacketHandlerChain> registeredChains = new ArrayList<>();
        if (route.typed()) {
            for (ClientVersion version : ClientVersion.values()) {
                if (!version.isRelease()) {
                    continue;
                }
                int packetId = route.packetId(version);
                if (packetId >= 0) {
                    this.add(route.side(), route.state(), version, packetId, handler, registeredChains);
                }
            }
        } else {
            for (ClientVersion version : ClientVersion.values()) {
                this.add(route.side(), route.state(), version, route.packetId(), handler, registeredChains);
            }
        }

        List<PacketHandlerChain> chains = List.copyOf(registeredChains);
        return new PacketRegistration(route, handler, () -> {
            for (PacketHandlerChain chain : chains) {
                chain.remove(handler);
            }
        });
    }

    public @Nullable PacketHandlerChain find(PacketSide side, ConnectionState state, ClientVersion version, int packetId) {
        if (side == null || state == null || packetId < 0) {
            return null;
        }

        ClientVersion mappedVersion = version == null || !version.isRelease() ? ClientVersion.getLatest() : version;
        PacketHandlerChain[] chains = this.handlers[side.ordinal()][state.ordinal()][mappedVersion.ordinal()];
        if (chains == null || packetId >= chains.length) {
            return null;
        }

        PacketHandlerChain chain = chains[packetId];
        return chain == null || chain.isEmpty() ? null : chain;
    }

    private void add(PacketSide side, ConnectionState state, ClientVersion version, int packetId, PacketHandler handler, List<PacketHandlerChain> registeredChains) {
        PacketHandlerChain chain = this.chain(side, state, version, packetId);
        chain.add(handler);
        registeredChains.add(chain);
    }

    private synchronized PacketHandlerChain chain(PacketSide side, ConnectionState state, ClientVersion version, int packetId) {
        PacketHandlerChain[][][] sideHandlers = this.handlers[side.ordinal()];
        PacketHandlerChain[][] stateHandlers = sideHandlers[state.ordinal()];
        PacketHandlerChain[] versionHandlers = stateHandlers[version.ordinal()];
        if (versionHandlers == null) {
            versionHandlers = new PacketHandlerChain[Math.max(packetId + 1, 4)];
            stateHandlers[version.ordinal()] = versionHandlers;
        } else if (packetId >= versionHandlers.length) {
            versionHandlers = Arrays.copyOf(versionHandlers, packetId + 1);
            stateHandlers[version.ordinal()] = versionHandlers;
        }

        PacketHandlerChain chain = versionHandlers[packetId];
        if (chain == null) {
            chain = new PacketHandlerChain();
            versionHandlers[packetId] = chain;
        }
        return chain;
    }
}
