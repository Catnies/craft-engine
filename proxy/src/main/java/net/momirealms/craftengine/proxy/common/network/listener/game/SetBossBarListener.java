package net.momirealms.craftengine.proxy.common.network.listener.game;

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
import java.util.UUID;

public class SetBossBarListener implements PacketHandler {
    private final CraftEngineProxyPlugin plugin;

    public SetBossBarListener(CraftEngineProxyPlugin plugin) {
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

        // 1.20.3 +
        if (clientVersion.isNewerThanOrEquals(ClientVersion.V_1_20_3)) {
            UUID uuid = buf.readUUID();
            int actionType = buf.readVarInt();
            if (actionType == 0) {
                Tag nbt = buf.readNbt(false);
                if (nbt == null) return;
                Map<String, ComponentProvider> tokens = netWorkTagData.matchNetworkTags(nbt);
                if (tokens.isEmpty()) return;

                float health = buf.readFloat();
                int color = buf.readVarInt();
                int division = buf.readVarInt();
                byte flag = buf.readByte();

                packet.rewritePayload(replaceBuf -> {
                    replaceBuf.writeVarInt(packet.packetID());
                    replaceBuf.writeUUID(uuid);
                    replaceBuf.writeVarInt(actionType);
                    replaceBuf.writeNbt(
                            AdventureHelper.componentToTag(
                                    clientVersion, AdventureHelper.replaceText(
                                            AdventureHelper.tagToComponent(clientVersion, nbt), tokens, new NetworkTextReplaceContext(player, netWorkTagData)
                                    )
                            ),
                            false
                    );
                    replaceBuf.writeFloat(health);
                    replaceBuf.writeVarInt(color);
                    replaceBuf.writeVarInt(division);
                    replaceBuf.writeByte(flag);
                });
            } else if (actionType == 3) {
                Tag nbt = buf.readNbt(false);
                if (nbt == null) return;
                Map<String, ComponentProvider> tokens = netWorkTagData.matchNetworkTags(nbt);
                if (tokens.isEmpty()) return;

                packet.rewritePayload(replaceBuf -> {
                    replaceBuf.writeVarInt(packet.packetID());
                    replaceBuf.writeUUID(uuid);
                    replaceBuf.writeVarInt(actionType);
                    replaceBuf.writeNbt(
                            AdventureHelper.componentToTag(
                                    clientVersion, AdventureHelper.replaceText(
                                            AdventureHelper.tagToComponent(clientVersion, nbt), tokens, new NetworkTextReplaceContext(player, netWorkTagData)
                                    )
                            ),
                            false
                    );
                });
            }
        }
        // 1.20 ~ 1.20.2
        else if (clientVersion.isNewerThanOrEquals(ClientVersion.V_1_20)) {
            UUID uuid = buf.readUUID();
            int actionType = buf.readVarInt();
            if (actionType == 0) {
                String json = buf.readUtf();
                Map<String, ComponentProvider> tokens = netWorkTagData.matchNetworkTags(json);
                if (tokens.isEmpty()) return;

                float health = buf.readFloat();
                int color = buf.readVarInt();
                int division = buf.readVarInt();
                byte flag = buf.readByte();

                packet.rewritePayload(replaceBuf -> {
                    replaceBuf.writeVarInt(packet.packetID());
                    replaceBuf.writeUUID(uuid);
                    replaceBuf.writeVarInt(actionType);
                    replaceBuf.writeUtf(
                            AdventureHelper.componentToJson(
                                    clientVersion, AdventureHelper.replaceText(
                                            AdventureHelper.jsonToComponent(clientVersion, json), tokens, new NetworkTextReplaceContext(player, netWorkTagData)
                                    )
                            )
                    );
                    replaceBuf.writeFloat(health);
                    replaceBuf.writeVarInt(color);
                    replaceBuf.writeVarInt(division);
                    replaceBuf.writeByte(flag);
                });
            } else if (actionType == 3) {
                String json = buf.readUtf();
                Map<String, ComponentProvider> tokens = netWorkTagData.matchNetworkTags(json);
                if (tokens.isEmpty()) return;

                packet.rewritePayload(replaceBuf -> {
                    replaceBuf.writeVarInt(packet.packetID());
                    replaceBuf.writeUUID(uuid);
                    replaceBuf.writeVarInt(actionType);
                    replaceBuf.writeUtf(
                            AdventureHelper.componentToJson(
                                    clientVersion, AdventureHelper.replaceText(
                                            AdventureHelper.jsonToComponent(clientVersion, json), tokens, new NetworkTextReplaceContext(player, netWorkTagData)
                                    )
                            )
                    );
                });
            }
        }
    }

}
