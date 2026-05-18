package net.momirealms.craftengine.proxy.velocity.network.inject;

import io.netty.buffer.ByteBuf;
import net.momirealms.craftengine.proxy.common.network.protocol.PacketSide;
import net.momirealms.craftengine.proxy.velocity.network.VelocityChannelConnection;

// 处理尚未转换为通用 ProxyPacketContext 的原始数据包
@FunctionalInterface
public interface VelocityPacketSink {

    ByteBuf handle(VelocityChannelConnection connection, PacketSide side, ByteBuf buffer);
}
