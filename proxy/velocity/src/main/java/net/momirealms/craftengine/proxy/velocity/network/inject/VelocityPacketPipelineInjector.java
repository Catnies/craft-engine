package net.momirealms.craftengine.proxy.velocity.network.inject;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import net.momirealms.craftengine.core.util.ReflectionUtils;
import net.momirealms.craftengine.proxy.velocity.CraftEngineVelocityPlugin;
import net.momirealms.craftengine.proxy.velocity.network.VelocityChannelConnection;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class VelocityPacketPipelineInjector {
    private static final String MINECRAFT_DECODER = "minecraft-decoder";
    private static final String MINECRAFT_ENCODER = "minecraft-encoder";
    private static final String PACKET_DECODER = "craftengine_proxy_packet_decoder";
    private static final String PACKET_ENCODER = "craftengine_proxy_packet_encoder";
    private static final String CONNECTION_MANAGER_CLASS_NAME = "com.velocitypowered.proxy.network.ConnectionManager";
    private static final String SERVER_INITIALIZER_HOLDER_CLASS_NAME = "com.velocitypowered.proxy.network.ServerChannelInitializerHolder";

    private final CraftEngineVelocityPlugin plugin;
    private final VelocityPacketSink packetSink; // raw ByteBuf 捕获回调
    private final Consumer<VelocityChannelConnection> connectionRegisterer; // 新 Channel 注册回调
    private final Consumer<VelocityChannelConnection> connectionUnregister; // Channel 关闭清理回调
    private volatile boolean injected; // initializer 是否处于注入状态

    public VelocityPacketPipelineInjector(
            CraftEngineVelocityPlugin plugin,
            VelocityPacketSink packetSink,
            Consumer<VelocityChannelConnection> connectionRegisterer,
            Consumer<VelocityChannelConnection> connectionUnregister
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
            Object holder = this.serverInitializerHolder();
            Supplier<ChannelInitializer<Channel>> supplier = (Supplier<ChannelInitializer<Channel>>) holder;
            ChannelInitializer<Channel> current = supplier.get();
            if (current instanceof VelocityChannelInitializer initializer) {
                // 如果注入的目标和当前一致, 则代表是重复注入, 忽略.
                if (initializer.belongsTo(this)) {
                    this.injected = true;
                    return;
                }
                // 如果已有其他 CraftEngine wrapper, 取回原始 initializer 后再包装
                current = initializer.wrappedInitializer();
            }
            // 注入
            Method setter = this.initializerSetter(holder.getClass());
            setter.invoke(holder, new VelocityChannelInitializer(
                    this,
                    current,
                    this.packetSink,
                    this.connectionRegisterer,
                    this.connectionUnregister
            ));
            this.injected = true;
        } catch (ReflectiveOperationException | ClassCastException e) {
            this.plugin.logger.warn("Failed to inject CraftEngine Velocity packet capture service", e);
        }
    }

    // 撤销注入的 channel initializer.
    @SuppressWarnings("unchecked")
    public void uninject() {
        this.injected = false;
        try {
            Object holder = this.serverInitializerHolder();
            Supplier<ChannelInitializer<Channel>> supplier = (Supplier<ChannelInitializer<Channel>>) holder;
            ChannelInitializer<Channel> current = supplier.get();
            if (!(current instanceof VelocityChannelInitializer initializer) || !initializer.belongsTo(this)) {
                return;
            }

            Method setter = this.initializerSetter(holder.getClass());
            setter.invoke(holder, initializer.wrappedInitializer());
        } catch (ReflectiveOperationException | ClassCastException e) {
            this.plugin.logger.warn("Failed to uninject CraftEngine Velocity packet capture service", e);
        }
    }

    boolean injected() {
        return this.injected;
    }

    // 获取 ServerChannelInitializerHolder
    private Object serverInitializerHolder() throws ReflectiveOperationException {
        Class<?> connectionManagerClass = ReflectionUtils.getClazz(CONNECTION_MANAGER_CLASS_NAME);
        Class<?> initializerHolderClass = ReflectionUtils.getClazz(SERVER_INITIALIZER_HOLDER_CLASS_NAME);
        if (connectionManagerClass == null || initializerHolderClass == null) {
            throw new ClassNotFoundException("Velocity connection manager or server initializer holder is unavailable");
        }

        Field connectionManagerField = ReflectionUtils.getDeclaredField(this.plugin.server, connectionManagerClass, 0);
        if (connectionManagerField == null) {
            throw new NoSuchFieldException("No field of type " + connectionManagerClass.getName() + " found in " + this.plugin.server.getClass().getName());
        }

        Object connectionManager = connectionManagerField.get(this.plugin.server);
        Field serverInitializerHolderField = ReflectionUtils.getDeclaredField(connectionManager, initializerHolderClass, 0);
        if (serverInitializerHolderField == null) {
            throw new NoSuchFieldException("No field of type " + initializerHolderClass.getName() + " found in " + connectionManager.getClass().getName());
        }
        return serverInitializerHolderField.get(connectionManager);
    }

    // 在 ServerChannelInitializerHolder 中查找设置 ChannelInitializer 的方法.
    private Method initializerSetter(Class<?> holderClass) throws NoSuchMethodException {
        Method publicSetter = ReflectionUtils.getMethod(holderClass, void.class, ChannelInitializer.class);
        if (publicSetter != null) {
            return ReflectionUtils.setAccessible(publicSetter);
        }
        for (Method method : holderClass.getDeclaredMethods()) {
            if (method.getParameterCount() == 1 && ChannelInitializer.class.isAssignableFrom(method.getParameterTypes()[0])) {
                return ReflectionUtils.setAccessible(method);
            }
        }
        throw new NoSuchMethodException("No ChannelInitializer setter found in " + holderClass.getName());
    }

    // 将数据包捕获 handler 添加到 Velocity Minecraft codec handler 之前
    public static void addTo(Channel channel, VelocityPacketSink packetSink, VelocityChannelConnection connection) {
        ChannelPipeline pipeline = channel.pipeline();
        removeHandlers(channel);

        if (pipeline.get(MINECRAFT_DECODER) != null) {
            pipeline.addBefore(MINECRAFT_DECODER, PACKET_DECODER, new VelocityPacketDecoder(packetSink, connection));
        }
        if (pipeline.get(MINECRAFT_ENCODER) != null) {
            pipeline.addBefore(MINECRAFT_ENCODER, PACKET_ENCODER, new VelocityPacketEncoder(packetSink, connection));
        }
    }

    // 从已注入的 channel 中移除 handler
    public static void removeHandlers(Channel channel) {
        ChannelPipeline pipeline = channel.pipeline();
        if (pipeline.get(PACKET_DECODER) != null) {
            pipeline.remove(PACKET_DECODER);
        }
        if (pipeline.get(PACKET_ENCODER) != null) {
            pipeline.remove(PACKET_ENCODER);
        }
    }

    // 检查 Velocity 是否启用了压缩包
    public static boolean isCompressionEnabledEvent(Object event) {
        return event instanceof Enum<?> value
                && value.name().equals("COMPRESSION_ENABLED")
                && value.getClass().getName().equals("com.velocitypowered.proxy.protocol.VelocityConnectionEvent");
    }

    // 启用压缩并改变 pipeline 后, 将数据包 handler 移回 Velocity codec 之前
    public static void relocate(ChannelPipeline pipeline) {
        relocate(pipeline, MINECRAFT_ENCODER, PACKET_ENCODER);
        relocate(pipeline, MINECRAFT_DECODER, PACKET_DECODER);
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
