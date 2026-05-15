package net.momirealms.craftengine.proxy.velocity.platform;

import com.velocitypowered.api.proxy.ServerConnection;
import net.momirealms.craftengine.proxy.common.platform.BackendServer;

public class VelocityBackendServer implements BackendServer {
    private final ServerConnection platform;

    private VelocityBackendServer(ServerConnection platform) {
        this.platform = platform;
    }

    public static VelocityBackendServer wrapper(ServerConnection platform) {
        return new VelocityBackendServer(platform);
    }

    @Override
    public String name() {
        return this.platform.getServerInfo().getName();
    }
}
