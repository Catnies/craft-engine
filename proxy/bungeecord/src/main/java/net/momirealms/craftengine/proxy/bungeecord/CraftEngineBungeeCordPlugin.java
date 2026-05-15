package net.momirealms.craftengine.proxy.bungeecord;

import net.md_5.bungee.api.plugin.Plugin;
import net.momirealms.craftengine.proxy.common.CraftEngineProxyPlugin;
import net.momirealms.craftengine.proxy.common.font.NetwrokTagDataSyncService;
import net.momirealms.craftengine.proxy.bungeecord.font.BungeeNetworkTagDataBridge;
import net.momirealms.craftengine.proxy.common.util.AdventureHelper;

public class CraftEngineBungeeCordPlugin extends Plugin implements CraftEngineProxyPlugin {
    public static CraftEngineBungeeCordPlugin INSTANCE;
    private BungeeNetworkTagDataBridge bungeeNetworkTagDataBridge;

    @Override
    public void onEnable() {
        INSTANCE = this;
        AdventureHelper.init();
        this.bungeeNetworkTagDataBridge = new BungeeNetworkTagDataBridge(this);
        this.bungeeNetworkTagDataBridge.load();
    }

    @Override
    public void onDisable() {
        if (this.bungeeNetworkTagDataBridge != null) {
            this.bungeeNetworkTagDataBridge.disable();
        }
    }

    @Override
    public NetwrokTagDataSyncService networkTagDataSyncService() {
        return this.bungeeNetworkTagDataBridge.networkTagDataSyncService();
    }
}
