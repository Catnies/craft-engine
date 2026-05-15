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
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ProxyMessageManager implements Listener, PluginMessageListener {
    private static final String TAG_DATA_IDENTIFIER = "craftengine:tag_data";
    private final BukkitCraftEngine plugin;
    private final boolean connectProxy;
    private final Map<UUID, Set<UUID>> proxyPlayers = new ConcurrentHashMap<>(); // ProxyUUID -> Set<PlayerUUID>
    private final Map<UUID, UUID> proxyByPlayer = new ConcurrentHashMap<>(); // PlayerUUID -> ProxyUUID
    private long networkTagDataVersion = System.currentTimeMillis();
    private FriendlyByteBuf tagDataBuf;

    public ProxyMessageManager(BukkitCraftEngine plugin) {
        this.plugin = plugin;
        this.connectProxy = Bukkit.getServer().getServerConfig().isProxyEnabled();
        this.init();
    }

    private void init() {
        if (connectProxy) {
            Bukkit.getPluginManager().registerEvents(this, plugin.javaPlugin());
            Bukkit.getServer().getMessenger().registerOutgoingPluginChannel(plugin.javaPlugin(), TAG_DATA_IDENTIFIER);
            Bukkit.getServer().getMessenger().registerIncomingPluginChannel(plugin.javaPlugin(), TAG_DATA_IDENTIFIER, this);
        }
    }

    private FriendlyByteBuf buildTagDataBuf() {
        FriendlyByteBuf byteBuf = new FriendlyByteBuf(Unpooled.buffer());
        // Version
        byteBuf.writeLong(System.currentTimeMillis());
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

        return byteBuf;
    }

    public boolean sendTagData(Player player) {
        if (player.isConnected()) {
            if (this.tagDataBuf == null) {
                this.tagDataBuf = this.buildTagDataBuf();
            }
            player.sendPluginMessage(plugin.javaPlugin(), "craftengine:tag_data", this.tagDataBuf.array());
            return true;
        }
        return false;
    }

    // 插件重载, 让每个玩家都刷新自身链接的代理服务器.
    @EventHandler
    public void onPluginReload(CraftEngineReloadEvent event) {
        this.networkTagDataVersion = System.currentTimeMillis();
        this.tagDataBuf = this.buildTagDataBuf();
        this.proxyPlayers.values().forEach(set -> {
            set.stream()
                    .findFirst()
                    .map(Bukkit::getPlayer)
                    .map(this::sendTagData);
        });
    }

    // 玩家离开服务器, 清理缓存数据
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerUUID = event.getPlayer().getUniqueId();
        Optional.ofNullable(this.proxyByPlayer.remove(playerUUID))
                .map(this.proxyPlayers::get)
                .map(it -> it.remove(playerUUID));
    }

    // 当收到玩家进服后的数据版本号, 决定是否要重发包回去.
    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, byte @NotNull [] message) {
        if (!channel.equals("craftengine:tag_data")) return;
        System.out.println(Bukkit.isPrimaryThread());
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(message));
        long dataVersion = buf.readLong();
        UUID proxyUUID = buf.readUUID();
        // 记录玩家所在的代理服务器.
        proxyPlayers.computeIfAbsent(proxyUUID, it -> new HashSet<>()).add(player.getUniqueId());
        proxyByPlayer.put(player.getUniqueId(), proxyUUID);
        // 更新字体数据.
        if (dataVersion != this.networkTagDataVersion) {
            this.sendTagData(player);
        }
    }
}
