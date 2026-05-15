package net.momirealms.craftengine.proxy.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import net.momirealms.craftengine.proxy.common.CraftEngineProxyPlugin;
import net.momirealms.craftengine.proxy.common.font.FontDataSyncService;
import net.momirealms.craftengine.proxy.common.util.AdventureHelper;
import net.momirealms.craftengine.proxy.velocity.font.VelocityFontDataBridge;
import org.slf4j.Logger;

import java.nio.file.Path;

@Plugin(
        id = "craftengine",
        name = "CraftEngine",
        version = "1.0.0-SNAPSHOT",
        authors = {"Catnies"},
        dependencies = {
                @Dependency(id = "packetevents")
        }
)
public class CraftEngineVelocityPlugin implements CraftEngineProxyPlugin {
    public static CraftEngineVelocityPlugin INSTANCE;
    public final ProxyServer server;
    public final Logger logger;
    public final PluginContainer pluginContainer;
    public final Path dataDirectory;
    private VelocityFontDataBridge velocityFontDataBridge;

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
        this.velocityFontDataBridge = new VelocityFontDataBridge(this);
        this.velocityFontDataBridge.load();
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        if (this.velocityFontDataBridge != null) {
            this.velocityFontDataBridge.disable();
        }
        this.server.getEventManager().unregisterListeners(this);
    }

    @Override
    public FontDataSyncService fontDataSyncService() {
        return this.velocityFontDataBridge.fontDataSyncService();
    }
}
