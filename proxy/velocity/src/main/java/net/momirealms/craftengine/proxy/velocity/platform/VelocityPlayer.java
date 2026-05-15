package net.momirealms.craftengine.proxy.velocity.platform;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import net.momirealms.craftengine.proxy.common.platform.ProxyPlayer;
import net.momirealms.craftengine.proxy.common.platform.BackendServer;

import java.util.Locale;
import java.util.UUID;

public class VelocityPlayer implements ProxyPlayer {
    private final Player platform;

    private VelocityPlayer(Player platform) {
        this.platform = platform;
    }

    public static VelocityPlayer wrapper(Player platform) {
        return new VelocityPlayer(platform);
    }

    @Override
    public UUID uuid() {
        return platform.getUniqueId();
    }

    @Override
    public BackendServer server() {
        return platform.getCurrentServer()
                .map(VelocityBackendServer::wrapper)
                .orElse(null);
    }

    @Override
    public boolean sendServerPluginMessage(String channel, byte[] data) {
        return platform.getCurrentServer()
                .map(it -> it.sendPluginMessage(MinecraftChannelIdentifier.from(channel), data))
                .orElse(false);
    }

    @Override
    public Locale locale() {
        return platform.getEffectiveLocale();
    }
}
