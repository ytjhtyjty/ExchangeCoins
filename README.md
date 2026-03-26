# ExchangeCoins

**Биржа для обмена PlayerPoints на виджеты**

---

## 📋 Основная информация

- **Версия:** 1.0.0
- **Целевая платформа:** PurpurMC 1.20.x - 1.21.x
- **Минимальная версия Java:** 17
- **Система сборки:** Maven

---

## 🔧 Зависимости

### Обязательные:
- **PlayerPoints** — система коинов

### Опциональные:
- **PlaceholderAPI** — поддержка плейсхолдеров

---

## 📥 Установка

1. Скачайте плагин из раздела [Releases](https://github.com/exchangecoins/ExchangeCoins/releases)
2. Поместите файл `ExchangeCoins-1.0.0.jar` в папку `plugins` вашего сервера
3. Убедитесь, что установлен плагин PlayerPoints
4. Запустите сервер для генерации конфигурации
5. Настройте `config.yml` под ваши нужды
6. Перезагрузите сервер или используйте `/ecoins reload`

---

## 🎯 Команды

| Команда | Описание | Permission |
|---------|----------|------------|
| `/burse` | Открыть меню биржи | `exchangecoins.burse` |
| `/ecoins order <количество> <цена>` | Создать заказ на продажу | `exchangecoins.order` |
| `/ecoins balance [ник]` | Проверить баланс виджетов | `exchangecoins.balance` |
| `/ecoins stats [ник]` | Статистика игрока | `exchangecoins.balance` |
| `/ecoins give <ник> <сумма>` | Выдать виджеты | `exchangecoins.give` (op) |
| `/ecoins take <ник> <сумма>` | Снять виджеты | `exchangecoins.take` (op) |
| `/ecoins set <ник> <сумма>` | Установить баланс | `exchangecoins.set` (op) |
| `/ecoins reload` | Перезагрузить конфиг | `exchangecoins.reload` (op) |

---

## 🏦 Механика работы

### Создание заказа
```
/ecoins order 1000 5000
```
Продаст 1000 коинов PlayerPoints за 5000 виджетов.

### Покупка
1. Откройте меню биржи: `/burse`
2. Кликните на заказ для покупки
3. Виджеты спишутся, коины будут получены

---

## ⚙️ Конфигурация

### Основные настройки

```yaml
# Настройки меню
menu:
  title: "&6&lБиржа Коинов"
  size: 54
  
# Настройки заказов
orders:
  default_expire_days: 6      # Срок действия заказа
  max_orders_per_player: 5    # Макс. заказов на игрока
  commission_percent: 0       # Комиссия биржи (%)
  min_order_amount: 1         # Мин. количество коинов
  max_order_amount: 1000000   # Макс. количество коинов
  order_cooldown_seconds: 30  # Кулдаун между заказами

# Настройки виджетов
widgets:
  starting_balance: 0         # Стартовый баланс
  max_balance: 9223372036854775807
```

### PlaceholderAPI

Доступные плейсхолдеры:
- `%exchangecoins_widgets%` — баланс виджетов
- `%exchangecoins_active_orders%` — количество активных заказов
- `%exchangecoins_stats_sold%` — всего продано коинов
- `%exchangecoins_stats_bought%` — всего куплено коинов
- `%exchangecoins_stats_earned%` — всего заработано виджетов
- `%exchangecoins_stats_spent%` — всего потрачено виджетов

---

## 📁 Структура проекта

```
ExchangeCoins/
├── pom.xml
├── src/main/
│   ├── java/com/exchangecoins/
│   │   ├── ExchangeCoinsPlugin.java
│   │   ├── commands/
│   │   │   ├── BurseCommand.java
│   │   │   └── EcoinsCommand.java
│   │   ├── database/
│   │   │   ├── DatabaseManager.java
│   │   │   ├── Order.java
│   │   │   └── PlayerStats.java
│   │   ├── economy/
│   │   │   ├── WidgetsManager.java
│   │   │   └── OrderManager.java
│   │   ├── listeners/
│   │   │   └── BurseListener.java
│   │   ├── logging/
│   │   │   └── TransactionLogger.java
│   │   ├── menu/
│   │   │   └── BurseMenu.java
│   │   ├── placeholders/
│   │   │   └── ExchangeCoinsPlaceholder.java
│   │   └── utils/
│   │       ├── ColorUtils.java
│   │       └── PlaceholderUtils.java
│   └── resources/
│       ├── plugin.yml
│       └── config.yml
```

---

## 🔨 Сборка

```bash
mvn clean package
```

Готовый JAR файл появится в папке `target/`

---

## 📊 База данных

### Таблицы:
- `players_widgets` — балансы игроков
- `exchange_orders` — активные заказы
- `transactions_log` — история транзакций
- `player_stats` — статистика игроков

---

## 📝 Логирование

Логи транзакций сохраняются в:
```
plugins/ExchangeCoins/logs/transactions_YYYY-MM.log
```

Формат записи:
```
[2026-03-23 15:30:45 MSK] buyer:Notch - seller:Jeb_ - coins:1000 - price:5000
```

---

## 🎨 Особенности

- ✅ Асинхронные операции с БД
- ✅ Кэширование балансов
- ✅ Поддержка цветов и HEX
- ✅ Система комиссий
- ✅ Уведомления о покупках
- ✅ Автоматическая очистка истёкших заказов
- ✅ Защита от дюпа
- ✅ Полная настройка через конфиг

---

## 📄 Лицензия

MIT License

---

## 🤝 Поддержка

При возникновении проблем создайте issue в репозитории.
