package net.momirealms.craftengine.bukkit.plugin.proxy;

import io.netty.buffer.Unpooled;
import net.momirealms.craftengine.bukkit.api.event.CraftEngineReloadEvent;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.core.font.NetworkTagDataSerializer;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ProxyMessageManager implements Listener, PluginMessageListener {
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
        if (connectProxy && Config.enableNetworkTagDataProxy()) {
            Bukkit.getPluginManager().registerEvents(this, plugin.javaPlugin());
            Bukkit.getServer().getMessenger().registerOutgoingPluginChannel(plugin.javaPlugin(), TAG_DATA_IDENTIFIER);
            Bukkit.getServer().getMessenger().registerIncomingPluginChannel(plugin.javaPlugin(), TAG_DATA_IDENTIFIER, this);
        }
    }

    private FriendlyByteBuf buildTagDataBuf() {
        FriendlyByteBuf byteBuf = new FriendlyByteBuf(Unpooled.buffer());
        byteBuf.writeUtf(Config.networkTagDataSecret()); // Secret
        byteBuf.writeLong(System.currentTimeMillis()); // Version
        NetworkTagDataSerializer.writeOffsetFont(byteBuf, this.plugin.fontManager().offsetFont());
        NetworkTagDataSerializer.writeImages(byteBuf, this.plugin.fontManager().loadedImages());
        NetworkTagDataSerializer.writeL10n(byteBuf, this.plugin.translationManager());
        NetworkTagDataSerializer.writeGlobalVariables(byteBuf, this.plugin.globalVariableManager());
        return byteBuf;
    }

    public boolean sendTagData(Player player) {
        if (player.isConnected() && !Config.networkTagDataSecret().isEmpty()) {
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
        if (Config.networkTagDataSecret().isEmpty()) return;

        this.networkTagDataVersion = System.currentTimeMillis();
        this.tagDataBuf = this.buildTagDataBuf();
        this.proxyPlayers.values().forEach(set -> {
            for (UUID playerUUID : set) {
                Player player = Bukkit.getPlayer(playerUUID);
                if (player != null && player.isConnected()) {
                    this.sendTagData(player);
                    break;
                }
            }
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
        String serverSecret = Config.networkTagDataSecret();
        if (!channel.equals("craftengine:tag_data") || serverSecret.isEmpty()) {
            return;
        }

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(message));
        String readSecret = buf.readUtf();
        if (serverSecret.equals(readSecret)) {
            long dataVersion = buf.readLong();
            UUID proxyUUID = buf.readUUID();
            // 记录玩家所在的代理服务器.
            proxyPlayers.computeIfAbsent(proxyUUID, it -> ConcurrentHashMap.newKeySet()).add(player.getUniqueId());
            proxyByPlayer.put(player.getUniqueId(), proxyUUID);
            // 更新字体数据.
            if (dataVersion != this.networkTagDataVersion) {
                this.sendTagData(player);
            }
        }
    }
}
