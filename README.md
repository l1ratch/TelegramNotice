# TelegramNotice 🚀

Плагин для Minecraft сервера, который отправляет уведомления в Telegram о событиях на сервере.

## 📋 Функциональность

- 🔔 Уведомления о запуске и выключении сервера
- 👥 Уведомления о входе и выходе игроков
- ⚡ Асинхронная отправка сообщений (не блокирует основной поток)
- 🔒 Безопасная работа с токенами
- 📝 Гибкая настройка сообщений

## 🛠 Установка

1. Скачайте последнюю версию плагина из [релизов](https://github.com/l1ratch/TelegramNotice/releases)
2. Поместите файл `TelegramNotice.jar` в папку `plugins` вашего сервера
3. Перезапустите сервер
4. Настройте конфигурационный файл

## ⚙ Настройка

### 1. Создание Telegram бота
1. Напишите [@BotFather](https://t.me/BotFather) в Telegram
2. Используйте команду `/newbot` чтобы создать нового бота
3. Скопируйте полученный токен

### 2. Получение Chat ID
1. Напишите [@userinfobot](https://t.me/userinfobot) в Telegram
2. Скопируйте ваш Chat ID

### 3. Настройка config.yml

```yaml
# === TELEGRAM SETTINGS ===
# Bot token from @BotFather
botToken: "Telegram_BotToken"

# Chat ID (get it from @userinfobot)
chatId: "Telegram_ChatID"

# === GENERAL SETTINGS ===
# Server identifier (will be displayed in messages)
serverId: "(ServerName)"

# === MESSAGES ===
# Leave empty to disable specific notification

# Server start message
serverEnable: "The Minecraft server has turned on! %serverId%"

# Server stop message
serverDisable: "The Minecraft server is shutting down... %serverId%"

# Player join message
playerJoin: "Player %player% has joined the game on %serverId%."

# Player quit message
playerQuit: "Player %player% has left the game from %serverId%."
```

## 🎯 Поддерживаемые события

- **Запуск сервера** - отправляется при включении плагина
- **Выключение сервера** - отправляется при отключении плагина
- **Вход игрока** - отправляется когда игрок заходит на сервер
- **Выход игрока** - отправляется когда игрок выходит с сервера

## 🔧 Технические особенности

- ✅ Асинхронная отправка сообщений
- ✅ Валидация конфигурации
- ✅ Маскировка токенов в логах
- ✅ Обработка ошибок сети
- ✅ Поддержка UTF-8
- ✅ Совместимость с Bukkit/Spigot/Paper

## 📄 Лицензия

Этот проект распространяется под лицензией MIT. Подробнее в файле [LICENSE](LICENSE).

## ⭐ Поддержка

Если вам нравится этот плагин, не забудьте поставить звезду на GitHub!

---
**Создано с ❤️ для сообщества Minecraft**