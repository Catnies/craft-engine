package net.momirealms.craftengine.proxy.bungeecord;

import net.md_5.bungee.api.plugin.Plugin;
import net.momirealms.craftengine.proxy.common.CraftEngineProxyPlugin;
import net.momirealms.craftengine.proxy.common.font.FontDataSyncService;
import net.momirealms.craftengine.proxy.bungeecord.font.BungeeFontDataBridge;

public class CraftEngineBungeeCordPlugin extends Plugin implements CraftEngineProxyPlugin {
    public static CraftEngineBungeeCordPlugin INSTANCE;
    private BungeeFontDataBridge bungeeFontDataBridge;

    @Override
    public void onEnable() {
        INSTANCE = this;
        this.bungeeFontDataBridge = new BungeeFontDataBridge(this);
        this.bungeeFontDataBridge.load();
    }

    @Override
    public void onDisable() {
        if (this.bungeeFontDataBridge != null) {
            this.bungeeFontDataBridge.disable();
        }
    }

    @Override
    public FontDataSyncService fontDataSyncService() {
        return this.bungeeFontDataBridge.fontDataSyncService();
    }
}
