package net.momirealms.craftengine.proxy.velocity.font;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerCommon;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.momirealms.craftengine.core.plugin.Manageable;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.proxy.common.font.FontDataSyncService;
import net.momirealms.craftengine.proxy.common.network.TextReplacePacketListener;
import net.momirealms.craftengine.proxy.common.platform.ProxyPlayer;
import net.momirealms.craftengine.proxy.velocity.CraftEngineVelocityPlugin;
import net.momirealms.craftengine.proxy.velocity.platform.VelocityPlayer;
import org.jetbrains.annotations.Nullable;

public class VelocityFontDataBridge implements Manageable {
    public static final MinecraftChannelIdentifier IDENTIFIER = MinecraftChannelIdentifier.from(FontDataSyncService.FONT_DATA_CHANNEL);

    private final CraftEngineVelocityPlugin plugin;
    private final FontDataSyncService fontDataSyncService;
    private final TextReplacePacketListener textReplacePacketListener;
    private PacketListenerCommon registeredPacketListener;

    public VelocityFontDataBridge(CraftEngineVelocityPlugin plugin) {
        this.plugin = plugin;
        this.fontDataSyncService = new FontDataSyncService();
        this.textReplacePacketListener = new TextReplacePacketListener(this.fontDataSyncService, this::wrapPlayer);
    }

    public FontDataSyncService fontDataSyncService() {
        return this.fontDataSyncService;
    }

    @Override
    public void load() {
        this.plugin.server.getChannelRegistrar().register(IDENTIFIER);
        this.plugin.server.getEventManager().register(this.plugin, this);
        this.registeredPacketListener = PacketEvents.getAPI().getEventManager().registerListener(this.textReplacePacketListener, PacketListenerPriority.LOWEST);
    }

    @Override
    public void disable() {
        this.plugin.server.getChannelRegistrar().unregister(IDENTIFIER);
        this.plugin.server.getEventManager().unregisterListener(this.plugin, this);
        if (this.registeredPacketListener != null) {
            PacketEvents.getAPI().getEventManager().unregisterListener(this.registeredPacketListener);
        }
        this.fontDataSyncService.clear();
    }

    @Subscribe
    public void onPlayerConnected(ServerPostConnectEvent event) {
        this.fontDataSyncService.sendFontDataVersion(VelocityPlayer.wrapper(event.getPlayer()));
    }

    @Subscribe
    public void receiveFontData(PluginMessageEvent event) {
        if (!IDENTIFIER.equals(event.getIdentifier())) return;
        event.setResult(PluginMessageEvent.ForwardResult.handled());

        ByteBuf buffer = Unpooled.buffer(event.getData().length);
        buffer.writeBytes(event.getData());
        FriendlyByteBuf in = new FriendlyByteBuf(buffer);
        String serverName = event.getSource() instanceof ServerConnection serverConnection
                ? serverConnection.getServer().getServerInfo().getName()
                : null;
        if (serverName == null) return;

        this.fontDataSyncService.receiveFontData(serverName, in);
    }

    @Nullable
    private ProxyPlayer wrapPlayer(Object player) {
        return player instanceof Player velocityPlayer ? VelocityPlayer.wrapper(velocityPlayer) : null;
    }
}
