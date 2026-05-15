package net.momirealms.craftengine.proxy.common.font;

import net.kyori.adventure.text.minimessage.internal.parser.Token;
import net.kyori.adventure.text.minimessage.internal.parser.TokenParser;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.momirealms.craftengine.core.font.Image;
import net.momirealms.craftengine.core.font.OffsetFont;
import net.momirealms.craftengine.core.plugin.locale.ServerLangData;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.proxy.common.text.component.ComponentProvider;
import net.momirealms.craftengine.proxy.common.text.minimessage.GlobalVariableTag;
import net.momirealms.craftengine.proxy.common.text.minimessage.ImageTag;
import net.momirealms.craftengine.proxy.common.text.minimessage.ShiftTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class NetworkTagData {
    public static final Set<String> NETWORK_TAGS = Set.of("image", "l10n", "shift", "global");
    public static final Locale SYSTEM_LOCALE = Locale.getDefault();

    private final String serverName;
    private final long version;
    private final OffsetFont offset;
    private final Map<Key, Image> images;
    private final Map<String, Image> imageByIdValue;
    private final Map<String, ServerLangData> l10n;
    private final Map<String, String> globalVariables;
    private final TagResolver[] tagResolvers;
    private final Map<String, ComponentProvider> networkTagMapper;

    public NetworkTagData(
            String serverName,
            long version,
            OffsetFont offset,
            Map<Key, Image> images,
            Map<String, ServerLangData> l10n,
            Map<String, String> globalVariables
    ) {
        this.serverName = serverName;
        this.version = version;
        this.offset = offset;
        this.images = images;
        this.imageByIdValue = images.entrySet()
                .stream()
                .collect(Collectors.toMap(
                        entry -> entry.getKey().value,
                        Map.Entry::getValue
                ));
        this.l10n = l10n;
        this.globalVariables = globalVariables;
        this.tagResolvers = new TagResolver[] {
                new ShiftTag(this),
                new ImageTag(this),
                new GlobalVariableTag(this)
        };
        this.networkTagMapper = MiscUtils.init(new HashMap<>(), it -> {
            // TODO 缓存
        });
    }

    @SuppressWarnings("UnstableApiUsage")
    public Map<String, ComponentProvider> matchNetworkTags(String text) {
        Map<String, ComponentProvider> tags = new HashMap<>();
        List<Token> root = TokenParser.tokenize(text, true);
        for (final Token token : root) {
            switch (token.type()) {
                case TEXT: break;
                case OPEN_TAG:
                case CLOSE_TAG:
                case OPEN_CLOSE_TAG:
                    if (token.childTokens().isEmpty()) {
                        continue;
                    }
                    final String sanitized = TokenParser.TagProvider.sanitizePlaceholderName(token.childTokens().getFirst().get(text).toString());
                    if (NETWORK_TAGS.contains(sanitized)) {
                        String tag = text.substring(token.startIndex(), token.endIndex());
                        tags.computeIfAbsent(tag, k -> Optional
                                .ofNullable(this.networkTagMapper.get(k))
                                .orElse(ComponentProvider.miniMessage(k))
                        );
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported token type " + token.type());
            }
        }
        return tags;
    }

    @Nullable
    public Image imageById(Key id) {
        return this.images.get(id);
    }

    @Nullable
    public Image imageByIdValue(String id) {
        return this.imageByIdValue.get(id);
    }

    @Nullable
    public String getGlobalVariable(String key) {
        return this.globalVariables.get(key);
    }

    @Nullable
    public ServerLangData getServerLangData(String key) {
        return this.l10n.get(key);
    }

    @NotNull
    public String miniMessageTranslation(String key, @Nullable Locale locale) {
        ServerLangData serverLangData = this.getServerLangData(key);
        if (serverLangData == null) {
            return key;
        }
        if (locale == null) {
            locale = SYSTEM_LOCALE;
        }
        return Optional.ofNullable(serverLangData.translate(locale)).orElse(key);
    }

    public String serverName() {
        return this.serverName;
    }

    public long version() {
        return this.version;
    }

    public OffsetFont offset() {
        return this.offset;
    }

    public Map<Key, Image> images() {
        return Collections.unmodifiableMap(images);
    }

    public Map<String, String> global() {
        return Collections.unmodifiableMap(globalVariables);
    }

    public Map<String, ServerLangData> l10n() {
        return Collections.unmodifiableMap(l10n);
    }

    public TagResolver[] tagResolvers() {
        return this.tagResolvers;
    }
}
