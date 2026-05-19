package net.momirealms.craftengine.proxy.bungeecord.tag;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.momirealms.craftengine.proxy.bungeecord.BungeeCordCraftEngine;
import net.momirealms.craftengine.proxy.bungeecord.platform.BungeePlayer;
import net.momirealms.craftengine.proxy.common.tag.NetworkTagDataSyncService;
import net.momirealms.craftengine.proxy.common.util.ProxyByteBuf;

public class BungeeNetworkTagDataBridge implements Listener {
    public static final String IDENTIFIER = NetworkTagDataSyncService.TAG_DATA_CHANNEL;

    private final BungeeCordCraftEngine plugin;
    private final NetworkTagDataSyncService networkTagDataSyncService;

    public BungeeNetworkTagDataBridge(BungeeCordCraftEngine plugin) {
        this.plugin = plugin;
        this.networkTagDataSyncService = new NetworkTagDataSyncService(plugin);
        this.load();
    }

    public NetworkTagDataSyncService networkTagDataSyncService() {
        return this.networkTagDataSyncService;
    }

    public void load() {
        this.plugin.getProxy().registerChannel(IDENTIFIER);
        this.plugin.getProxy().getPluginManager().registerListener(this.plugin, this);
    }

    public void disable() {
        this.plugin.getProxy().unregisterChannel(IDENTIFIER);
        this.plugin.getProxy().getPluginManager().unregisterListener(this);
        this.networkTagDataSyncService.clear();
    }

    @EventHandler
    public void onServerConnected(ServerConnectedEvent event) {
        this.networkTagDataSyncService.sendTagDataVersion(BungeePlayer.wrap(event.getPlayer()));
    }

    @EventHandler
    public void receiveTagData(PluginMessageEvent event) {
        if (!IDENTIFIER.equals(event.getTag())) return;
        event.setCancelled(true);
        if (!(event.getSender() instanceof Server server)) return;

        ByteBuf buffer = Unpooled.buffer(event.getData().length);
        buffer.writeBytes(event.getData());
        ProxyByteBuf in = new ProxyByteBuf(buffer);
        this.networkTagDataSyncService.receiveTagData(server.getInfo().getName(), in);
    }
}
