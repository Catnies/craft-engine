package net.momirealms.craftengine.proxy.velocity.platform;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.proxy.Player;
import net.momirealms.craftengine.proxy.common.platform.PlayerManager;
import net.momirealms.craftengine.proxy.velocity.Velocity;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class VelocityPlayerManager implements PlayerManager {
    private final Velocity plugin;
    private final Map<UUID, VelocityPlayer> players = new ConcurrentHashMap<>();

    public VelocityPlayerManager(Velocity plugin) {
        this.plugin = plugin;
        this.load();
    }

    @Override
    public void load() {
        this.plugin.server.getEventManager().register(this.plugin, this);
    }

    @Override
    public void disable() {
        this.plugin.server.getEventManager().unregisterListener(this.plugin, this);
        this.players.clear();
    }

    public VelocityPlayer wrapper(Player platform) {
        return this.players.computeIfAbsent(platform.getUniqueId(), uuid ->  new VelocityPlayer(platform));
    }

    @Override
    public @Nullable VelocityPlayer getOrWrapperPlayer(UUID uuid) {
        VelocityPlayer player = this.players.get(uuid);
        if (player != null) {
            return player;
        }
        return this.plugin.server.getPlayer(uuid)
                .map(this::wrapper)
                .orElse(null);
    }

    @Override
    public @Nullable VelocityPlayer getPlayer(UUID uuid) {
        return this.players.get(uuid);
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        this.players.remove(event.getPlayer().getUniqueId());
    }
}
