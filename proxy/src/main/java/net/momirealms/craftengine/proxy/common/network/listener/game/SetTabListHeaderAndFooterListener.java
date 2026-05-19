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

public class SetTabListHeaderAndFooterListener implements PacketHandler {
    private final ProxyCraftEngine plugin;

    public SetTabListHeaderAndFooterListener(ProxyCraftEngine plugin) {
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
            Tag nbt1 = buf.readNbt(false);
            if (nbt1 == null) return;
            Tag nbt2 = buf.readNbt(false);
            if (nbt2 == null) return;
            Map<String, ComponentProvider> tokens1 = netWorkTagData.matchNetworkTags(nbt1);
            Map<String, ComponentProvider> tokens2 = netWorkTagData.matchNetworkTags(nbt2);
            if (tokens1.isEmpty() && tokens2.isEmpty()) return;

            NetworkTextReplaceContext context = new NetworkTextReplaceContext(player, netWorkTagData);
            packet.rewritePayload(replaceBuf -> {
                replaceBuf.writeVarInt(packet.packetID());
                replaceBuf.writeNbt(tokens1.isEmpty() ? nbt1 : AdventureHelper.componentToTag(clientVersion, AdventureHelper.replaceText(AdventureHelper.tagToComponent(clientVersion, nbt1), tokens1, context)), false);
                replaceBuf.writeNbt(tokens2.isEmpty() ? nbt2 : AdventureHelper.componentToTag(clientVersion, AdventureHelper.replaceText(AdventureHelper.tagToComponent(clientVersion, nbt2), tokens2, context)), false);
            });
        }
        // 1.20 ~ 1.20.2
        else if (clientVersion.isNewerThanOrEquals(ClientVersion.V_1_20)) {
            String json1 = buf.readUtf();
            String json2 = buf.readUtf();
            Map<String, ComponentProvider> tokens1 = netWorkTagData.matchNetworkTags(json1);
            Map<String, ComponentProvider> tokens2 = netWorkTagData.matchNetworkTags(json2);
            if (tokens1.isEmpty() && tokens2.isEmpty()) return;

            NetworkTextReplaceContext context = new NetworkTextReplaceContext(player, netWorkTagData);
            packet.rewritePayload(replaceBuf -> {
                replaceBuf.writeVarInt(packet.packetID());
                replaceBuf.writeUtf(tokens1.isEmpty() ? json1 : AdventureHelper.componentToJson(clientVersion, AdventureHelper.replaceText(AdventureHelper.jsonToComponent(clientVersion, json1), tokens1, context)));
                replaceBuf.writeUtf(tokens2.isEmpty() ? json2 : AdventureHelper.componentToJson(clientVersion, AdventureHelper.replaceText(AdventureHelper.jsonToComponent(clientVersion, json2), tokens2, context)));
            });
        }
    }

}
