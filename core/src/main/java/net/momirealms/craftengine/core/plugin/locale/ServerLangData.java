package net.momirealms.craftengine.core.plugin.locale;

import net.momirealms.craftengine.core.util.FriendlyByteBuf;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class ServerLangData {
    private final Map<Locale, String> translations = new HashMap<>();
    private final String fallback;

    public ServerLangData(String fallback) {
        this.fallback = fallback;
    }

    public ServerLangData() {
        this.fallback = null;
    }

    public void addTranslation(final Locale locale, final String translation) {
        this.translations.putIfAbsent(locale, translation);
    }

    public String translate(final Locale locale) {
        String translation = this.translations.get(locale);
        if (translation == null) {
            translation = this.translations.get(Locale.of(locale.getLanguage()));
            if (translation == null) {
                translation = this.fallback;
            }
        }
        return translation;
    }

    // Velocity
    public static ServerLangData read(FriendlyByteBuf buf) {
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

    public void write(FriendlyByteBuf buf) {
        // fallback
        buf.writeBoolean(this.fallback != null);
        if (this.fallback != null) {
            buf.writeUtf(this.fallback);
        }
        // translations
        buf.writeVarInt(this.translations.size());
        for (Map.Entry<Locale, String> entry : this.translations.entrySet()) {
            buf.writeUtf(entry.getKey().toLanguageTag());
            buf.writeUtf(entry.getValue());
        }
    }
}
