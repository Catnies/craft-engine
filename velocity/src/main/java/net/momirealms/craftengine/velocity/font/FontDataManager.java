package net.momirealms.craftengine.velocity.font;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.ChannelMessageSource;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.momirealms.craftengine.core.font.*;
import net.momirealms.craftengine.core.plugin.locale.ServerLangData;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.LazyReference;
import net.momirealms.craftengine.velocity.CraftEnginePlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FontDataManager {
    public static final MinecraftChannelIdentifier IDENTIFIER = MinecraftChannelIdentifier.from("craftengine:font_data");
    private final CraftEnginePlugin plugin;
    // ServerName -> FontData
    public final Map<String, FontData> serverFonts = new ConcurrentHashMap<>();

    public FontDataManager(CraftEnginePlugin plugin) {
        this.plugin = plugin;
        this.plugin.server.getChannelRegistrar().register(IDENTIFIER);
        this.plugin.server.getEventManager().register(this.plugin, this);
    }

    // 收取后端服务器发送回的字体数据
    @Subscribe
    public void receiveFontData(PluginMessageEvent event) {
        if (!FontDataManager.IDENTIFIER.equals(event.getIdentifier())) return;
        ByteBuf buffer = Unpooled.buffer(event.getData().length);
        buffer.writeBytes(event.getData());
        FriendlyByteBuf in = new FriendlyByteBuf(buffer);
        event.setResult(PluginMessageEvent.ForwardResult.handled());
        ChannelMessageSource eventSource = event.getSource();
        String serverName = eventSource instanceof ServerConnection serverConnection
                ? serverConnection.getServer().getServerInfo().getName()
                : null;
        if (serverName == null) return;

        // offset
        OffsetFont offsetFont = new OffsetFont(in);
        // Images
        Map<Key, Image> images = new HashMap<>();
        int imageSize = in.readVarInt();
        for (int i = 0; i < imageSize; i++) {
            Key key = in.readKey();
            byte type = in.readByte();
            Image image = null;
            // BitmapImage
            if (type == 0) {
                image = BitmapImage.read(in);
            }
            // ReferenceImage
            else if (type == 1) {
                Key refId = in.readKey();
                image = new ReferenceImage(LazyReference.lazyReference(() -> {
                    FontData fontData = this.serverFonts.get(serverName);
                    if (fontData != null) {
                        Image img = fontData.images().get(refId);
                        if (img instanceof BitmapImage bitmapImage) {
                            return bitmapImage;
                        }
                    }
                    return DummyImage.INSTANCE;
                }), refId, 0, 0);
            }
            if (image != null) {
                images.put(key, image);
            }
        }
        // l10n
        Map<String, ServerLangData> l10n = new HashMap<>();
        int l10nSize = in.readVarInt();
        for (int i = 0; i < l10nSize; i++) {
            String langKey = in.readUtf();
            ServerLangData langData = ServerLangData.read(in);
            l10n.put(langKey, langData);
        }
        // Global
        Map<String, String> global = new HashMap<>();
        int globalSize = in.readVarInt();
        for (int i = 0; i < globalSize; i++) {
            String key = in.readUtf();
            String value = in.readUtf();
            global.put(key, value);
        }

        FontData fontData = new FontData(serverName, offsetFont, images, l10n, global);
        this.serverFonts.put(serverName, fontData);
    }
}
