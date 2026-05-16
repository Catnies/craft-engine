package net.momirealms.craftengine.proxy.bungeecord;

import com.github.retrooper.packetevents.PacketEvents;
import io.github.retrooper.packetevents.bungee.factory.BungeePacketEventsBuilder;
import net.md_5.bungee.api.plugin.Plugin;
import net.momirealms.craftengine.proxy.common.CraftEngineProxyPlugin;
import net.momirealms.craftengine.proxy.common.font.NetworkTagDataSyncService;
import net.momirealms.craftengine.proxy.bungeecord.font.BungeeNetworkTagDataBridge;
import net.momirealms.craftengine.proxy.common.util.AdventureHelper;

public class CraftEngineBungeeCordPlugin extends Plugin implements CraftEngineProxyPlugin {
    public static CraftEngineBungeeCordPlugin INSTANCE;
    private BungeeNetworkTagDataBridge bungeeNetworkTagDataBridge;

    @Override
    public void onEnable() {
        INSTANCE = this;
        PacketEvents.setAPI(BungeePacketEventsBuilder.build(this));
        PacketEvents.getAPI().load();
        AdventureHelper.init();
        this.bungeeNetworkTagDataBridge = new BungeeNetworkTagDataBridge(this);
        this.bungeeNetworkTagDataBridge.load();
        PacketEvents.getAPI().init();
    }

    @Override
    public void onDisable() {
        if (this.bungeeNetworkTagDataBridge != null) {
            this.bungeeNetworkTagDataBridge.disable();
        }
    }

    @Override
    public NetworkTagDataSyncService networkTagDataSyncService() {
        return this.bungeeNetworkTagDataBridge.networkTagDataSyncService();
    }
}
