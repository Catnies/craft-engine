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

import java.util.List;
import java.util.Map;

public class SetPlayerTeamListener implements PacketHandler {
    private final ProxyCraftEngine plugin;

    public SetPlayerTeamListener(ProxyCraftEngine plugin) {
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
            String name = buf.readUtf();
            byte method = buf.readByte();
            if (method != 2 && method != 0) return;
            Tag displayName = buf.readNbt(false);
            if (displayName == null) return;
            byte friendlyFlags = buf.readByte();

            boolean over1205 = clientVersion.isNewerThanOrEquals(ClientVersion.V_1_20_5);
            Integer visibilityInt = over1205 ? buf.readVarInt() : null;
            String visibilityStr = over1205 ? null : buf.readUtf(40);
            Integer collisionRuleInt = over1205 ? buf.readVarInt() : null;
            String collisionRuleStr = over1205 ? null : buf.readUtf(40);

            int color = buf.readVarInt();
            Tag prefix = buf.readNbt(false);
            if (prefix == null) return;
            Tag suffix = buf.readNbt(false);
            if (suffix == null) return;
            List<String> entities = method == 0 ? buf.readStringList() : null;

            Map<String, ComponentProvider> tokens1 = netWorkTagData.matchNetworkTags(displayName);
            Map<String, ComponentProvider> tokens2 = netWorkTagData.matchNetworkTags(prefix);
            Map<String, ComponentProvider> tokens3 = netWorkTagData.matchNetworkTags(suffix);
            if (tokens1.isEmpty() && tokens2.isEmpty() && tokens3.isEmpty()) return;

            NetworkTextReplaceContext context = new NetworkTextReplaceContext(player, netWorkTagData);
            packet.rewritePayload(replaceBuf -> {
                replaceBuf.writeVarInt(packet.packetID());
                replaceBuf.writeUtf(name);
                replaceBuf.writeByte(method);
                replaceBuf.writeNbt(tokens1.isEmpty() ? displayName : AdventureHelper.componentToTag(clientVersion, AdventureHelper.replaceText(AdventureHelper.tagToComponent(clientVersion, displayName), tokens1, context)), false);
                replaceBuf.writeByte(friendlyFlags);
                if (visibilityInt != null) replaceBuf.writeVarInt(visibilityInt); else replaceBuf.writeUtf(visibilityStr);
                if (collisionRuleInt != null) replaceBuf.writeVarInt(collisionRuleInt); else replaceBuf.writeUtf(collisionRuleStr);
                replaceBuf.writeVarInt(color);
                replaceBuf.writeNbt(tokens2.isEmpty() ? prefix : AdventureHelper.componentToTag(clientVersion, AdventureHelper.replaceText(AdventureHelper.tagToComponent(clientVersion, prefix), tokens2, context)), false);
                replaceBuf.writeNbt(tokens3.isEmpty() ? suffix : AdventureHelper.componentToTag(clientVersion, AdventureHelper.replaceText(AdventureHelper.tagToComponent(clientVersion, suffix), tokens3, context)), false);
                if (entities != null) {
                    replaceBuf.writeStringList(entities);
                }
            });
        }
        // 1.20 ~ 1.20.2
        else if (clientVersion.isNewerThanOrEquals(ClientVersion.V_1_20)) {
            String name = buf.readUtf();
            byte method = buf.readByte();
            if (method != 2 && method != 0)
                return;
            String displayName = buf.readUtf();
            byte friendlyFlags = buf.readByte();
            String nameTagVisibility = buf.readUtf(40);
            String collisionRule = buf.readUtf(40);
            int color = buf.readVarInt();
            String prefix = buf.readUtf();
            String suffix = buf.readUtf();
            List<String> entities = method == 0 ? buf.readStringList() : null;

            Map<String, ComponentProvider> tokens1 = netWorkTagData.matchNetworkTags(displayName);
            Map<String, ComponentProvider> tokens2 = netWorkTagData.matchNetworkTags(prefix);
            Map<String, ComponentProvider> tokens3 = netWorkTagData.matchNetworkTags(suffix);
            if (tokens1.isEmpty() && tokens2.isEmpty() && tokens3.isEmpty()) return;

            NetworkTextReplaceContext context = new NetworkTextReplaceContext(player, netWorkTagData);
            packet.rewritePayload(replaceBuf -> {
                replaceBuf.writeVarInt(packet.packetID());
                replaceBuf.writeUtf(name);
                replaceBuf.writeByte(method);
                replaceBuf.writeUtf(tokens1.isEmpty() ? displayName : AdventureHelper.componentToJson(clientVersion, AdventureHelper.replaceText(AdventureHelper.jsonToComponent(clientVersion, displayName), tokens1, context)));
                replaceBuf.writeByte(friendlyFlags);
                replaceBuf.writeUtf(nameTagVisibility);
                replaceBuf.writeUtf(collisionRule);
                replaceBuf.writeVarInt(color);
                replaceBuf.writeUtf(tokens2.isEmpty() ? prefix : AdventureHelper.componentToJson(clientVersion, AdventureHelper.replaceText(AdventureHelper.jsonToComponent(clientVersion, prefix), tokens2, context)));
                replaceBuf.writeUtf(tokens3.isEmpty() ? suffix : AdventureHelper.componentToJson(clientVersion, AdventureHelper.replaceText(AdventureHelper.jsonToComponent(clientVersion, suffix), tokens3, context)));
                if (entities != null) {
                    buf.writeStringList(entities);
                }
            });
        }
    }

}
