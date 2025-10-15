package ru.l1ratch.telegramnotice;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.Scanner;

public class TelegramNotice extends JavaPlugin implements Listener {

    private String botToken;
    private String chatId;
    private String serverId;
    private String vkAccessToken;
    private String vkPeerId;
    private String vkAutoMessagePrefix;
    private boolean configValid = true;
    private boolean telegramEnabled = false;
    private boolean vkEnabled = false;

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

        // Тестируем подключения при запуске
        testConnections();

        // Отправляем уведомления о включении сервера
        sendNotifications(getFormattedMessage("serverEnable"));

        getLogger().info("Плагин TelegramNotice успешно запущен!");
    }

    private void testConnections() {
        if (vkEnabled) {
            getLogger().info("🔍 Тестирование подключения VK...");
            testVKConnection();
        }
    }

    private void testVKConnection() {
        CompletableFuture.runAsync(() -> {
            try {
                // Получаем информацию о группе
                String urlString = "https://api.vk.com/method/groups.getById?access_token=" + vkAccessToken + "&v=5.131";
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);

                int responseCode = connection.getResponseCode();
                String responseBody = readResponse(connection);

                if (responseCode == HttpURLConnection.HTTP_OK && !responseBody.contains("\"error\"")) {
                    getLogger().info("✅ VK: Подключение к API успешно");

                    // Извлекаем ID группы из ответа
                    if (responseBody.contains("\"id\"")) {
                        String idStr = responseBody.split("\"id\":")[1].split(",")[0].trim();
                        getLogger().info("✅ VK: ID группы: " + idStr);

                        // Проверяем возможность отправки в указанный чат
                        testChatConnection();
                    }
                } else {
                    if (responseBody.contains("error_code\":5")) {
                        getLogger().severe("❌ VK: Неверный токен доступа!");
                    } else {
                        getLogger().warning("❌ VK: Ошибка получения информации о группе: " + responseBody);
                    }
                }

                connection.disconnect();
            } catch (Exception e) {
                getLogger().warning("❌ VK: Ошибка тестирования: " + e.getMessage());
            }
        });
    }

    private void testChatConnection() {
        try {
            String testMessage = "🔧 Тестовое сообщение от Minecraft сервера";
            String vkMessage = getVKFormattedMessage(testMessage);

            String urlString = "https://api.vk.com/method/messages.send";
            String requestBody = "peer_id=" + vkPeerId +
                    "&message=" + URLEncoder.encode(vkMessage, StandardCharsets.UTF_8.toString()) +
                    "&random_id=" + System.currentTimeMillis() +
                    "&access_token=" + vkAccessToken +
                    "&v=5.131";

            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setDoOutput(true);
            connection.setConnectTimeout(10000);

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = requestBody.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = connection.getResponseCode();
            String responseBody = readResponse(connection);

            if (responseCode == HttpURLConnection.HTTP_OK) {
                if (responseBody.contains("\"error\"")) {
                    analyzeVKError(responseBody);
                } else {
                    getLogger().info("✅ VK: Тестовое сообщение отправлено успешно!");
                }
            }

            connection.disconnect();
        } catch (Exception e) {
            getLogger().warning("❌ VK: Ошибка тестирования чата: " + e.getMessage());
        }
    }

    private void analyzeVKError(String responseBody) {
        if (responseBody.contains("error_code\":917")) {
            getLogger().severe("❌ VK: Группа не добавлена в беседу!");
            getLogger().severe("📝 Решение: Добавьте группу в беседу как участника");
        } else if (responseBody.contains("error_code\":901")) {
            getLogger().severe("❌ VK: Нет прав для отправки сообщений!");
            getLogger().severe("📝 Решение: Для личных сообщений пользователь должен написать первым");
        } else if (responseBody.contains("error_code\":15")) {
            getLogger().severe("❌ VK: Нет доступа к чату!");
            getLogger().severe("📝 Решение: Убедитесь, что чат существует и группа имеет к нему доступ");
        } else {
            getLogger().warning("❌ VK ошибка API: " + responseBody);
        }
    }

    @Override
    public void onDisable() {
        if (configValid) {
            String message = getFormattedMessage("serverDisable");
            if (message != null && !message.isEmpty()) {
                sendNotificationsSync(message);
            }
        }
        getLogger().info("Плагин TelegramNotice отключен!");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        sendNotifications(getFormattedMessage("playerJoin", event.getPlayer().getName()));
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        sendNotifications(getFormattedMessage("playerQuit", event.getPlayer().getName()));
    }

    private void loadAndValidateConfig() {
        // Telegram настройки
        botToken = getConfig().getString("botToken", "").trim();
        chatId = getConfig().getString("chatId", "").trim();
        serverId = getConfig().getString("serverId", "").trim();

        // VK настройки
        vkAccessToken = getConfig().getString("vkAccessToken", "").trim();
        vkPeerId = getConfig().getString("vkPeerId", "").trim();
        vkAutoMessagePrefix = getConfig().getString("vkAutoMessagePrefix", "🤖 Автоматическое уведомление:").trim();

        // Проверяем, включены ли мессенджеры
        telegramEnabled = isTelegramEnabled();
        vkEnabled = isVKEnabled();

        if (!telegramEnabled && !vkEnabled) {
            getLogger().warning("Ни один мессенджер не настроен!");
            configValid = true;
            return;
        }

        // Детальная информация о настройках
        if (telegramEnabled) {
            getLogger().info("✅ Telegram: ВКЛЮЧЕН");
            getLogger().info("   Bot Token: " + maskToken(botToken));
            getLogger().info("   Chat ID: " + chatId);
        } else {
            getLogger().info("❌ Telegram: ОТКЛЮЧЕН");
        }

        if (vkEnabled) {
            getLogger().info("✅ VK: ВКЛЮЧЕН");
            getLogger().info("   Access Token: " + maskToken(vkAccessToken));
            getLogger().info("   Peer ID: " + vkPeerId);

            try {
                long peerId = Long.parseLong(vkPeerId);
                if (peerId > 2000000000) {
                    getLogger().info("   Тип чата: Беседа");
                    long chatId = peerId - 2000000000;
                    getLogger().info("   ID беседы: " + chatId);
                } else if (peerId < 0) {
                    getLogger().info("   Тип чата: Чат сообщества");
                    getLogger().info("   ID группы: " + Math.abs(peerId));
                } else {
                    getLogger().info("   Тип чата: Личные сообщения");
                    getLogger().warning("   ВНИМАНИЕ: Для сообществ личные сообщения могут не работать!");
                }
            } catch (NumberFormatException e) {
                getLogger().warning("   ОШИБКА: vkPeerId должен быть числом!");
            }
        } else {
            getLogger().info("❌ VK: ОТКЛЮЧЕН");
        }

        getLogger().info("Конфигурация загружена успешно!");
        configValid = true;
    }

    private String maskToken(String token) {
        if (token == null || token.length() < 10) return "***";
        return token.substring(0, 6) + "***" + token.substring(token.length() - 4);
    }

    private boolean isTelegramEnabled() {
        if (botToken.isEmpty() || chatId.isEmpty()) return false;
        return !botToken.equals("Telegram_BotToken") &&
                !chatId.equals("Telegram_ChatID");
    }

    private boolean isVKEnabled() {
        if (vkAccessToken.isEmpty() || vkPeerId.isEmpty()) return false;
        return !vkAccessToken.equals("VK_Access_Token") &&
                !vkPeerId.equals("VK_Peer_ID");
    }

    private String getFormattedMessage(String key, String... args) {
        String message = getConfig().getString(key, "").trim();
        if (message.isEmpty()) return null;

        message = message.replace("%serverId%", serverId.isEmpty() ? "Server" : serverId);

        for (int i = 0; i < args.length; i++) {
            message = message.replace("%player%", args[i]);
        }

        return message;
    }

    private String getVKFormattedMessage(String originalMessage) {
        return vkAutoMessagePrefix + "\n\n" + originalMessage;
    }

    private void sendNotifications(String message) {
        if (message == null || message.isEmpty() || !configValid) return;

        CompletableFuture.runAsync(() -> {
            if (telegramEnabled) sendToTelegram(message);
            if (vkEnabled) sendToVK(message);
        }).exceptionally(throwable -> {
            getLogger().warning("Ошибка при асинхронной отправке: " + throwable.getMessage());
            return null;
        });
    }

    private void sendNotificationsSync(String message) {
        if (telegramEnabled) sendToTelegramSync(message);
        if (vkEnabled) sendToVKSync(message);
    }

    private void sendToTelegram(String message) {
        CompletableFuture.runAsync(() -> sendToTelegramSync(message));
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

            int responseCode = connection.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                getLogger().info("✅ Telegram: " + message);
            } else {
                getLogger().warning("❌ Telegram ошибка: " + responseCode);
            }

            connection.disconnect();
        } catch (IOException e) {
            getLogger().warning("❌ Telegram: " + e.getMessage());
        }
    }

    private void sendToVK(String message) {
        CompletableFuture.runAsync(() -> sendToVKSync(message));
    }

    private void sendToVKSync(String message) {
        try {
            String vkMessage = getVKFormattedMessage(message);

            String urlString = "https://api.vk.com/method/messages.send";
            String requestBody = "peer_id=" + vkPeerId +
                    "&message=" + URLEncoder.encode(vkMessage, StandardCharsets.UTF_8.toString()) +
                    "&random_id=" + System.currentTimeMillis() +
                    "&access_token=" + vkAccessToken +
                    "&v=5.131";

            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setDoOutput(true);
            connection.setConnectTimeout(10000);

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = requestBody.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = connection.getResponseCode();
            String responseBody = readResponse(connection);

            if (responseCode == HttpURLConnection.HTTP_OK) {
                if (responseBody.contains("\"error\"")) {
                    analyzeVKError(responseBody);
                    getLogger().warning("❌ VK: Не удалось отправить сообщение: " + message);
                } else {
                    getLogger().info("✅ VK: " + message);
                }
            } else {
                getLogger().warning("❌ VK HTTP ошибка: " + responseCode);
            }

            connection.disconnect();
        } catch (IOException e) {
            getLogger().warning("❌ VK: " + e.getMessage());
        }
    }

    private String readResponse(HttpURLConnection connection) {
        try {
            InputStream inputStream = connection.getResponseCode() >= 400 ?
                    connection.getErrorStream() : connection.getInputStream();

            if (inputStream == null) return "{}";

            try (Scanner scanner = new Scanner(inputStream, StandardCharsets.UTF_8.name())) {
                return scanner.useDelimiter("\\A").hasNext() ? scanner.next() : "{}";
            }
        } catch (IOException e) {
            return "{\"error\":\"Failed to read response: " + e.getMessage() + "\"}";
        }
    }
}