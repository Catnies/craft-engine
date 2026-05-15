package net.momirealms.craftengine.proxy.bungeecord.platform;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.momirealms.craftengine.proxy.common.platform.ProxyPlayer;
import net.momirealms.craftengine.proxy.common.platform.BackendServer;

import java.util.Locale;
import java.util.UUID;

public class BungeePlayer implements ProxyPlayer {
    private final ProxiedPlayer platform;

    private BungeePlayer(ProxiedPlayer platform) {
        this.platform = platform;
    }

    public static BungeePlayer wrapper(ProxiedPlayer platform) {
        return new BungeePlayer(platform);
    }

    @Override
    public UUID uuid() {
        return platform.getUniqueId();
    }

    @Override
    public BackendServer server() {
        Server server = platform.getServer();
        return server != null ? BungeeBackendServer.wrapper(server) : null;
    }

    @Override
    public boolean sendServerPluginMessage(String channel, byte[] data) {
        Server server = platform.getServer();
        if (server != null) {
            server.sendData(channel, data);
            return true;
        }
        return false;
    }

    @Override
    public Locale locale() {
        return platform.getLocale();
    }
}
