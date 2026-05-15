package net.momirealms.craftengine.proxy.common.font;

import net.momirealms.craftengine.proxy.common.platform.BackendServer;
import net.momirealms.craftengine.proxy.common.platform.ProxyPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class FontDataRegistry {
    private final Map<String, FontData> serverFonts = new ConcurrentHashMap<>();

    @Nullable
    public FontData get(String serverName) {
        return this.serverFonts.get(serverName);
    }

    @Nullable
    public FontData getForPlayer(ProxyPlayer player) {
        return Optional.ofNullable(player.server())
                .map(BackendServer::name)
                .map(this::get)
                .orElse(null);
    }

    public void put(String serverName, FontData fontData) {
        this.serverFonts.put(serverName, fontData);
    }

    public void clear() {
        this.serverFonts.clear();
    }
}
