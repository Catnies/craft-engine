package net.momirealms.craftengine.proxy.common.platform;

import net.momirealms.craftengine.proxy.common.network.ProtocolStateHolder;

import java.util.Locale;
import java.util.UUID;

public interface ProxyPlayer extends ProtocolStateHolder {

    UUID uuid();

    Object platform();

    BackendServer server();

    boolean sendServerPluginMessage(String channel, byte[] data);

    Locale locale();
}
