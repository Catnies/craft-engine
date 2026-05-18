package net.momirealms.craftengine.proxy.bungeecord.network.inject;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import net.md_5.bungee.api.ProxyServer;
import net.momirealms.craftengine.core.util.ReflectionUtils;
import net.momirealms.craftengine.core.util.SetMonitor;
import net.momirealms.craftengine.proxy.bungeecord.CraftEngineBungeeCordPlugin;
import net.momirealms.craftengine.proxy.common.network.ChannelConnection;
import net.momirealms.craftengine.proxy.common.network.packet.ProxyPacketSink;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.util.Set;
import java.util.function.Consumer;

public final class BungeePacketPipelineInjector {
    private static final String MINECRAFT_DECODER = "packet-decoder";
    private static final String MINECRAFT_ENCODER = "packet-encoder";
    private static final String PACKET_DECODER = "craftengine_proxy_packet_decoder";
    private static final String PACKET_ENCODER = "craftengine_proxy_packet_encoder";
    private static final Field LISTENERS_FIELD;
    private final CraftEngineBungeeCordPlugin plugin;
    private final ProxyPacketSink packetSink; // raw ByteBuf 捕获回调
    private final Consumer<ChannelConnection> connectionRegisterer; // 新 Channel 注册回调
    private final Consumer<ChannelConnection> connectionUnregister; // Channel 关闭清理回调
    private volatile boolean injected; // initializer 是否处于注入状态

    static {
        LISTENERS_FIELD = ReflectionUtils.getDeclaredField(ProxyServer.getInstance().getClass(), "listeners");
    }

    public BungeePacketPipelineInjector(
            CraftEngineBungeeCordPlugin plugin,
            ProxyPacketSink packetSink,
            Consumer<ChannelConnection> connectionRegisterer,
            Consumer<ChannelConnection> connectionUnregister
    ) {
        this.plugin = plugin;
        this.packetSink = packetSink;
        this.connectionRegisterer = connectionRegisterer;
        this.connectionUnregister = connectionUnregister;
    }

    // 注入服务端 channel initializer
    @SuppressWarnings("unchecked")
    public void inject() {
        try {
            Set<Channel> listeners = (Set<Channel>) LISTENERS_FIELD.get(ProxyServer.getInstance());
            for (Channel channel : listeners) {
                this.injectChannel(channel);
            }

            Set<Channel> wrapper = new SetMonitor<>(listeners, this::injectChannel, channel -> {});
            LISTENERS_FIELD.set(ProxyServer.getInstance(), wrapper);
            this.injected = true;
        } catch (IllegalAccessException e) {
            PrintWriter printWriter = new PrintWriter(new StringWriter());
            e.printStackTrace(printWriter);
            this.plugin.getLogger().severe(printWriter.toString());
        }
    }

    // 撤销注入的 channel initializer.
    public void uninject() {
    }

    boolean injected() {
        return this.injected;
    }

    public void injectChannel(Channel channel) {
        Field initializerField = null;
        ChannelHandler bootstrapAcceptor = null;

        for (String channelName : channel.pipeline().names()) {
            if (channelName.contains("QueryHandler")) {
                return;
            }
            ChannelHandler handler = channel.pipeline().get(channelName);
            if (handler == null) continue;
            bootstrapAcceptor = handler;
            initializerField = ReflectionUtils.getDeclaredField(handler.getClass(), "childHandler");
        }
        if (bootstrapAcceptor == null) {
            bootstrapAcceptor = channel.pipeline().first();
            initializerField = ReflectionUtils.getDeclaredField(bootstrapAcceptor.getClass(), "childHandler");
        }

        try {
            @SuppressWarnings("unchecked")
            ChannelInitializer<Channel> newInitializer = new BungeeChannelInitializer(
                    this,
                    (ChannelInitializer<Channel>) initializerField.get(bootstrapAcceptor),
                    this.packetSink,
                    this.connectionRegisterer,
                    this.connectionUnregister
            );
            initializerField.set(bootstrapAcceptor, newInitializer);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    // 将数据包捕获 handler 添加到 BungeeCord Minecraft codec handler 之前
    public static void addTo(Channel channel, ProxyPacketSink packetSink, ChannelConnection connection) {
        ChannelPipeline pipeline = channel.pipeline();
        BungeePacketPipelineInjector.removeHandlers(channel);

        if (pipeline.get(BungeePacketPipelineInjector.MINECRAFT_DECODER) != null) {
            pipeline.addBefore(BungeePacketPipelineInjector.MINECRAFT_DECODER, BungeePacketPipelineInjector.PACKET_DECODER, new BungeePacketDecoder(packetSink, connection));
        }
        if (pipeline.get(BungeePacketPipelineInjector.MINECRAFT_ENCODER) != null) {
            pipeline.addBefore(BungeePacketPipelineInjector.MINECRAFT_ENCODER, BungeePacketPipelineInjector.PACKET_ENCODER, new BungeePacketEncoder(packetSink, connection));
        }
    }

    // 从已注入的 channel 中移除 handler
    public static void removeHandlers(Channel channel) {
        ChannelPipeline pipeline = channel.pipeline();
        if (pipeline.get(BungeePacketPipelineInjector.PACKET_DECODER) != null) {
            pipeline.remove(BungeePacketPipelineInjector.PACKET_DECODER);
        }
        if (pipeline.get(BungeePacketPipelineInjector.PACKET_ENCODER) != null) {
            pipeline.remove(BungeePacketPipelineInjector.PACKET_ENCODER);
        }
    }

    // 启用压缩并改变 pipeline 后, 将数据包 handler 移回 BungeeCord codec 之前
    public static void relocate(ChannelPipeline pipeline) {
        BungeePacketPipelineInjector.relocate(pipeline, BungeePacketPipelineInjector.MINECRAFT_ENCODER, BungeePacketPipelineInjector.PACKET_ENCODER);
        BungeePacketPipelineInjector.relocate(pipeline, BungeePacketPipelineInjector.MINECRAFT_DECODER, BungeePacketPipelineInjector.PACKET_DECODER);
    }

    // 保留同一个 handler 实例, 并将其重新添加到目标 handler 之前
    private static void relocate(ChannelPipeline pipeline, String target, String handlerName) {
        ChannelHandler handler = pipeline.get(handlerName);
        if (handler != null && pipeline.get(target) != null) {
            pipeline.remove(handlerName);
            pipeline.addBefore(target, handlerName, handler);
        }
    }
}
