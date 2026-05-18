package net.momirealms.craftengine.proxy.common.platform;

import net.momirealms.craftengine.core.plugin.Manageable;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public interface PlayerManager extends Manageable {

    @Nullable
    ProxyPlayer getOrWrapperPlayer(UUID uuid);

    @Nullable
    ProxyPlayer getPlayer(UUID uuid);

}
