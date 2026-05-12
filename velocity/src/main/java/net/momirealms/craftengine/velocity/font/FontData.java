package net.momirealms.craftengine.velocity.font;

import net.momirealms.craftengine.core.font.Image;
import net.momirealms.craftengine.core.font.OffsetFont;
import net.momirealms.craftengine.core.plugin.locale.ServerLangData;
import net.momirealms.craftengine.core.util.Key;

import java.util.Collections;
import java.util.Map;

public class FontData {
    public final String serverName;
    private OffsetFont offset;
    private Map<Key, Image> images;
    private Map<String, ServerLangData> l10n;
    private Map<String, String> global;
    private boolean dirty;

    public FontData(
            String serverName,
            OffsetFont offset,
            Map<Key, Image> images,
            Map<String, ServerLangData> l10n,
            Map<String, String> global
    ) {
        this.serverName = serverName;
        this.offset = offset;
        this.images = images;
        this.l10n = l10n;
        this.global = global;
    }

    public OffsetFont offset() {
        return this.offset;
    }

    public Map<Key, Image> images() {
        return Collections.unmodifiableMap(images);
    }

    public Map<String, String> global() {
        return Collections.unmodifiableMap(global);
    }

    public Map<String, ServerLangData> l10n() {
        return Collections.unmodifiableMap(l10n);
    }

    public boolean dirty() {
        return dirty;
    }

    public void dirty(boolean dirty) {
        this.dirty = dirty;
    }
}
