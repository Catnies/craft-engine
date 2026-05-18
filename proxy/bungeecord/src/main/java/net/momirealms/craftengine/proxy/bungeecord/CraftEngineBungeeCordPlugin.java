package net.momirealms.craftengine.proxy.bungeecord;

import net.md_5.bungee.api.plugin.Plugin;
import net.momirealms.craftengine.proxy.bungeecord.network.BungeePacketListenerManager;
import net.momirealms.craftengine.proxy.bungeecord.platform.BungeePlayerManager;
import net.momirealms.craftengine.proxy.bungeecord.tag.BungeeNetworkTagDataBridge;
import net.momirealms.craftengine.proxy.common.CraftEngineProxyPlugin;
import net.momirealms.craftengine.proxy.common.network.listener.PacketListenerManager;
import net.momirealms.craftengine.proxy.common.network.packet.ProxyPacketRegistry;
import net.momirealms.craftengine.proxy.common.tag.NetworkTagDataSyncService;
import net.momirealms.craftengine.proxy.common.util.AdventureHelper;

import java.io.File;
import java.nio.file.Path;

public class CraftEngineBungeeCordPlugin extends Plugin implements CraftEngineProxyPlugin {
    public static CraftEngineBungeeCordPlugin INSTANCE;
    private BungeePlayerManager playerManager;
    private BungeePacketListenerManager packetListenerManager;
    private BungeeNetworkTagDataBridge bungeeNetworkTagDataBridge;

    @Override
    public void onEnable() {
        INSTANCE = this;
        AdventureHelper.init();
        this.playerManager = new BungeePlayerManager(this);
        this.packetListenerManager = new BungeePacketListenerManager(this);
        this.bungeeNetworkTagDataBridge = new BungeeNetworkTagDataBridge(this);
    }

    @Override
    public void onDisable() {
        if (this.bungeeNetworkTagDataBridge != null) {
            this.bungeeNetworkTagDataBridge.disable();
        }
    }

    @Override
    public File dataFolderFile() {
        return this.getDataFolder();
    }

    @Override
    public Path dataFolderPath() {
        return this.getDataFolder().toPath();
    }

    @Override
    public BungeePlayerManager playerManager() {
        return this.playerManager;
    }

    @Override
    public PacketListenerManager packetListenerManager() {
        return this.packetListenerManager;
    }

    @Override
    public NetworkTagDataSyncService networkTagDataSyncService() {
        return this.bungeeNetworkTagDataBridge.networkTagDataSyncService();
    }
}
