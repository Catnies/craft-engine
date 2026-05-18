package net.momirealms.craftengine.proxy.common.network.listener.game;

import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.proxy.common.CraftEngineProxyPlugin;
import net.momirealms.craftengine.proxy.common.context.NetworkTextReplaceContext;
import net.momirealms.craftengine.proxy.common.network.ProtocolStateHolder;
import net.momirealms.craftengine.proxy.common.network.packet.PacketContext;
import net.momirealms.craftengine.proxy.common.network.packet.PacketHandler;
import net.momirealms.craftengine.proxy.common.network.protocol.player.ClientVersion;
import net.momirealms.craftengine.proxy.common.platform.ProxyPlayer;
import net.momirealms.craftengine.proxy.common.tag.NetworkTagData;
import net.momirealms.craftengine.proxy.common.text.component.ComponentProvider;
import net.momirealms.craftengine.proxy.common.util.AdventureHelper;
import net.momirealms.sparrow.nbt.Tag;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class SetScoreListener implements PacketHandler {
    private final CraftEngineProxyPlugin plugin;

    public SetScoreListener(CraftEngineProxyPlugin plugin) {
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
        FriendlyByteBuf buf = packet.payload();
        NetworkTextReplaceContext context = new NetworkTextReplaceContext(player, netWorkTagData);

        boolean isChanged = false;
        String owner = buf.readUtf();
        String objectiveName = buf.readUtf();
        int score = buf.readVarInt();
        boolean hasDisplay = buf.readBoolean();
        Tag displayName = null;
        if (hasDisplay) {
            displayName = buf.readNbt(false);
        }
        outside:
        if (displayName != null) {
            Map<String, ComponentProvider> tokens = netWorkTagData.matchNetworkTags(displayName);
            if (tokens.isEmpty()) break outside;
            Component component = AdventureHelper.tagToComponent(clientVersion, displayName);
            component = AdventureHelper.replaceText(component, tokens, context);
            displayName = AdventureHelper.componentToTag(clientVersion, component);
            isChanged = true;
        }
        boolean hasNumberFormat = buf.readBoolean();
        int format = -1;
        Tag style = null;
        Tag fixed = null;
        if (hasNumberFormat) {
            format = buf.readVarInt();
            if (format == 0) {
                if (displayName == null) return;
            } else if (format == 1) {
                if (displayName == null) return;
                style = buf.readNbt(false);
            } else if (format == 2) {
                fixed = buf.readNbt(false);
                if (fixed == null) return;
                Map<String, ComponentProvider> tokens = netWorkTagData.matchNetworkTags(fixed);
                if (tokens.isEmpty() && !isChanged) return;
                if (!tokens.isEmpty()) {
                    Component component = AdventureHelper.tagToComponent(clientVersion, fixed);
                    component = AdventureHelper.replaceText(component, tokens, context);
                    fixed = AdventureHelper.componentToTag(clientVersion, component);
                    isChanged = true;
                }
            }
        }
        if (isChanged) {
            final Tag displayNameF = displayName;
            final int formatF = format;
            final Tag styleF = style;
            final Tag fixedF = fixed;
            packet.rewritePayload(replaceBuf -> {
                replaceBuf.writeVarInt(packet.packetID());
                replaceBuf.writeUtf(owner);
                replaceBuf.writeUtf(objectiveName);
                replaceBuf.writeVarInt(score);
                if (hasDisplay) {
                    replaceBuf.writeBoolean(true);
                    replaceBuf.writeNbt(displayNameF, false);
                } else {
                    replaceBuf.writeBoolean(false);
                }
                if (hasNumberFormat) {
                    replaceBuf.writeBoolean(true);
                    replaceBuf.writeVarInt(formatF);
                    if (formatF == 1) {
                        replaceBuf.writeNbt(styleF, false);
                    } else if (formatF == 2) {
                        replaceBuf.writeNbt(fixedF, false);
                    }
                } else {
                    replaceBuf.writeBoolean(false);
                }
            });
        }
    }
}
