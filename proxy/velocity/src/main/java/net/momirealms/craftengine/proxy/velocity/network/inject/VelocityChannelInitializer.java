package net.momirealms.craftengine.proxy.velocity.network.inject;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import net.momirealms.craftengine.core.util.ReflectionUtils;
import net.momirealms.craftengine.proxy.common.network.ChannelConnection;
import net.momirealms.craftengine.proxy.common.network.packet.ProxyPacketSink;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.function.Consumer;


final class VelocityChannelInitializer extends ChannelInitializer<Channel> {
    private static volatile Method initChannelMethod;

    private final VelocityPacketPipelineInjector injector;
    private final ChannelInitializer<Channel> wrappedInitializer;
    private final ProxyPacketSink packetSink;
    private final Consumer<ChannelConnection> connectionRegisterer;
    private final Consumer<ChannelConnection> connectionUnregister;

    VelocityChannelInitializer(
            VelocityPacketPipelineInjector injector,
            ChannelInitializer<Channel> wrappedInitializer,
            ProxyPacketSink packetSink,
            Consumer<ChannelConnection> connectionRegisterer,
            Consumer<ChannelConnection> connectionUnregister
    ) {
        this.injector = injector;
        this.wrappedInitializer = wrappedInitializer;
        this.packetSink = packetSink;
        this.connectionRegisterer = connectionRegisterer;
        this.connectionUnregister = connectionUnregister;
    }

    @Override
    protected void initChannel(@NotNull Channel channel) throws Exception {
        // 先让 Velocity 完成自己的 pipeline 构建, 再添加自定义的 handler
        this.invokeWrappedInitializer(channel);
        if (!this.injector.injected()) {
            return;
        }
        // 连接状态从 channel 创建时开始记录, 后续再绑定到 ProxyPlayer
        ChannelConnection connection = new ChannelConnection(channel);
        this.connectionRegisterer.accept(connection);
        VelocityPacketPipelineInjector.addTo(channel, this.packetSink, connection);
        channel.closeFuture().addListener((ChannelFutureListener) future -> this.connectionUnregister.accept(connection));
    }

    boolean belongsTo(VelocityPacketPipelineInjector injector) {
        return this.injector == injector;
    }

    ChannelInitializer<Channel> wrappedInitializer() {
        return this.wrappedInitializer;
    }

    // 调用 Velocity 原始 protected initializer 上的 ChannelInitializer#initChannel
    private void invokeWrappedInitializer(Channel channel) throws Exception {
        Method method = initChannelMethod;
        if (method == null) {
            method = ReflectionUtils.setAccessible(ChannelInitializer.class.getDeclaredMethod("initChannel", Channel.class));
            initChannelMethod = method;
        }
        method.invoke(this.wrappedInitializer, channel);
    }
}
