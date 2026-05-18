package net.momirealms.craftengine.proxy.common;

import net.momirealms.craftengine.proxy.common.network.listener.PacketListenerManager;
import net.momirealms.craftengine.proxy.common.platform.PlayerManager;
import net.momirealms.craftengine.proxy.common.tag.NetworkTagDataSyncService;

import java.io.File;
import java.nio.file.Path;

public interface CraftEngineProxyPlugin {

    File dataFolderFile();

    Path dataFolderPath();

    PlayerManager playerManager();

    PacketListenerManager packetListenerManager();

    NetworkTagDataSyncService networkTagDataSyncService();

}
