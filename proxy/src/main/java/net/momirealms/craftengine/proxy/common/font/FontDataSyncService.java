package net.momirealms.craftengine.proxy.common.font;

import io.netty.buffer.Unpooled;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.proxy.common.platform.ProxyPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class FontDataSyncService {
    public static final String FONT_DATA_CHANNEL = "craftengine:font_data";
    public static final UUID PROXY_UUID = UUID.randomUUID();

    private final FontDataRegistry registry;
    private final FontDataCodec codec;

    public FontDataSyncService() {
        this.registry = new FontDataRegistry();
        this.codec = new FontDataCodec(this.registry);
    }

    public FontDataRegistry registry() {
        return this.registry;
    }

    @Nullable
    public FontData getFontDataForPlayer(ProxyPlayer player) {
        return this.registry.getForPlayer(player);
    }

    public void sendFontDataVersion(ProxyPlayer player) {
        FontData fontData = this.getFontDataForPlayer(player);
        long version = fontData != null ? fontData.version() : -1L;

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeLong(version);
        buf.writeUUID(PROXY_UUID);
        byte[] data = new byte[buf.readableBytes()];
        buf.getBytes(buf.readerIndex(), data);
        player.sendServerPluginMessage(FONT_DATA_CHANNEL, data);
    }

    public void receiveFontData(String serverName, FriendlyByteBuf in) {
        this.registry.put(serverName, this.codec.read(serverName, in));
    }

    public void clear() {
        this.registry.clear();
    }
}
