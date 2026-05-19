package net.momirealms.craftengine.proxy.bungeecord.platform;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.momirealms.craftengine.proxy.bungeecord.BungeeCord;
import net.momirealms.craftengine.proxy.common.network.protocol.ConnectionState;
import net.momirealms.craftengine.proxy.common.network.protocol.player.ClientVersion;
import net.momirealms.craftengine.proxy.common.platform.BackendServer;
import net.momirealms.craftengine.proxy.common.platform.ProxyPlayer;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public class BungeePlayer implements ProxyPlayer {
    private volatile ProxiedPlayer platform;
    private volatile int protocolVersion = -1;
    private ClientVersion clientVersion = ClientVersion.UNKNOWN;
    private ConnectionState decoderState = ConnectionState.HANDSHAKING;
    private ConnectionState encoderState = ConnectionState.HANDSHAKING;

    public BungeePlayer(ProxiedPlayer platform) {
        this.platform = platform;
        this.setProtocolVersion(platform.getPendingConnection().getVersion());
    }

    public static BungeePlayer wrap(ProxiedPlayer platform) {
        return BungeeCord.INSTANCE.playerManager().wrapper(platform);
    }

    @Override
    public UUID uuid() {
        return platform.getUniqueId();
    }

    @Override
    public Object platform() {
        return this.platform;
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

    @Override
    public ClientVersion clientVersion() {
        return this.clientVersion;
    }

    @Override
    public int protocolVersion() {
        return this.protocolVersion;
    }

    @Override
    public void setProtocolVersion(int protocolVersion) {
        this.protocolVersion = protocolVersion;
        this.clientVersion = protocolVersion < 0 ? ClientVersion.UNKNOWN : ClientVersion.getById(protocolVersion);
    }

    @Override
    public void setConnectionState(ConnectionState connectionState) {
        ConnectionState state = Objects.requireNonNull(connectionState, "connectionState");
        this.decoderState = state;
        this.encoderState = state;
    }

    @Override
    public ConnectionState decoderState() {
        return this.decoderState;
    }

    @Override
    public ConnectionState encoderState() {
        return this.encoderState;
    }

    @Override
    public void setDecoderState(ConnectionState decoderState) {
        this.decoderState = Objects.requireNonNull(decoderState, "decoderState");
    }

    @Override
    public void setEncoderState(ConnectionState encoderState) {
        this.encoderState = Objects.requireNonNull(encoderState, "encoderState");
    }
}
