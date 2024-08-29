package ru.l1ratch.telegramnotice;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Bukkit;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.DataOutputStream;

public class TelegramNotice extends JavaPlugin implements Listener {

    private String botToken;
    private String chatId;
    private String serverId;

    @Override
    public void onEnable() {
        saveDefaultConfig();  // Ensure the config file is loaded
        botToken = getConfig().getString("botToken");
        chatId = getConfig().getString("chatId");
        serverId = getConfig().getString("serverId");

        getServer().getPluginManager().registerEvents(this, this);
        sendMessageToTelegram(getFormattedMessage("serverEnable"));
    }

    @Override
    public void onDisable() {
        sendMessageToTelegram(getFormattedMessage("serverDisable"));
    }

    @EventHandler
    public void onServerStart(ServerLoadEvent event) {
        sendMessageToTelegram(getFormattedMessage("serverEnable"));
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        sendMessageToTelegram(getFormattedMessage("playerJoin", event.getPlayer().getName()));
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        sendMessageToTelegram(getFormattedMessage("playerQuit", event.getPlayer().getName()));
    }

    private String getFormattedMessage(String key, String... args) {
        String message = getConfig().getString(key);
        if (message != null && !message.isEmpty()) {
            message = message.replace("%serverId%", serverId);
            for (int i = 0; i < args.length; i++) {
                message = message.replace("%player%", args[i]);
            }
            return message;
        }
        return null;
    }

    private void sendMessageToTelegram(String message) {
        if (message == null || message.isEmpty()) {
            return;
        }

        String urlString = "https://api.telegram.org/bot" + botToken + "/sendMessage?chat_id=" + chatId + "&text=" + message;

        try {
            URL url = new URL(urlString);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");

            int responseCode = con.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                Bukkit.getLogger().info("Сообщение успешно отправлено в Telegram!");
            } else {
                Bukkit.getLogger().warning("Не удалось отправить сообщение в Telegram. Код ошибки: " + responseCode);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}