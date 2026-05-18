package net.momirealms.craftengine.proxy.common.network.packet;

import net.momirealms.craftengine.proxy.common.network.ProtocolStateHolder;
import net.momirealms.craftengine.proxy.common.platform.ProxyPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public class PacketHandlerChain {
    private volatile PacketHandler[] handlers = new PacketHandler[0];

    public void handle(ProtocolStateHolder connection, @Nullable ProxyPlayer player, PacketContext packet) {
        for (PacketHandler handler : this.handlers) {
            handler.handle(connection, player, packet);
            if (packet.isCancelled()) {
                return;
            }
        }
    }

    public synchronized void add(PacketHandler handler) {
        PacketHandler[] current = this.handlers;
        PacketHandler[] updated = Arrays.copyOf(current, current.length + 1);
        updated[current.length] = handler;
        this.handlers = updated;
    }

    public synchronized void remove(PacketHandler handler) {
        PacketHandler[] current = this.handlers;
        for (int i = 0; i < current.length; i++) {
            if (current[i] != handler) {
                continue;
            }
            PacketHandler[] updated = new PacketHandler[current.length - 1];
            System.arraycopy(current, 0, updated, 0, i);
            System.arraycopy(current, i + 1, updated, i, current.length - i - 1);
            this.handlers = updated;
            return;
        }
    }

    public int size() {
        return this.handlers.length;
    }

    public boolean isEmpty() {
        return this.size() == 0;
    }
}
