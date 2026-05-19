package net.momirealms.craftengine.proxy.velocity.platform;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.proxy.common.network.protocol.ConnectionState;
import net.momirealms.craftengine.proxy.common.network.protocol.player.ClientVersion;
import net.momirealms.craftengine.proxy.common.platform.BackendServer;
import net.momirealms.craftengine.proxy.common.platform.ProxyPlayer;
import net.momirealms.craftengine.proxy.velocity.VelocityCraftEngine;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public class VelocityPlayer implements ProxyPlayer {
    private volatile Player platform;
    private volatile int protocolVersion = -1;
    private ClientVersion clientVersion = ClientVersion.UNKNOWN;
    private ConnectionState decoderState = ConnectionState.HANDSHAKING;
    private ConnectionState encoderState = ConnectionState.HANDSHAKING;

    public VelocityPlayer(Player platform) {
        this.platform = platform;
        this.setProtocolVersion(platform.getProtocolVersion().getProtocol());
    }

    public static VelocityPlayer wrap(Player platform) {
        return VelocityCraftEngine.INSTANCE.wrap(platform);
    }

    @Override
    public UUID uuid() {
        return this.platform.getUniqueId();
    }

    @Override
    public Object platform() {
        return platform;
    }

    @Override
    public BackendServer server() {
        return this.platform.getCurrentServer()
                .map(ServerConnection::getServer)
                .map(VelocityBackendServer::wrapper)
                .orElse(null);
    }

    @Override
    public boolean sendServerPluginMessage(String channel, byte[] data) {
        return this.platform.getCurrentServer()
                .map(it -> it.sendPluginMessage(MinecraftChannelIdentifier.from(channel), data))
                .orElse(false);
    }

    @Override
    public Locale locale() {
        return this.platform.getEffectiveLocale();
    }

    @Override
    public void kick(String reason) {
        this.platform.disconnect(Component.text(reason));
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
