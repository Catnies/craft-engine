package net.momirealms.craftengine.proxy.bungeecord.network.inject;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.util.ReferenceCountUtil;
import net.momirealms.craftengine.proxy.bungeecord.network.BungeeChannelConnection;
import net.momirealms.craftengine.proxy.common.network.protocol.PacketSide;

import java.util.List;

@ChannelHandler.Sharable
final class BungeePacketEncoder extends MessageToMessageEncoder<ByteBuf> {
    private final BungeePacketSink packetSink;
    private final BungeeChannelConnection connection;

    BungeePacketEncoder(BungeePacketSink packetSink, BungeeChannelConnection connection) {
        this.packetSink = packetSink;
        this.connection = connection;
    }

    @Override
    protected void encode(ChannelHandlerContext context, ByteBuf message, List<Object> output) {
        // packetSink 可能返回原始 buffer、替换 buffer 或空 buffer 来取消 packet
        ByteBuf result = this.packetSink.handle(this.connection, PacketSide.SERVER, message);
        if (!result.isReadable()) {
            if (result != message) {
                ReferenceCountUtil.release(result);
            }
            return;
        }
        output.add(result == message ? message.retain() : result);
    }
}
