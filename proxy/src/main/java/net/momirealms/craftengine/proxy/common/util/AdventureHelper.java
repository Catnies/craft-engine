package net.momirealms.craftengine.proxy.common.util;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.proxy.common.text.component.ComponentProvider;
import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.field.matcher.FieldMatcher;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class AdventureHelper {
    private static final Cache<String, Pattern> PATTERN_CACHE = Caffeine.newBuilder()
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .build();
    private final MiniMessage miniMessage;
    private final MiniMessage miniMessageStrict;
    private final MiniMessage miniMessageCustom;

    private AdventureHelper() {
        this.miniMessage = MiniMessage.builder().build();
        this.miniMessageStrict = MiniMessage.builder().strict(true).build();
        this.miniMessageCustom = MiniMessage.builder().tags(TagResolver.empty()).build();
    }

    static {
        SparrowClass.of(SparrowClass.findNoRemap("net.kyori.adventure.text.TextComponentImpl")).getDeclaredSparrowField(FieldMatcher.named("WARN_WHEN_LEGACY_FORMATTING_DETECTED")).mh().set(null, false);
        SparrowClass.of(SparrowClass.findNoRemap("net{}kyori{}adventure{}text{}TextComponentImpl".replace("{}", "."))).getDeclaredSparrowField(FieldMatcher.named("WARN_WHEN_LEGACY_FORMATTING_DETECTED")).mh().set(null, false);
    }

    public static void init() {}

    public static Component replaceText(Component text, Map<String, ComponentProvider> replacements, Context context) {
        int size = replacements.size();
        if (size == 0) return text;
        if (size == 1) {
            return text.replaceText(builder ->
                    builder.matchLiteral(replacements.keySet().iterator().next())
                            .replacement((result, b) ->
                                    Optional.ofNullable(replacements.get(result.group())).orElseThrow(() -> new IllegalStateException("Could not find tag '" + result.group() + "'")).apply(context)
                            )
            );
        } else {
            String patternString = replacements.keySet().stream()
                    .map(Pattern::quote)
                    .collect(Collectors.joining("|"));
            return text.replaceText(builder ->
                    builder.match(Objects.requireNonNull(PATTERN_CACHE.get(patternString, Pattern::compile)))
                            .replacement((result, b) ->
                                    Optional.ofNullable(replacements.get(result.group())).orElseThrow(() -> new IllegalStateException("Could not find tag '" + result.group() + "'")).apply(context)
                            )
            );
        }
    }

    private static class SingletonHolder {
        private static final AdventureHelper INSTANCE = new AdventureHelper();
    }

    public static AdventureHelper getInstance() {
        return SingletonHolder.INSTANCE;
    }

    public static MiniMessage miniMessage() {
        return getInstance().miniMessage;
    }

    public static MiniMessage customMiniMessage() {
        return getInstance().miniMessageCustom;
    }

    public static MiniMessage strictMiniMessage() {
        return getInstance().miniMessageStrict;
    }
}
