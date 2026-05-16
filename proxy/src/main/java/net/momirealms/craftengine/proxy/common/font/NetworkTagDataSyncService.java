package net.momirealms.craftengine.proxy.common.font;

import io.netty.buffer.Unpooled;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.proxy.common.platform.ProxyPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class NetworkTagDataSyncService {
    public static final String TAG_DATA_CHANNEL = "craftengine:tag_data";
    public static final UUID PROXY_UUID = UUID.randomUUID();

    private final NetworkTagDataRegistry registry;
    private final NetworkTagDataCodec codec;

    public NetworkTagDataSyncService() {
        this.registry = new NetworkTagDataRegistry();
        this.codec = new NetworkTagDataCodec(this.registry);
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

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeLong(version);
        buf.writeUUID(PROXY_UUID);
        byte[] data = new byte[buf.readableBytes()];
        buf.getBytes(buf.readerIndex(), data);
        player.sendServerPluginMessage(TAG_DATA_CHANNEL, data);
    }

    public void receiveTagData(String serverName, FriendlyByteBuf in) {
        this.registry.put(serverName, this.codec.read(serverName, in));
    }

    public void clear() {
        this.registry.clear();
    }
}
