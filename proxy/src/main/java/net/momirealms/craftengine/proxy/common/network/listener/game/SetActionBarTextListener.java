package net.momirealms.craftengine.proxy.common.network.listener.game;

import net.momirealms.craftengine.proxy.common.ProxyCraftEngine;
import net.momirealms.craftengine.proxy.common.context.NetworkTextReplaceContext;
import net.momirealms.craftengine.proxy.common.network.ProtocolStateHolder;
import net.momirealms.craftengine.proxy.common.network.packet.PacketContext;
import net.momirealms.craftengine.proxy.common.network.packet.PacketHandler;
import net.momirealms.craftengine.proxy.common.network.protocol.player.ClientVersion;
import net.momirealms.craftengine.proxy.common.platform.ProxyPlayer;
import net.momirealms.craftengine.proxy.common.tag.NetworkTagData;
import net.momirealms.craftengine.proxy.common.text.component.ComponentProvider;
import net.momirealms.craftengine.proxy.common.util.AdventureHelper;
import net.momirealms.craftengine.proxy.common.util.ProxyByteBuf;
import net.momirealms.sparrow.nbt.Tag;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class SetActionBarTextListener implements PacketHandler {
    private final ProxyCraftEngine plugin;

    public SetActionBarTextListener(ProxyCraftEngine plugin) {
        this.plugin = plugin;
    }

    @Override
    public void handle(ProtocolStateHolder connection, @Nullable ProxyPlayer player, PacketContext packet) {
        // 检查是否存在玩家当前服务器的数据
        if (player == null) return;
        NetworkTagData netWorkTagData = this.plugin.networkTagDataSyncService().getTagDataForPlayer(player);
        if (netWorkTagData == null) return;

        // 读取数据
        ClientVersion clientVersion = packet.clientVersion();
        ProxyByteBuf buf = packet.payload();

        // 1.20.3 +
        if (clientVersion.isNewerThanOrEquals(ClientVersion.V_1_20_3)) {
            Tag nbt = buf.readNbt(false);
            if (nbt == null) return;
            Map<String, ComponentProvider> tokens = netWorkTagData.matchNetworkTags(nbt);
            if (tokens.isEmpty()) return;

            NetworkTextReplaceContext context = new NetworkTextReplaceContext(player, netWorkTagData);
            Tag tag = AdventureHelper.componentToTag(clientVersion, AdventureHelper.replaceText(AdventureHelper.tagToComponent(clientVersion, nbt), tokens, context));
            packet.rewritePayload(replaceBuf -> {
                replaceBuf.writeVarInt(packet.packetID());
                replaceBuf.writeNbt(tag, false);
            });
        }
        // 1.20 ~ 1.20.2
        else if (clientVersion.isNewerThanOrEquals(ClientVersion.V_1_20)) {
            String json = buf.readUtf();
            Map<String, ComponentProvider> tokens = netWorkTagData.matchNetworkTags(json);
            if (tokens.isEmpty()) return;

            NetworkTextReplaceContext context = new NetworkTextReplaceContext(player, netWorkTagData);
            String replacedJson = AdventureHelper.componentToJson(clientVersion, AdventureHelper.replaceText(AdventureHelper.jsonToComponent(clientVersion, json), tokens, context));
            packet.rewritePayload(replaceBuf -> {
                replaceBuf.writeVarInt(packet.packetID());
                replaceBuf.writeUtf(replacedJson);
            });
        }
    }
}
