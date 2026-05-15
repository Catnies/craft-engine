package net.momirealms.craftengine.proxy.common.font;

import net.momirealms.craftengine.core.font.BitmapImage;
import net.momirealms.craftengine.core.font.DummyImage;
import net.momirealms.craftengine.core.font.Image;
import net.momirealms.craftengine.core.font.OffsetFont;
import net.momirealms.craftengine.core.font.ReferenceImage;
import net.momirealms.craftengine.core.plugin.locale.ServerLangData;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.LazyReference;

import java.util.HashMap;
import java.util.Map;

public final class NetworkTagDataCodec {
    private final NetworkTagDataRegistry registry;

    public NetworkTagDataCodec(NetworkTagDataRegistry registry) {
        this.registry = registry;
    }

    public NetworkTagData read(String serverName, FriendlyByteBuf in) {
        long version = in.readLong();
        OffsetFont offsetFont = new OffsetFont(in);

        Map<Key, Image> images = new HashMap<>();
        int imageSize = in.readVarInt();
        for (int i = 0; i < imageSize; i++) {
            Key key = in.readKey();
            byte type = in.readByte();
            Image image = null;
            if (type == 0) {
                image = BitmapImage.read(in);
            } else if (type == 1) {
                Key refId = in.readKey();
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

        Map<String, ServerLangData> l10n = new HashMap<>();
        int l10nSize = in.readVarInt();
        for (int i = 0; i < l10nSize; i++) {
            String langKey = in.readUtf();
            ServerLangData langData = ServerLangData.read(in);
            l10n.put(langKey, langData);
        }

        Map<String, String> global = new HashMap<>();
        int globalSize = in.readVarInt();
        for (int i = 0; i < globalSize; i++) {
            String key = in.readUtf();
            String value = in.readUtf();
            global.put(key, value);
        }

        return new NetworkTagData(serverName, version, offsetFont, images, l10n, global);
    }

    public void write(NetworkTagData netWorkTagData, FriendlyByteBuf out) {
        out.writeLong(netWorkTagData.version());
        netWorkTagData.offset().write(out);

        out.writeVarInt(netWorkTagData.images().size());
        for (Map.Entry<Key, Image> entry : netWorkTagData.images().entrySet()) {
            out.writeKey(entry.getKey());
            Image image = entry.getValue();
            if (image instanceof BitmapImage bitmapImage) {
                out.writeByte(0);
                bitmapImage.write(out);
            } else if (image instanceof ReferenceImage referenceImage) {
                out.writeByte(1);
                referenceImage.write(out);
            } else {
                throw new IllegalArgumentException("Unsupported image type " + image.getClass().getName());
            }
        }

        out.writeVarInt(netWorkTagData.l10n().size());
        for (Map.Entry<String, ServerLangData> entry : netWorkTagData.l10n().entrySet()) {
            out.writeUtf(entry.getKey());
            entry.getValue().write(out);
        }

        out.writeVarInt(netWorkTagData.global().size());
        for (Map.Entry<String, String> entry : netWorkTagData.global().entrySet()) {
            out.writeUtf(entry.getKey());
            out.writeUtf(entry.getValue());
        }
    }
}
