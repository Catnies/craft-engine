package net.momirealms.craftengine.proxy.common.tag;

import net.momirealms.craftengine.core.font.*;
import net.momirealms.craftengine.core.plugin.locale.ServerLangData;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.LazyReference;
import net.momirealms.craftengine.proxy.common.util.ProxyByteBuf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class NetworkTagDataCodec {
    private final NetworkTagDataRegistry registry;

    public NetworkTagDataCodec(NetworkTagDataRegistry registry) {
        this.registry = registry;
    }

    public NetworkTagData read(String serverName, ProxyByteBuf buf) {
        long version = buf.readLong();
        OffsetFont offsetFont = this.readOffsetFont(buf);
        Map<Key, Image> images = this.readImage(buf, serverName);
        Map<String, ServerLangData> l10n = this.readL10n(buf);
        Map<String, String> global = this.readGlobal(buf);
        return new NetworkTagData(serverName, version, offsetFont, images, l10n, global);
    }

    private OffsetFont readOffsetFont(ProxyByteBuf buf) {
        Key font = buf.readKey();

        String[] negativeOffsets = new String[16];
        for (int i = 1; i <= 15; i++) {
            negativeOffsets[i] = buf.readUtf();
        }
        String NEG_16 = buf.readUtf();
        String NEG_24 = buf.readUtf();
        String NEG_32 = buf.readUtf();
        String NEG_48 = buf.readUtf();
        String NEG_64 = buf.readUtf();
        String NEG_128 = buf.readUtf();
        String NEG_256 = buf.readUtf();

        String[] positiveOffsets = new String[16];
        for (int i = 1; i <= 15; i++) {
            positiveOffsets[i] = buf.readUtf();
        }
        String POS_16 = buf.readUtf();
        String POS_24 = buf.readUtf();
        String POS_32 = buf.readUtf();
        String POS_48 = buf.readUtf();
        String POS_64 = buf.readUtf();
        String POS_128 = buf.readUtf();
        String POS_256 = buf.readUtf();

        return new OffsetFont(
                font,
                NEG_16, NEG_24, NEG_32, NEG_48, NEG_64, NEG_128, NEG_256,
                POS_16, POS_24, POS_32, POS_48, POS_64, POS_128, POS_256,
                negativeOffsets, positiveOffsets
        );
    }

    private Map<Key, Image> readImage(ProxyByteBuf buf, String serverName) {
        Map<Key, Image> images = new HashMap<>();
        int imageSize = buf.readVarInt();
        for (int i = 0; i < imageSize; i++) {
            Key key = buf.readKey();
            byte type = buf.readByte();
            Image image = null;
            // BitmapImage
            if (type == 0) {
                Key id = buf.readKey();
                Key font = buf.readKey();
                int[][] codepointGrid = buf.readCollection(
                                value -> new ArrayList<>(),
                                buf1 -> buf1.readCollection(
                                        value -> new ArrayList<>(),
                                        ProxyByteBuf::readVarInt
                                )
                        ).stream()
                        .map(list -> list.stream()
                                .mapToInt(Integer::intValue)
                                .toArray())
                        .toArray(int[][]::new);
                image = new BitmapImage(id, font, 0, 0, "", codepointGrid);
            }
            // ReferenceImage
            else if (type == 1) {
                Key refId = buf.readKey();
                image = new ReferenceImage(LazyReference.lazyReference(() -> {
                    NetworkTagData netWorkTagData = this.registry.get(serverName);
                    if (netWorkTagData != null) {
                        Image img = netWorkTagData.images().get(refId);
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
        return images;
    }

    private Map<String, ServerLangData> readL10n(ProxyByteBuf buf) {
        Map<String, ServerLangData> l10n = new HashMap<>();
        int l10nSize = buf.readVarInt();
        for (int i = 0; i < l10nSize; i++) {
            String langKey = buf.readUtf();
            ServerLangData langData = this.readServerLangData(buf);
            l10n.put(langKey, langData);
        }
        return l10n;
    }

    private ServerLangData readServerLangData(ProxyByteBuf buf) {
        String fallback = buf.readBoolean() ? buf.readUtf() : null;
        ServerLangData serverLangData = new ServerLangData(fallback);
        // 读取
        int size = buf.readVarInt();
        for (int i = 0; i < size; i++) {
            Locale locale = Locale.forLanguageTag(buf.readUtf());
            String translation = buf.readUtf();
            serverLangData.addTranslation(locale, translation);
        }
        return serverLangData;
    }

    private Map<String, String> readGlobal(ProxyByteBuf buf) {
        Map<String, String> global = new HashMap<>();
        int globalSize = buf.readVarInt();
        for (int i = 0; i < globalSize; i++) {
            String key = buf.readUtf();
            String value = buf.readUtf();
            global.put(key, value);
        }
        return global;
    }
}
