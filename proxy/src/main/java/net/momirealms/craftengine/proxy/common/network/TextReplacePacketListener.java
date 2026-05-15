package net.momirealms.craftengine.proxy.common.network;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerListHeaderAndFooter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.momirealms.craftengine.proxy.common.context.NetworkTextReplaceContext;
import net.momirealms.craftengine.proxy.common.platform.ProxyPlayer;
import net.momirealms.craftengine.proxy.common.font.FontData;
import net.momirealms.craftengine.proxy.common.font.FontDataSyncService;
import net.momirealms.craftengine.proxy.common.text.component.ComponentProvider;
import net.momirealms.craftengine.proxy.common.util.AdventureHelper;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.Function;

public class TextReplacePacketListener implements PacketListener {
    private final FontDataSyncService fontDataSyncService;
    private final Function<Object, @Nullable ProxyPlayer> playerWrapper;

    public TextReplacePacketListener(FontDataSyncService fontDataSyncService, Function<Object, @Nullable ProxyPlayer> playerWrapper) {
        this.fontDataSyncService = fontDataSyncService;
        this.playerWrapper = playerWrapper;
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        PacketTypeCommon packetType = event.getPacketType();
        switch (packetType) {
            case PacketType.Play.Server.PLAYER_LIST_HEADER_AND_FOOTER -> handleSetTabListHeaderAndFooterPacket(event);
            default -> { return; }
        }
    }

    // 处理 Tab
    private void handleSetTabListHeaderAndFooterPacket(PacketSendEvent event) {
        // 检查是否存在玩家当前服务器的数据
        ProxyPlayer player = this.playerWrapper.apply(event.getPlayer());
        if (player == null) return;
        FontData fontData = this.fontDataSyncService.getFontDataForPlayer(player);
        if (fontData == null) return;

        WrapperPlayServerPlayerListHeaderAndFooter wrapper = new WrapperPlayServerPlayerListHeaderAndFooter(event);
        Component headerComponent = wrapper.getHeader();
        Component footerComponent = wrapper.getFooter();
        String headerJson = GsonComponentSerializer.gson().serialize(headerComponent);
        String footerJson = GsonComponentSerializer.gson().serialize(footerComponent);
        Map<String, ComponentProvider> tokens1 = fontData.matchNetworkTags(headerJson);
        Map<String, ComponentProvider> tokens2 = fontData.matchNetworkTags(footerJson);
        if (tokens1.isEmpty() && tokens2.isEmpty()) return;

        NetworkTextReplaceContext<ProxyPlayer> context = new NetworkTextReplaceContext<>(player, fontData);
        if (!tokens1.isEmpty()) headerComponent = AdventureHelper.replaceText(headerComponent, tokens1, context);
        if (!tokens2.isEmpty()) footerComponent = AdventureHelper.replaceText(footerComponent, tokens2, context);

        wrapper.setHeader(headerComponent);
        wrapper.setFooter(footerComponent);
    }
}
