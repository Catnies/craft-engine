package net.momirealms.craftengine.proxy.bungeecord.platform;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.momirealms.craftengine.proxy.bungeecord.BungeeCord;
import net.momirealms.craftengine.proxy.common.platform.PlayerManager;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BungeePlayerManager implements PlayerManager {
    private final BungeeCord plugin;
    private final Map<UUID, BungeePlayer> players = new ConcurrentHashMap<>();

    public BungeePlayerManager(BungeeCord plugin) {
        this.plugin = plugin;
    }

    public BungeePlayer wrapper(ProxiedPlayer platform) {
        return this.players.computeIfAbsent(platform.getUniqueId(), uuid ->  new BungeePlayer(platform));
    }

    @Override
    public @Nullable BungeePlayer getOrWrapperPlayer(UUID uuid) {
        BungeePlayer player = this.players.get(uuid);
        if (player != null) {
            return player;
        }
        return Optional.ofNullable(this.plugin.getProxy().getPlayer(uuid))
                .map(this::wrapper)
                .orElse(null);
    }

    @Override
    public @Nullable BungeePlayer getPlayer(UUID uuid) {
        return this.players.get(uuid);
    }
}
