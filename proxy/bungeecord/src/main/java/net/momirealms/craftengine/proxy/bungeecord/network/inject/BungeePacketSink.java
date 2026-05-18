package net.momirealms.craftengine.proxy.bungeecord.network.inject;

import io.netty.buffer.ByteBuf;
import net.momirealms.craftengine.proxy.bungeecord.network.BungeeChannelConnection;
import net.momirealms.craftengine.proxy.common.network.protocol.PacketSide;

// 处理尚未转换为通用 ProxyPacketContext 的原始数据包
@FunctionalInterface
public interface BungeePacketSink {

    ByteBuf handle(BungeeChannelConnection connection, PacketSide side, ByteBuf buffer);
}
