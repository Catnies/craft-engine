package net.momirealms.craftengine.proxy.common.network;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.play.server.*;
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
            case PacketType.Play.Server.PLAYER_LIST_HEADER_AND_FOOTER -> this.handleSetTabListHeaderAndFooterPacket(event);
            case PacketType.Play.Server.BOSS_BAR -> this.handleSetBossBarPacket(event);
            case PacketType.Play.Server.TITLE -> this.handleSetTitlePacket(event);
            case PacketType.Play.Server.SET_TITLE_TEXT -> this.handleSetTitleTextPacket(event);
            case PacketType.Play.Server.SET_TITLE_SUBTITLE -> this.handleSetSubTitlePacket(event);
            case PacketType.Play.Server.ACTION_BAR -> this.handleSetActionBarPacket(event);
            case PacketType.Play.Server.SYSTEM_CHAT_MESSAGE -> this.handleSetSystemMessagePacket(event);
            default -> { }
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

    // 处理 BossBar
    private void handleSetBossBarPacket(PacketSendEvent event) {
        // 检查是否存在玩家当前服务器的数据
        ProxyPlayer player = this.playerWrapper.apply(event.getPlayer());
        if (player == null) return;
        FontData fontData = this.fontDataSyncService.getFontDataForPlayer(player);
        if (fontData == null) return;

        WrapperPlayServerBossBar wrapper = new WrapperPlayServerBossBar(event);
        if (wrapper.getAction() != WrapperPlayServerBossBar.Action.ADD && wrapper.getAction() != WrapperPlayServerBossBar.Action.UPDATE_TITLE) return;
        Component titleComponent = wrapper.getTitle();

        String titleJson = GsonComponentSerializer.gson().serialize(titleComponent);
        Map<String, ComponentProvider> tokens = fontData.matchNetworkTags(titleJson);
        if (tokens.isEmpty()) return;

        NetworkTextReplaceContext<ProxyPlayer> context = new NetworkTextReplaceContext<>(player, fontData);
        titleComponent = AdventureHelper.replaceText(titleComponent, tokens, context);

        wrapper.setTitle(titleComponent);
    }

    // 处理 Title
    private void handleSetTitlePacket(PacketSendEvent event) {
        // 检查是否存在玩家当前服务器的数据
        ProxyPlayer player = this.playerWrapper.apply(event.getPlayer());
        if (player == null) return;
        FontData fontData = this.fontDataSyncService.getFontDataForPlayer(player);
        if (fontData == null) return;

        WrapperPlayServerTitle wrapper = new WrapperPlayServerTitle(event);
        if (wrapper.getAction() == WrapperPlayServerTitle.TitleAction.SET_TITLE) {
            Component titleComponent = wrapper.getTitle();
            if (titleComponent == null) return;

            String titleJson = GsonComponentSerializer.gson().serialize(titleComponent);
            Map<String, ComponentProvider> tokens = fontData.matchNetworkTags(titleJson);
            if (tokens.isEmpty()) return;

            NetworkTextReplaceContext<ProxyPlayer> context = new NetworkTextReplaceContext<>(player, fontData);
            titleComponent = AdventureHelper.replaceText(titleComponent, tokens, context);

            wrapper.setTitle(titleComponent);
        } else if (wrapper.getAction() == WrapperPlayServerTitle.TitleAction.SET_SUBTITLE) {
            Component subtitleComponent = wrapper.getSubtitle();
            if (subtitleComponent == null) return;

            String subtitleJson = GsonComponentSerializer.gson().serialize(subtitleComponent);
            Map<String, ComponentProvider> tokens = fontData.matchNetworkTags(subtitleJson);
            if (tokens.isEmpty()) return;

            NetworkTextReplaceContext<ProxyPlayer> context = new NetworkTextReplaceContext<>(player, fontData);
            subtitleComponent = AdventureHelper.replaceText(subtitleComponent, tokens, context);

            wrapper.setSubtitle(subtitleComponent);
        }
    }

    // 处理 Title文本
    private void handleSetTitleTextPacket(PacketSendEvent event) {
        // 检查是否存在玩家当前服务器的数据
        ProxyPlayer player = this.playerWrapper.apply(event.getPlayer());
        if (player == null) return;
        FontData fontData = this.fontDataSyncService.getFontDataForPlayer(player);
        if (fontData == null) return;

        WrapperPlayServerSetTitleText wrapper = new WrapperPlayServerSetTitleText(event);
        Component titleComponent = wrapper.getTitle();

        String titleJson = GsonComponentSerializer.gson().serialize(titleComponent);
        Map<String, ComponentProvider> tokens = fontData.matchNetworkTags(titleJson);
        if (tokens.isEmpty()) return;

        NetworkTextReplaceContext<ProxyPlayer> context = new NetworkTextReplaceContext<>(player, fontData);
        titleComponent = AdventureHelper.replaceText(titleComponent, tokens, context);

        wrapper.setTitle(titleComponent);
    }

    // 处理 Title子文本
    private void handleSetSubTitlePacket(PacketSendEvent event) {
        // 检查是否存在玩家当前服务器的数据
        ProxyPlayer player = this.playerWrapper.apply(event.getPlayer());
        if (player == null) return;
        FontData fontData = this.fontDataSyncService.getFontDataForPlayer(player);
        if (fontData == null) return;

        WrapperPlayServerSetTitleSubtitle wrapper = new WrapperPlayServerSetTitleSubtitle(event);
        Component subtitleComponent = wrapper.getSubtitle();

        String subtitleJson = GsonComponentSerializer.gson().serialize(subtitleComponent);
        Map<String, ComponentProvider> tokens = fontData.matchNetworkTags(subtitleJson);
        if (tokens.isEmpty()) return;

        NetworkTextReplaceContext<ProxyPlayer> context = new NetworkTextReplaceContext<>(player, fontData);
        subtitleComponent = AdventureHelper.replaceText(subtitleComponent, tokens, context);

        wrapper.setSubtitle(subtitleComponent);
    }

    // 处理 ActionBar
    private void handleSetActionBarPacket(PacketSendEvent event) {
        // 检查是否存在玩家当前服务器的数据
        ProxyPlayer player = this.playerWrapper.apply(event.getPlayer());
        if (player == null) return;
        FontData fontData = this.fontDataSyncService.getFontDataForPlayer(player);
        if (fontData == null) return;

        WrapperPlayServerActionBar wrapper = new WrapperPlayServerActionBar(event);
        Component component = wrapper.getActionBarText();

        String subtitleJson = GsonComponentSerializer.gson().serialize(component);
        Map<String, ComponentProvider> tokens = fontData.matchNetworkTags(subtitleJson);
        if (tokens.isEmpty()) return;

        NetworkTextReplaceContext<ProxyPlayer> context = new NetworkTextReplaceContext<>(player, fontData);
        component = AdventureHelper.replaceText(component, tokens, context);

        wrapper.setActionBarText(component);
    }

    // 处理 SystemMessage
    private void handleSetSystemMessagePacket(PacketSendEvent event) {
        // 检查是否存在玩家当前服务器的数据
        ProxyPlayer player = this.playerWrapper.apply(event.getPlayer());
        if (player == null) return;
        FontData fontData = this.fontDataSyncService.getFontDataForPlayer(player);
        if (fontData == null) return;

        WrapperPlayServerSystemChatMessage wrapper = new WrapperPlayServerSystemChatMessage(event);
        Component component = wrapper.getMessage();

        String subtitleJson = GsonComponentSerializer.gson().serialize(component);
        Map<String, ComponentProvider> tokens = fontData.matchNetworkTags(subtitleJson);
        if (tokens.isEmpty()) return;

        NetworkTextReplaceContext<ProxyPlayer> context = new NetworkTextReplaceContext<>(player, fontData);
        component = AdventureHelper.replaceText(component, tokens, context);

        wrapper.setMessage(component);
    }
}
