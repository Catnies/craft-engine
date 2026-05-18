package net.momirealms.craftengine.proxy.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import net.momirealms.craftengine.proxy.common.CraftEngineProxyPlugin;
import net.momirealms.craftengine.proxy.common.network.listener.PacketListenerManager;
import net.momirealms.craftengine.proxy.common.network.packet.ProxyPacketRegistry;
import net.momirealms.craftengine.proxy.common.tag.NetworkTagDataSyncService;
import net.momirealms.craftengine.proxy.common.util.AdventureHelper;
import net.momirealms.craftengine.proxy.velocity.network.VelocityPacketListenerManager;
import net.momirealms.craftengine.proxy.velocity.platform.VelocityPlayerManager;
import net.momirealms.craftengine.proxy.velocity.tag.VelocityNetworkTagDataBridge;
import org.slf4j.Logger;

import java.io.File;
import java.nio.file.Path;

@Plugin(
        id = "craftengine",
        name = "CraftEngine",
        version = "1.0.0-SNAPSHOT",
        authors = {"Catnies"}
)
public class CraftEngineVelocityPlugin implements CraftEngineProxyPlugin {
    public static CraftEngineVelocityPlugin INSTANCE;
    public final ProxyServer server;
    public final Logger logger;
    public final PluginContainer pluginContainer;
    public final Path dataDirectory;
    private VelocityPlayerManager playerManager;
    private VelocityPacketListenerManager packetListenerManager;
    private VelocityNetworkTagDataBridge networkTagDataBridge;

    @Inject
    public CraftEngineVelocityPlugin(ProxyServer server, Logger logger, PluginContainer pluginContainer, @DataDirectory Path dataDirectory) {
        INSTANCE = this;
        this.server = server;
        this.logger = logger;
        this.pluginContainer = pluginContainer;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        AdventureHelper.init();
        this.playerManager = new VelocityPlayerManager(this);
        this.packetListenerManager = new VelocityPacketListenerManager(this);
        this.networkTagDataBridge = new VelocityNetworkTagDataBridge(this);
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        if (this.playerManager != null) this.playerManager.disable();
        if (this.networkTagDataBridge != null) this.networkTagDataBridge.disable();
        if (this.packetListenerManager != null) this.packetListenerManager.disable();
        this.server.getEventManager().unregisterListeners(this);
    }

    @Override
    public File dataFolderFile() {
        return this.dataDirectory.toFile();
    }

    @Override
    public Path dataFolderPath() {
        return this.dataDirectory;
    }

    @Override
    public VelocityPlayerManager playerManager() {
        return this.playerManager;
    }

    @Override
    public VelocityPacketListenerManager packetListenerManager() {
        return this.packetListenerManager;
    }

    @Override
    public NetworkTagDataSyncService networkTagDataSyncService() {
        return this.networkTagDataBridge.networkTagDataSyncService();
    }
}
