package ru.l1ratch.telegramnotice;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

public class TelegramNotice extends JavaPlugin implements Listener {

    private String botToken;
    private String chatId;
    private String serverId;
    private boolean configValid = true;
    private boolean telegramEnabled = false;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadConfig();

        loadAndValidateConfig();

        if (!configValid) {
            getLogger().severe("Плагин отключен из-за невалидной конфигурации!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getServer().getPluginManager().registerEvents(this, this);

        // Отправляем уведомление о включении сервера
        sendToTelegram(getFormattedMessage("serverEnable"));

        getLogger().info("Плагин TelegramNotice успешно запущен!");
    }

    @Override
    public void onDisable() {
        if (configValid && telegramEnabled) {
            String message = getFormattedMessage("serverDisable");
            if (message != null && !message.isEmpty()) {
                sendToTelegramSync(message);
            }
        }
        getLogger().info("Плагин TelegramNotice отключен!");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        sendToTelegram(getFormattedMessage("playerJoin", event.getPlayer().getName()));
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        sendToTelegram(getFormattedMessage("playerQuit", event.getPlayer().getName()));
    }

    private void loadAndValidateConfig() {
        // Telegram настройки
        botToken = getConfig().getString("botToken", "").trim();
        chatId = getConfig().getString("chatId", "").trim();
        serverId = getConfig().getString("serverId", "").trim();

        // Проверяем, включен ли Telegram
        telegramEnabled = isTelegramEnabled();

        if (!telegramEnabled) {
            getLogger().warning("Telegram не настроен!");
            getLogger().warning("Заполните botToken и chatId в config.yml");
            configValid = true;
            return;
        }

        // Детальная информация о настройках
        getLogger().info("✅ Telegram: ВКЛЮЧЕН");
        getLogger().info("   Bot Token: " + maskToken(botToken));
        getLogger().info("   Chat ID: " + chatId);
        getLogger().info("   Server ID: " + serverId);

        getLogger().info("Конфигурация загружена успешно!");
        configValid = true;
    }

    private String maskToken(String token) {
        if (token == null || token.length() < 10) return "***";
        return token.substring(0, 6) + "***" + token.substring(token.length() - 4);
    }

    private boolean isTelegramEnabled() {
        if (botToken.isEmpty() || chatId.isEmpty()) return false;

        // Проверяем, что это не стандартные заглушки
        boolean isDefaultToken = botToken.equals("Telegram_BotToken") ||
                botToken.equals("YOUR_BOT_TOKEN") ||
                botToken.equals("placeholder") ||
                botToken.contains("example");

        boolean isDefaultChatId = chatId.equals("Telegram_ChatID") ||
                chatId.equals("YOUR_CHAT_ID") ||
                chatId.equals("placeholder") ||
                chatId.contains("example");

        return !isDefaultToken && !isDefaultChatId;
    }

    private String getFormattedMessage(String key, String... args) {
        String message = getConfig().getString(key, "").trim();

        if (message.isEmpty()) {
            return null;
        }

        message = message.replace("%serverId%", serverId.isEmpty() ? "Server" : serverId);

        for (int i = 0; i < args.length; i++) {
            message = message.replace("%player%", args[i]);
        }

        return message;
    }

    private void sendToTelegram(String message) {
        if (message == null || message.isEmpty() || !telegramEnabled) return;

        CompletableFuture.runAsync(() -> {
            sendToTelegramSync(message);
        }).exceptionally(throwable -> {
            getLogger().warning("Ошибка при асинхронной отправке в Telegram: " + throwable.getMessage());
            return null;
        });
    }

    private void sendToTelegramSync(String message) {
        try {
            String encodedMessage = URLEncoder.encode(message, StandardCharsets.UTF_8.toString());
            String urlString = "https://api.telegram.org/bot" + botToken +
                    "/sendMessage?chat_id=" + chatId + "&text=" + encodedMessage;

            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);

            int responseCode = connection.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                getLogger().info("✅ Telegram: " + message);
            } else {
                getLogger().warning("❌ Не удалось отправить сообщение в Telegram. Код ошибки: " + responseCode);

                // Детальная информация об ошибках Telegram
                if (responseCode == 400) {
                    getLogger().warning("   Проверьте chatId - возможно он неверный");
                } else if (responseCode == 404) {
                    getLogger().warning("   Проверьте botToken - возможно он неверный");
                }
            }

            connection.disconnect();

        } catch (IOException e) {
            getLogger().warning("❌ Ошибка при отправке сообщения в Telegram: " + e.getMessage());
        }
    }
}