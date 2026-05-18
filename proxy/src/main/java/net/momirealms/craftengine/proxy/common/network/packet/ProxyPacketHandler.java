package net.momirealms.craftengine.proxy.common.network.packet;

import net.momirealms.craftengine.proxy.common.network.ProtocolStateHolder;
import net.momirealms.craftengine.proxy.common.platform.ProxyPlayer;
import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface ProxyPacketHandler {

    void handle(ProtocolStateHolder connection, @Nullable ProxyPlayer player, ProxyPacketContext packet);
}
