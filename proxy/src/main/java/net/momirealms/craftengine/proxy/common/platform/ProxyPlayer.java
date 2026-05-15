package net.momirealms.craftengine.proxy.common.platform;

import java.util.Locale;
import java.util.UUID;

public interface ProxyPlayer {

    UUID uuid();

    BackendServer server();

    boolean sendServerPluginMessage(String channel, byte[] data);

    Locale locale();

}
