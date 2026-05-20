package net.momirealms.craftengine.proxy.velocity.tag;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.momirealms.craftengine.proxy.common.tag.NetworkTagDataSyncService;
import net.momirealms.craftengine.proxy.common.util.ProxyByteBuf;
import net.momirealms.craftengine.proxy.velocity.VelocityCraftEngine;
import net.momirealms.craftengine.proxy.velocity.platform.VelocityPlayer;

public class VelocityNetworkTagDataBridge{
    public static final MinecraftChannelIdentifier IDENTIFIER = MinecraftChannelIdentifier.from(NetworkTagDataSyncService.TAG_DATA_CHANNEL);
    private final VelocityCraftEngine plugin;
    private final NetworkTagDataSyncService networkTagDataSyncService;

    public VelocityNetworkTagDataBridge(VelocityCraftEngine plugin) {
        this.plugin = plugin;
        this.networkTagDataSyncService = new NetworkTagDataSyncService(plugin);
        this.load();
    }

    public NetworkTagDataSyncService networkTagDataSyncService() {
        return this.networkTagDataSyncService;
    }

    public void load() {
        this.plugin.server.getChannelRegistrar().register(IDENTIFIER);
        this.plugin.server.getEventManager().register(this.plugin, this);
    }

    public void disable() {
        this.plugin.server.getChannelRegistrar().unregister(IDENTIFIER);
        this.plugin.server.getEventManager().unregisterListener(this.plugin, this);
        this.networkTagDataSyncService.clear();
    }

    @Subscribe
    public void onPlayerConnected(ServerPostConnectEvent event) {
        VelocityPlayer player = this.plugin.getPlayer(event.getPlayer().getUniqueId());
        if (player != null) {
            this.networkTagDataSyncService.sendTagDataVersion(player);
        }
    }

    @Subscribe
    public void receiveTagData(PluginMessageEvent event) {
        if (!IDENTIFIER.equals(event.getIdentifier())) return;
        event.setResult(PluginMessageEvent.ForwardResult.handled());

        ByteBuf buffer = Unpooled.buffer(event.getData().length);
        buffer.writeBytes(event.getData());
        ProxyByteBuf in = new ProxyByteBuf(buffer);
        String serverName = event.getSource() instanceof ServerConnection serverConnection
                ? serverConnection.getServer().getServerInfo().getName()
                : null;
        if (serverName == null) return;

        this.networkTagDataSyncService.receiveTagData(serverName, in);
    }
}
