package net.momirealms.craftengine.proxy.bungeecord.network;

import net.momirealms.craftengine.proxy.bungeecord.CraftEngineBungeeCordPlugin;
import net.momirealms.craftengine.proxy.common.CraftEngineProxyPlugin;
import net.momirealms.craftengine.proxy.common.network.listener.PacketListenerManager;
import net.momirealms.craftengine.proxy.common.network.packet.PacketRegistration;
import net.momirealms.craftengine.proxy.common.network.protocol.PacketSide;
import net.momirealms.craftengine.proxy.common.network.protocol.packettype.PacketType;

import java.util.ArrayList;
import java.util.List;

public class BungeePacketListenerManager extends PacketListenerManager {
    private final CraftEngineBungeeCordPlugin plugin;
    private final PacketListenerManager.ErrorHandler errorHandler;
    private final List<PacketRegistration> internalRegistrations = new ArrayList<>(); // 内部协议状态监听
    private volatile boolean loaded;

    public BungeePacketListenerManager(CraftEngineBungeeCordPlugin plugin) {
        super();
        this.plugin = plugin;
        this.errorHandler = this::handlePacketError;
        this.load();
    }

    @Override
    public void load() {
        if (this.loaded) {
            return;
        }
        PacketType.prepare();
        this.loaded = true;

        // 先注册内部状态监听, 再开始
        super.registerInternalRegistrations();
        // 注册常规监听器
        this.registerPacketListeners();
        // 注册玩家监听器, 注入管道, 接入 Netty 流量
//        this.plugin.server.getEventManager().register(this.plugin, this);
//        this.pipelineInjector.inject();
    }

    private void handlePacketError(int packetId, PacketSide side, Throwable throwable) {
        this.plugin.getLogger().warning("An error occurred when handling Velocity packet " + packetId + " (" + side + ")");
    }

    @Override
    public ErrorHandler errorHandler() {
        return this.errorHandler;
    }

    @Override
    public CraftEngineProxyPlugin plugin() {
        return this.plugin;
    }

}
