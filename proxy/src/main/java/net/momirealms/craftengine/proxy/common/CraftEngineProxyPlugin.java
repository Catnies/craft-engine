package net.momirealms.craftengine.proxy.common;

import net.momirealms.craftengine.proxy.common.font.NetworkTagDataSyncService;

import java.io.File;
import java.nio.file.Path;

public interface CraftEngineProxyPlugin {

    File dataFolderFile();

    Path dataFolderPath();

    NetworkTagDataSyncService networkTagDataSyncService();

}
