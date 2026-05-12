package net.momirealms.craftengine.bukkit.plugin.proxy;

import io.netty.buffer.Unpooled;
import net.momirealms.craftengine.bukkit.api.event.CraftEngineReloadEvent;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.core.font.BitmapImage;
import net.momirealms.craftengine.core.font.Image;
import net.momirealms.craftengine.core.font.ReferenceImage;
import net.momirealms.craftengine.core.plugin.locale.ServerLangData;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.core.util.Key;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.Map;
import java.util.Set;

public class ProxyMessageManager implements Listener {
    private static final String FONT_DATA_IDENTIFIER = "craftengine:font_data";
    private final BukkitCraftEngine plugin;
    private final boolean connectProxy;
    private boolean fontDataDirty = true;

    public ProxyMessageManager(BukkitCraftEngine plugin) {
        this.plugin = plugin;
        this.connectProxy = Bukkit.getServer().getServerConfig().isProxyEnabled();
        this.init();
    }

    private void init() {
        if (connectProxy) {
            Bukkit.getPluginManager().registerEvents(this, plugin.javaPlugin());
            Bukkit.getServer().getMessenger().registerOutgoingPluginChannel(plugin.javaPlugin(), FONT_DATA_IDENTIFIER);
            Bukkit.getServer().getMessenger().registerOutgoingPluginChannel(plugin.javaPlugin(), "BungeeCord");
        }
    }

    public boolean refreshFontData(Player player) {
        FriendlyByteBuf byteBuf = new FriendlyByteBuf(Unpooled.buffer());
        // OffsetFont
        this.plugin.fontManager().offsetFont().write(byteBuf);
        // Images
        Map<Key, Image> imageMap = this.plugin.fontManager().loadedImages();
        byteBuf.writeVarInt(imageMap.size());
        for (Map.Entry<Key, Image> entry : imageMap.entrySet()) {
            byteBuf.writeKey(entry.getKey());
            Image image = entry.getValue();
            if (image instanceof BitmapImage bitmapImage) {
                byteBuf.writeByte(0);
                bitmapImage.write(byteBuf);
            } else if (image instanceof ReferenceImage referenceImage) {
                byteBuf.writeByte(1);
                referenceImage.write(byteBuf);
            }
        }
        // L10n
        Set<String> serverLangKeys = this.plugin.translationManager().translationKeys();
        byteBuf.writeVarInt(serverLangKeys.size());
        for (String serverLangKey : serverLangKeys) {
            ServerLangData data = this.plugin.translationManager().translationData(serverLangKey);
            if (data != null) {
                byteBuf.writeUtf(serverLangKey);
                data.write(byteBuf);
            }
        }
        // Global
        Map<String, String> globalVariables = this.plugin.globalVariableManager().globalVariables();
        byteBuf.writeVarInt(globalVariables.size());
        for (Map.Entry<String, String> entry : globalVariables.entrySet()) {
            byteBuf.writeUtf(entry.getKey());
            byteBuf.writeUtf(entry.getValue());
        }

        player.sendPluginMessage(plugin.javaPlugin(), "craftengine:font_data", byteBuf.array());
        return true;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!fontDataDirty) {
            return;
        }
        Bukkit.getScheduler().runTaskLater(plugin.javaPlugin(), () -> {
            Player player = event.getPlayer();
            if (!player.isOnline()) return;
            if (this.refreshFontData(player)) {
                fontDataDirty = false;
            }
        }, 2L);
    }

    @EventHandler
    public void onPluginReload(CraftEngineReloadEvent event) {
        this.fontDataDirty = true;
        Bukkit.getOnlinePlayers().stream().findFirst().ifPresent(player -> {
            if (this.refreshFontData(player)) {
                fontDataDirty = false;
            }
        });
    }
}
