package net.momirealms.craftengine.proxy.common.tag;

import io.netty.buffer.Unpooled;
import net.momirealms.craftengine.proxy.common.ProxyCraftEngine;
import net.momirealms.craftengine.proxy.common.platform.ProxyPlayer;
import net.momirealms.craftengine.proxy.common.util.ProxyByteBuf;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class NetworkTagDataSyncService {
    public static final String TAG_DATA_CHANNEL = "craftengine:tag_data";
    public static final UUID PROXY_UUID = UUID.randomUUID();
    private final ProxyCraftEngine plugin;
    private final NetworkTagDataRegistry registry;

    public NetworkTagDataSyncService(ProxyCraftEngine plugin) {
        this.plugin = plugin;
        this.registry = new NetworkTagDataRegistry();
    }

    public NetworkTagDataRegistry registry() {
        return this.registry;
    }

    @Nullable
    public NetworkTagData getTagDataForPlayer(ProxyPlayer player) {
        return this.registry.getForPlayer(player);
    }

    public void sendTagDataVersion(ProxyPlayer player) {
        NetworkTagData netWorkTagData = this.getTagDataForPlayer(player);
        long version = netWorkTagData != null ? netWorkTagData.version() : -1L;

        ProxyByteBuf buf = new ProxyByteBuf(Unpooled.buffer());
        buf.writeLong(version);
        buf.writeUUID(PROXY_UUID);
        byte[] data = new byte[buf.readableBytes()];
        buf.getBytes(buf.readerIndex(), data);
        player.sendServerPluginMessage(TAG_DATA_CHANNEL, data);
    }

    public void receiveTagData(String serverName, ProxyByteBuf in) {
        this.registry.put(serverName, NetworkTagDataDeserializer.read(in, this.registry, serverName));
    }

    public void clear() {
        this.registry.clear();
    }
}
