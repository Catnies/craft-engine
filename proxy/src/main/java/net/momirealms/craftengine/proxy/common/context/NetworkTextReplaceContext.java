package net.momirealms.craftengine.proxy.common.context;

import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.ContextHolder;
import net.momirealms.craftengine.core.plugin.context.ContextKey;
import net.momirealms.craftengine.core.util.ArrayUtils;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.proxy.common.platform.ProxyPlayer;
import net.momirealms.craftengine.proxy.common.font.FontData;
import net.momirealms.craftengine.proxy.common.text.minimessage.NetworkL10NTag;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Optional;

public final class NetworkTextReplaceContext<P> implements Context {
    public final ContextKey<ProxyPlayer> PLAYER = ContextKey.direct("proxy_player");
    private final ContextHolder contexts;
    private final ProxyPlayer player;
    private final TagResolver[] staticTagResolvers;
    private final FontData fontData;
    private TagResolver[] tagResolvers;

    public NetworkTextReplaceContext(ProxyPlayer player, FontData fontData) {
        this.contexts = ContextHolder.trustedMutable(MiscUtils.init(new HashMap<>(4), it -> {
            it.put(PLAYER, () -> player);
        }));
        this.player = player;
        this.fontData = fontData;
        this.staticTagResolvers = fontData.tagResolvers();
    }

    @NotNull
    public ProxyPlayer player() {
        return this.player;
    }

    @NotNull
    public FontData fontData() {
        return this.fontData;
    }

    @Override
    public ContextHolder contexts() {
        return this.contexts;
    }

    @Override
    public TagResolver[] tagResolvers() {
        if (this.tagResolvers == null) {
            this.tagResolvers = ArrayUtils.mergeNoCopy(
                    this.staticTagResolvers,
                    new TagResolver[] {
                            new NetworkL10NTag(this)
                    }
            );
        }
        return this.tagResolvers;
    }

    @Override
    public <T> Optional<T> getOptionalParameter(ContextKey<T> parameter) {
        return this.contexts.getOptional(parameter);
    }
}
