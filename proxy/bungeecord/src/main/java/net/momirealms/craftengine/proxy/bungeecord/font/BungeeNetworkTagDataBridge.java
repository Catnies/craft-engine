package net.momirealms.craftengine.proxy.bungeecord.font;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerCommon;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.momirealms.craftengine.core.plugin.Manageable;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.proxy.bungeecord.CraftEngineBungeeCordPlugin;
import net.momirealms.craftengine.proxy.bungeecord.platform.BungeePlayer;
import net.momirealms.craftengine.proxy.common.font.NetworkTagDataSyncService;
import net.momirealms.craftengine.proxy.common.network.TextReplacePacketListener;
import net.momirealms.craftengine.proxy.common.platform.ProxyPlayer;
import org.jetbrains.annotations.Nullable;

public class BungeeNetworkTagDataBridge implements Manageable, Listener {
    public static final String IDENTIFIER = NetworkTagDataSyncService.TAG_DATA_CHANNEL;

    private final CraftEngineBungeeCordPlugin plugin;
    private final NetworkTagDataSyncService networkTagDataSyncService;
    private final TextReplacePacketListener textReplacePacketListener;
    private PacketListenerCommon registeredPacketListener;

    public BungeeNetworkTagDataBridge(CraftEngineBungeeCordPlugin plugin) {
        this.plugin = plugin;
        this.networkTagDataSyncService = new NetworkTagDataSyncService();
        this.textReplacePacketListener = new TextReplacePacketListener(this.networkTagDataSyncService, this::wrapPlayer);
    }

    public NetworkTagDataSyncService networkTagDataSyncService() {
        return this.networkTagDataSyncService;
    }

    @Override
    public void load() {
        this.plugin.getProxy().registerChannel(IDENTIFIER);
        this.plugin.getProxy().getPluginManager().registerListener(this.plugin, this);
        this.registeredPacketListener = PacketEvents.getAPI().getEventManager().registerListener(this.textReplacePacketListener, PacketListenerPriority.LOWEST);
    }

    @Override
    public void disable() {
        this.plugin.getProxy().unregisterChannel(IDENTIFIER);
        this.plugin.getProxy().getPluginManager().unregisterListener(this);
        if (this.registeredPacketListener != null) {
            PacketEvents.getAPI().getEventManager().unregisterListener(this.registeredPacketListener);
        }
        this.networkTagDataSyncService.clear();
    }

    @EventHandler
    public void onServerConnected(ServerConnectedEvent event) {
        this.networkTagDataSyncService.sendTagDataVersion(BungeePlayer.wrapper(event.getPlayer()));
    }

    @EventHandler
    public void receiveTagData(PluginMessageEvent event) {
        if (!IDENTIFIER.equals(event.getTag())) return;
        event.setCancelled(true);
        if (!(event.getSender() instanceof Server server)) return;

        ByteBuf buffer = Unpooled.buffer(event.getData().length);
        buffer.writeBytes(event.getData());
        FriendlyByteBuf in = new FriendlyByteBuf(buffer);
        this.networkTagDataSyncService.receiveTagData(server.getInfo().getName(), in);
    }

    @Nullable
    private ProxyPlayer wrapPlayer(Object player) {
        return player instanceof ProxiedPlayer proxiedPlayer ? BungeePlayer.wrapper(proxiedPlayer) : null;
    }
}
