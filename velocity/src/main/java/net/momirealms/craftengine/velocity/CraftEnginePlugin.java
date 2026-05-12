package net.momirealms.craftengine.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import net.momirealms.craftengine.velocity.font.FontDataManager;
import org.slf4j.Logger;

import java.nio.file.Path;

@Plugin(id = "craftengine", name = "CraftEngine", version = "1.0.0-SNAPSHOT", authors = {"XiaoMomi"})
public class CraftEnginePlugin {
    public final ProxyServer server;
    public final Logger logger;
    public final PluginContainer pluginContainer;
    public final Path dataDirectory;
    private FontDataManager fontDataManager;

    @Inject
    public CraftEnginePlugin(ProxyServer server, Logger logger, PluginContainer pluginContainer, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.pluginContainer = pluginContainer;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        this.fontDataManager = new FontDataManager(this);
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        this.server.getEventManager().unregisterListeners(this);
    }
}