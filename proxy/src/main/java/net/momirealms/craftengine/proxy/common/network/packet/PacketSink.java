package net.momirealms.craftengine.proxy.common.network.packet;

import io.netty.buffer.ByteBuf;
import net.momirealms.craftengine.proxy.common.network.ChannelConnection;
import net.momirealms.craftengine.proxy.common.network.protocol.PacketSide;

// 处理尚未转换为通用 ProxyPacketContext 的原始数据包
@FunctionalInterface
public interface PacketSink {
    ByteBuf handle(ChannelConnection connection, PacketSide side, ByteBuf buffer);
}
