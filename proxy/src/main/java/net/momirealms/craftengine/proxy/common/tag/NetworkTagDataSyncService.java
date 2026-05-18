package net.momirealms.craftengine.proxy.common.tag;

import io.netty.buffer.Unpooled;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.proxy.common.CraftEngineProxyPlugin;
import net.momirealms.craftengine.proxy.common.platform.ProxyPlayer;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.SecureRandom;
import java.util.Properties;
import java.util.UUID;

public final class NetworkTagDataSyncService {
    public static final String TAG_DATA_CHANNEL = "craftengine:tag_data";
    private static final String SECRET_FILE_NAME = "secret.Properties";
    private static final String SECRET_KEY = "secret";
    private static final String SECRET_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int SECRET_LENGTH = 16;
    private static final SecureRandom RANDOM = new SecureRandom();
    public static final UUID PROXY_UUID = UUID.randomUUID();
    private final CraftEngineProxyPlugin plugin;
    private final NetworkTagDataRegistry registry;
    private final NetworkTagDataCodec codec;
    private final String secret;

    public NetworkTagDataSyncService(CraftEngineProxyPlugin plugin) {
        this.plugin = plugin;
        this.registry = new NetworkTagDataRegistry();
        this.codec = new NetworkTagDataCodec(this.registry);
        this.secret = this.readOrCreateSecret();
    }

    private String readOrCreateSecret() {
        Path folderPath = this.plugin.dataFolderPath();
        Path secretFilePath = folderPath.resolve(SECRET_FILE_NAME);

        try {
            Files.createDirectories(folderPath);
            Properties properties = new Properties();

            if (Files.exists(secretFilePath)) {
                try (Reader reader = Files.newBufferedReader(secretFilePath, StandardCharsets.UTF_8)) {
                    properties.load(reader);
                }
                String existingSecret = properties.getProperty(SECRET_KEY);
                if (existingSecret != null && !existingSecret.isBlank()) {
                    return existingSecret;
                }
            }

            String newSecret = generateSecret(SECRET_LENGTH);
            properties.setProperty(SECRET_KEY, newSecret);

            try (Writer writer = Files.newBufferedWriter(
                    secretFilePath,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
            )) {
                properties.store(writer, "Plugin secret");
            }

            return newSecret;
        } catch (IOException e) {
            throw new RuntimeException("Failed to read or create secret file: " + secretFilePath, e);
        }
    }

    private static String generateSecret(int length) {
        StringBuilder builder = new StringBuilder(length);

        for (int i = 0; i < length; i++) {
            int index = RANDOM.nextInt(SECRET_CHARS.length());
            builder.append(SECRET_CHARS.charAt(index));
        }

        return builder.toString();
    }

    public NetworkTagDataRegistry registry() {
        return this.registry;
    }

    public NetworkTagDataCodec codec() {
        return codec;
    }

    public String secret() {
        return secret;
    }

    @Nullable
    public NetworkTagData getTagDataForPlayer(ProxyPlayer player) {
        return this.registry.getForPlayer(player);
    }

    public void sendTagDataVersion(ProxyPlayer player) {
        NetworkTagData netWorkTagData = this.getTagDataForPlayer(player);
        long version = netWorkTagData != null ? netWorkTagData.version() : -1L;

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeUtf(this.secret);
        buf.writeLong(version);
        buf.writeUUID(PROXY_UUID);
        byte[] data = new byte[buf.readableBytes()];
        buf.getBytes(buf.readerIndex(), data);
        player.sendServerPluginMessage(TAG_DATA_CHANNEL, data);
    }

    public void receiveTagData(String serverName, FriendlyByteBuf in) {
        if (this.secret.equals(in.readUtf())) {
            this.registry.put(serverName, this.codec.read(serverName, in));
        }
    }

    public void clear() {
        this.registry.clear();
    }
}
