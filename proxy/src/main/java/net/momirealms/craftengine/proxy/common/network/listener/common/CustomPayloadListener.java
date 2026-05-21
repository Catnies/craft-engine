package net.momirealms.craftengine.proxy.common.network.listener.common;

import net.momirealms.craftengine.proxy.common.ProxyCraftEngine;
import net.momirealms.craftengine.proxy.common.network.ChannelConnection;
import net.momirealms.craftengine.proxy.common.network.packet.PacketContext;
import net.momirealms.craftengine.proxy.common.network.packet.PacketHandler;
import net.momirealms.craftengine.proxy.common.network.packet.PacketHandlerRegistry;
import net.momirealms.craftengine.proxy.common.network.packet.PacketRoute;
import net.momirealms.craftengine.proxy.common.network.protocol.ConnectionState;
import net.momirealms.craftengine.proxy.common.network.protocol.packettype.PacketType;
import net.momirealms.craftengine.proxy.common.platform.BackendServer;
import net.momirealms.craftengine.proxy.common.platform.ProxyPlayer;
import net.momirealms.craftengine.proxy.common.tag.NetworkTagDataSyncService;
import net.momirealms.craftengine.proxy.common.util.Key;
import net.momirealms.craftengine.proxy.common.util.ProxyByteBuf;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public final class CustomPayloadListener {
    private CustomPayloadListener() {}

    public static void register(PacketHandlerRegistry registry, ProxyCraftEngine plugin) {
        registry.register(PacketRoute.typed(ConnectionState.CONFIGURATION, PacketType.Configuration.Client.PLUGIN_MESSAGE), new Client_Handler(plugin));
        registry.register(PacketRoute.typed(ConnectionState.CONFIGURATION, PacketType.Configuration.Server.PLUGIN_MESSAGE), new Server_Handler(plugin));
        registry.register(PacketRoute.typed(ConnectionState.PLAY, PacketType.Play.Client.PLUGIN_MESSAGE), new Client_Handler(plugin));
        registry.register(PacketRoute.typed(ConnectionState.PLAY, PacketType.Play.Server.PLUGIN_MESSAGE), new Server_Handler(plugin));
    }

    private static final class Client_Handler implements PacketHandler {
        private final ProxyCraftEngine plugin;

        private Client_Handler(ProxyCraftEngine plugin) {
            this.plugin = plugin;
        }

        @Override
        public void handle(ChannelConnection connection, @Nullable ProxyPlayer player, PacketContext packet) {
            ProxyByteBuf buf = packet.payload();
            Key channel = buf.readKey();
            // NetworkTagData -> 这里仅拦截客户端发送给Proxy的伪造包
            if (channel.equals(NetworkTagDataSyncService.TAG_DATA_CHANNEL_KEY)) {
                packet.setCancelled(true);
            }
        }
    }

    private static final class Server_Handler implements PacketHandler {
        private final ProxyCraftEngine plugin;

        private Server_Handler(ProxyCraftEngine plugin) {
            this.plugin = plugin;
        }

        @Override
        public void handle(ChannelConnection connection, @Nullable ProxyPlayer player, PacketContext packet) {
            ProxyByteBuf buf = packet.payload();
            Key channel = buf.readKey();
            // NetworkTagData
            if (channel.equals(NetworkTagDataSyncService.TAG_DATA_CHANNEL_KEY)) {
                Optional.ofNullable(connection.player())
                        .map(ProxyPlayer::server)
                        .map(BackendServer::name)
                        .ifPresent(name -> {
                            this.plugin.networkTagDataSyncService().receiveTagData(name, buf);
                        });
            }
        }
    }
}
