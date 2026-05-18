package net.momirealms.craftengine.proxy.bungeecord.network.inject;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import net.momirealms.craftengine.core.util.ReflectionUtils;
import net.momirealms.craftengine.proxy.bungeecord.network.BungeeChannelConnection;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.function.Consumer;


final class BungeeChannelInitializer extends ChannelInitializer<Channel> {
    private static volatile Method initChannelMethod;

    private final BungeePacketPipelineInjector injector;
    private final ChannelInitializer<Channel> wrappedInitializer;
    private final BungeePacketSink packetSink;
    private final Consumer<BungeeChannelConnection> connectionRegisterer;
    private final Consumer<BungeeChannelConnection> connectionUnregister;

    BungeeChannelInitializer(
            BungeePacketPipelineInjector injector,
            ChannelInitializer<Channel> wrappedInitializer,
            BungeePacketSink packetSink,
            Consumer<BungeeChannelConnection> connectionRegisterer,
            Consumer<BungeeChannelConnection> connectionUnregister
    ) {
        this.injector = injector;
        this.wrappedInitializer = wrappedInitializer;
        this.packetSink = packetSink;
        this.connectionRegisterer = connectionRegisterer;
        this.connectionUnregister = connectionUnregister;
    }

    @Override
    protected void initChannel(@NotNull Channel channel) throws Exception {
        // 先让 Bungee 完成自己的 pipeline 构建, 再添加自定义的 handler
        this.invokeWrappedInitializer(channel);
        if (!this.injector.injected()) {
            return;
        }
        // 连接状态从 channel 创建时开始记录, 后续再绑定到 ProxyPlayer
        BungeeChannelConnection connection = new BungeeChannelConnection(channel);
        this.connectionRegisterer.accept(connection);
        BungeePacketPipelineInjector.addTo(channel, this.packetSink, connection);
        channel.closeFuture().addListener((ChannelFutureListener) future -> this.connectionUnregister.accept(connection));
    }

    boolean belongsTo(BungeePacketPipelineInjector injector) {
        return this.injector == injector;
    }

    ChannelInitializer<Channel> wrappedInitializer() {
        return this.wrappedInitializer;
    }

    // 调用 Bungee 原始 protected initializer 上的 ChannelInitializer#initChannel
    private void invokeWrappedInitializer(Channel channel) throws Exception {
        Method method = BungeeChannelInitializer.initChannelMethod;
        if (method == null) {
            method = ReflectionUtils.setAccessible(ChannelInitializer.class.getDeclaredMethod("initChannel", Channel.class));
            BungeeChannelInitializer.initChannelMethod = method;
        }
        method.invoke(this.wrappedInitializer, channel);
    }
}
