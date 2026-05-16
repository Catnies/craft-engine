package net.momirealms.craftengine.proxy.velocity;

import com.github.retrooper.packetevents.PacketEvents;
import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import io.github.retrooper.packetevents.velocity.factory.VelocityPacketEventsBuilder;
import net.momirealms.craftengine.proxy.common.CraftEngineProxyPlugin;
import net.momirealms.craftengine.proxy.common.font.NetworkTagDataSyncService;
import net.momirealms.craftengine.proxy.common.util.AdventureHelper;
import net.momirealms.craftengine.proxy.velocity.font.VelocityNetworkTagDataBridge;
import org.slf4j.Logger;

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
    private VelocityNetworkTagDataBridge velocityNetworkTagDataBridge;

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
        PacketEvents.setAPI(VelocityPacketEventsBuilder.build(this.server, this.pluginContainer, this.logger, this.dataDirectory));
        PacketEvents.getAPI().load();
        AdventureHelper.init();
        this.velocityNetworkTagDataBridge = new VelocityNetworkTagDataBridge(this);
        this.velocityNetworkTagDataBridge.load();
        PacketEvents.getAPI().init();
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        if (this.velocityNetworkTagDataBridge != null) {
            this.velocityNetworkTagDataBridge.disable();
        }
        this.server.getEventManager().unregisterListeners(this);
    }

    @Override
    public NetworkTagDataSyncService networkTagDataSyncService() {
        return this.velocityNetworkTagDataBridge.networkTagDataSyncService();
    }
}
