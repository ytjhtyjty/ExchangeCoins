package com.exchangecoins.database;

import com.exchangecoins.ExchangeCoinsPlugin;
import com.exchangecoins.database.Order;
import com.exchangecoins.utils.ColorUtils;
import org.bukkit.configuration.ConfigurationSection;

import java.sql.*;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class DatabaseManager {

    private final ExchangeCoinsPlugin plugin;
    private Connection connection;
    private final Map<String, Long> widgetsCache = new ConcurrentHashMap<>();
    private final Map<UUID, String> usernameCache = new ConcurrentHashMap<>();

    private static final ZoneId MSK_ZONE = ZoneId.of("Europe/Moscow");
    private static final DateTimeFormatter MSK_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public DatabaseManager(ExchangeCoinsPlugin plugin) {
        this.plugin = plugin;
    }


    public void initialize() {
        try {
            connect();
            createTables();
            loadWidgetsCache();
            plugin.getLogger().info("База данных успешно инициализирована!");
        } catch (SQLException e) {
            plugin.getLogger().severe("Ошибка инициализации базы данных: " + e.getMessage());
            e.printStackTrace();
        }
    }


    private void checkConnection() throws SQLException {
        if (connection == null || connection.isClosed() || !connection.isValid(5)) {
            plugin.getLogger().warning("Соединение с БД потеряно. Переподключение...");
            connect();
        }
    }


    private boolean isValidUuid(String uuid) {
        if (uuid == null || uuid.isEmpty()) {
            return false;
        }
        try {
            UUID.fromString(uuid);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }


    private boolean isValidAmount(long amount) {
        return amount >= 0;
    }


    private void connect() throws SQLException {
        ConfigurationSection dbSection = plugin.getConfig().getConfigurationSection("database");
        String type = dbSection.getString("type", "sqlite");

        if ("mysql".equalsIgnoreCase(type)) {
            connectMySQL(dbSection);
        } else {
            connectSQLite();
        }
    }

    private void connectSQLite() throws SQLException {
        String dbPath = plugin.getDataFolder().getAbsolutePath() + "/database.db";
        connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
        plugin.getLogger().info("Подключено SQLite хранилище");
    }

    private void connectMySQL(ConfigurationSection section) throws SQLException {
        String host = section.getString("mysql.host", "localhost");
        int port = section.getInt("mysql.port", 3306);
        String database = section.getString("mysql.database", "exchangecoins");
        String username = section.getString("mysql.username", "root");
        String password = section.getString("mysql.password", "");
        boolean useSSL = section.getBoolean("mysql.useSSL", false);
        boolean autoReconnect = section.getBoolean("mysql.autoReconnect", true);

        String url = String.format("jdbc:mysql://%s:%d/%s?useSSL=%s&autoReconnect=%s&useUnicode=true&characterEncoding=utf8",
                host, port, database, useSSL, autoReconnect);

        connection = DriverManager.getConnection(url, username, password);
        plugin.getLogger().info("Подключено MySQL хранилище");
    }


    private void createTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS players_widgets (
                    uuid VARCHAR(36) PRIMARY KEY,
                    username VARCHAR(16) NOT NULL,
                    widgets BIGINT DEFAULT 0,
                    last_seen TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);


            stmt.execute("""
                CREATE TABLE IF NOT EXISTS exchange_orders (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    seller_uuid VARCHAR(36) NOT NULL,
                    seller_name VARCHAR(16) NOT NULL,
                    coins_amount INTEGER NOT NULL,
                    price BIGINT NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    expires_at TIMESTAMP NOT NULL,
                    status VARCHAR(10) DEFAULT 'active'
                )
            """);


            stmt.execute("""
                CREATE TABLE IF NOT EXISTS transactions_log (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    buyer_uuid VARCHAR(36) NOT NULL,
                    buyer_name VARCHAR(16) NOT NULL,
                    seller_uuid VARCHAR(36) NOT NULL,
                    seller_name VARCHAR(16) NOT NULL,
                    coins_amount INTEGER NOT NULL,
                    price BIGINT NOT NULL,
                    commission BIGINT DEFAULT 0,
                    transaction_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);


            stmt.execute("""
                CREATE TABLE IF NOT EXISTS player_stats (
                    uuid VARCHAR(36) PRIMARY KEY,
                    username VARCHAR(16) NOT NULL,
                    total_sold INTEGER DEFAULT 0,
                    total_bought INTEGER DEFAULT 0,
                    total_earned BIGINT DEFAULT 0,
                    total_spent BIGINT DEFAULT 0,
                    orders_created INTEGER DEFAULT 0,
                    orders_completed INTEGER DEFAULT 0,
                    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);
        }
    }


    private void loadWidgetsCache() throws SQLException {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT uuid, username, widgets FROM players_widgets")) {

            while (rs.next()) {
                String uuid = rs.getString("uuid");
                String username = rs.getString("username");
                long widgets = rs.getLong("widgets");

                widgetsCache.put(uuid, widgets);
                try {
                    usernameCache.put(UUID.fromString(uuid), username);
                } catch (IllegalArgumentException e) {

                }
            }
        }
    }




    public CompletableFuture<Long> getWidgets(String uuid) {
        return CompletableFuture.supplyAsync(() -> {
            if (widgetsCache.containsKey(uuid)) {
                return widgetsCache.get(uuid);
            }

            try (PreparedStatement stmt = connection.prepareStatement(
                    "SELECT widgets FROM players_widgets WHERE uuid = ?")) {
                stmt.setString(1, uuid);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        long widgets = rs.getLong("widgets");
                        widgetsCache.put(uuid, widgets);
                        return widgets;
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("Ошибка получения баланса: " + e.getMessage());
            }

            long startingBalance = plugin.getConfig().getLong("widgets.starting_balance", 0);
            widgetsCache.put(uuid, startingBalance);
            return startingBalance;
        });
    }


    public CompletableFuture<Void> setWidgets(String uuid, String username, long amount) {
        return CompletableFuture.runAsync(() -> {
            try (PreparedStatement stmt = connection.prepareStatement("""
                INSERT INTO players_widgets (uuid, username, widgets, last_seen)
                VALUES (?, ?, ?, CURRENT_TIMESTAMP)
                ON CONFLICT(uuid) DO UPDATE SET widgets = ?, username = ?, last_seen = CURRENT_TIMESTAMP
            """)) {
                stmt.setString(1, uuid);
                stmt.setString(2, username);
                stmt.setLong(3, amount);
                stmt.setLong(4, amount);
                stmt.setString(5, username);
                stmt.executeUpdate();

                widgetsCache.put(uuid, amount);
                if (username != null) {
                    try {
                        usernameCache.put(UUID.fromString(uuid), username);
                    } catch (IllegalArgumentException ignored) {}
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Ошибка установки баланса: " + e.getMessage());
            }
        });
    }


    public CompletableFuture<Long> addWidgets(String uuid, String username, long amount) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                long current = getWidgets(uuid).join();
                long newValue = current + amount;
                setWidgets(uuid, username, newValue).join();
                return newValue;
            } catch (Exception e) {
                plugin.getLogger().severe("Ошибка добавления виджетов: " + e.getMessage());
                return 0L;
            }
        });
    }


    public CompletableFuture<Long> removeWidgets(String uuid, String username, long amount) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                long current = getWidgets(uuid).join();
                if (current < amount) {
                    return current;
                }
                long newValue = current - amount;
                setWidgets(uuid, username, newValue).join();
                return newValue;
            } catch (Exception e) {
                plugin.getLogger().severe("Ошибка снятия виджетов: " + e.getMessage());
                return 0L;
            }
        });
    }


    public CompletableFuture<Boolean> hasWidgets(String uuid, long amount) {
        return getWidgets(uuid).thenApply(balance -> balance >= amount);
    }


    public String getUsername(UUID uuid) {
        if (usernameCache.containsKey(uuid)) {
            return usernameCache.get(uuid);
        }
        return null;
    }


    public void updateUsername(UUID uuid, String username) {
        String cachedUsername = usernameCache.get(uuid);


        if (username.equals(cachedUsername)) {
            return;
        }

        plugin.getLogger().info(String.format(
            "Обновление ника для %s: %s -> %s",
            uuid.toString().substring(0, 8),
            cachedUsername != null ? cachedUsername : "unknown",
            username
        ));

        usernameCache.put(uuid, username);
        CompletableFuture.runAsync(() -> {
            try (PreparedStatement stmt = connection.prepareStatement(
                    "UPDATE players_widgets SET username = ? WHERE uuid = ?")) {
                stmt.setString(1, username);
                stmt.setString(2, uuid.toString());
                int affected = stmt.executeUpdate();

                if (affected > 0) {
                    plugin.getLogger().info(String.format(
                        "Ник игрока %s успешно обновлён в БД", username
                    ));
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("Ошибка обновления ника в БД: " + e.getMessage());
            }
        });
    }




    public CompletableFuture<Integer> createOrder(Order order) {
        return CompletableFuture.supplyAsync(() -> {
            try (PreparedStatement stmt = connection.prepareStatement(
                    "INSERT INTO exchange_orders (seller_uuid, seller_name, coins_amount, price, created_at, expires_at, status) VALUES (?, ?, ?, ?, ?, ?, 'active')",
                    Statement.RETURN_GENERATED_KEYS)) {

                stmt.setString(1, order.getSellerUuid().toString());
                stmt.setString(2, order.getSellerName());
                stmt.setInt(3, order.getCoinsAmount());
                stmt.setLong(4, order.getPrice());

                ZonedDateTime now = ZonedDateTime.now(MSK_ZONE);
                ZonedDateTime expires = order.getExpiresAt();

                stmt.setString(5, now.format(MSK_FORMATTER));
                stmt.setString(6, expires.format(MSK_FORMATTER));

                stmt.executeUpdate();

                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        int id = rs.getInt(1);
                        order.setId(id);
                        return id;
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Ошибка создания заказа: " + e.getMessage());
                e.printStackTrace();
            }
            return -1;
        });
    }


    public CompletableFuture<List<Order>> getActiveOrders() {
        return CompletableFuture.supplyAsync(() -> {
            List<Order> orders = new ArrayList<>();
            try (PreparedStatement stmt = connection.prepareStatement(
                    "SELECT * FROM exchange_orders WHERE status = 'active' ORDER BY created_at DESC")) {

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        orders.add(Order.fromResultSet(rs));
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Ошибка получения заказов: " + e.getMessage());
            }
            return orders;
        });
    }


    public CompletableFuture<Order> getOrder(int orderId) {
        return CompletableFuture.supplyAsync(() -> {
            try (PreparedStatement stmt = connection.prepareStatement(
                    "SELECT * FROM exchange_orders WHERE id = ?")) {
                stmt.setInt(1, orderId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return Order.fromResultSet(rs);
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Ошибка получения заказа: " + e.getMessage());
            }
            return null;
        });
    }


    public CompletableFuture<List<Order>> getPlayerOrders(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            List<Order> orders = new ArrayList<>();
            try (PreparedStatement stmt = connection.prepareStatement(
                    "SELECT * FROM exchange_orders WHERE seller_uuid = ? AND status = 'active'")) {
                stmt.setString(1, playerUuid.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        orders.add(Order.fromResultSet(rs));
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Ошибка получения заказов игрока: " + e.getMessage());
            }
            return orders;
        });
    }


    public CompletableFuture<Integer> getPlayerOrdersCount(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            try (PreparedStatement stmt = connection.prepareStatement(
                    "SELECT COUNT(*) FROM exchange_orders WHERE seller_uuid = ? AND status = 'active'")) {
                stmt.setString(1, playerUuid.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Ошибка подсчёта заказов: " + e.getMessage());
            }
            return 0;
        });
    }


    public CompletableFuture<Void> deleteOrder(int orderId) {
        return CompletableFuture.runAsync(() -> {
            try (PreparedStatement stmt = connection.prepareStatement(
                    "UPDATE exchange_orders SET status = 'completed' WHERE id = ?")) {
                stmt.setInt(1, orderId);
                stmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Ошибка удаления заказа: " + e.getMessage());
            }
        });
    }


    public CompletableFuture<List<Order>> removeExpiredOrders() {
        return CompletableFuture.supplyAsync(() -> {
            List<Order> expiredOrders = new ArrayList<>();
            ZonedDateTime now = ZonedDateTime.now(MSK_ZONE);

            try (PreparedStatement stmt = connection.prepareStatement(
                    "SELECT * FROM exchange_orders WHERE expires_at < ? AND status = 'active'")) {
                stmt.setString(1, now.format(MSK_FORMATTER));

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        Order order = Order.fromResultSet(rs);
                        expiredOrders.add(order);
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Ошибка получения истёкших заказов: " + e.getMessage());
            }


            if (!expiredOrders.isEmpty()) {
                try (PreparedStatement stmt = connection.prepareStatement(
                        "UPDATE exchange_orders SET status = 'expired' WHERE id = ?")) {
                    for (Order order : expiredOrders) {
                        stmt.setInt(1, order.getId());
                        stmt.addBatch();
                    }
                    stmt.executeBatch();
                } catch (SQLException e) {
                    plugin.getLogger().severe("Ошибка удаления истёкших заказов: " + e.getMessage());
                }
            }

            return expiredOrders;
        });
    }




    public CompletableFuture<Void> logTransaction(String buyerUuid, String buyerName,
                                                   String sellerUuid, String sellerName,
                                                   int coinsAmount, long price, long commission) {
        return CompletableFuture.runAsync(() -> {
            try (PreparedStatement stmt = connection.prepareStatement(
                    "INSERT INTO transactions_log (buyer_uuid, buyer_name, seller_uuid, seller_name, coins_amount, price, commission) VALUES (?, ?, ?, ?, ?, ?, ?)")) {

                stmt.setString(1, buyerUuid);
                stmt.setString(2, buyerName);
                stmt.setString(3, sellerUuid);
                stmt.setString(4, sellerName);
                stmt.setInt(5, coinsAmount);
                stmt.setLong(6, price);
                stmt.setLong(7, commission);

                stmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Ошибка логирования транзакции: " + e.getMessage());
            }
        });
    }




    public CompletableFuture<Void> updateStats(String uuid, String username, boolean isSeller,
                                                int coinsAmount, long price) {
        return CompletableFuture.runAsync(() -> {
            try (PreparedStatement stmt = connection.prepareStatement("""
                INSERT INTO player_stats (uuid, username, total_sold, total_bought, total_earned, total_spent, orders_completed, last_updated)
                VALUES (?, ?, ?, ?, ?, ?, 1, CURRENT_TIMESTAMP)
                ON CONFLICT(uuid) DO UPDATE SET
                    username = ?,
                    total_sold = total_sold + ?,
                    total_bought = total_bought + ?,
                    total_earned = total_earned + ?,
                    total_spent = total_spent + ?,
                    orders_completed = orders_completed + 1,
                    last_updated = CURRENT_TIMESTAMP
            """)) {

                stmt.setString(1, uuid);
                stmt.setString(2, username);

                int sold = isSeller ? coinsAmount : 0;
                int bought = isSeller ? 0 : coinsAmount;
                long earned = isSeller ? price : 0;
                long spent = isSeller ? 0 : price;

                stmt.setInt(3, sold);
                stmt.setInt(4, bought);
                stmt.setLong(5, earned);
                stmt.setLong(6, spent);

                stmt.setString(7, username);
                stmt.setInt(8, sold);
                stmt.setInt(9, bought);
                stmt.setLong(10, earned);
                stmt.setLong(11, spent);

                stmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Ошибка обновления статистики: " + e.getMessage());
            }
        });
    }


    public CompletableFuture<PlayerStats> getStats(String uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try (PreparedStatement stmt = connection.prepareStatement(
                    "SELECT * FROM player_stats WHERE uuid = ?")) {
                stmt.setString(1, uuid);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return PlayerStats.fromResultSet(rs);
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Ошибка получения статистики: " + e.getMessage());
            }
            return new PlayerStats(uuid);
        });
    }


    public CompletableFuture<Void> incrementOrdersCreated(String uuid) {
        return CompletableFuture.runAsync(() -> {
            try (PreparedStatement stmt = connection.prepareStatement(
                    "INSERT INTO player_stats (uuid, username, orders_created) VALUES (?, 'unknown', 1) ON CONFLICT(uuid) DO UPDATE SET orders_created = orders_created + 1")) {
                stmt.setString(1, uuid);
                stmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Ошибка обновления счётчика заказов: " + e.getMessage());
            }
        });
    }




    public CompletableFuture<PurchaseTransactionResult> executePurchase(
            String buyerUuid, String buyerName,
            String sellerUuid, String sellerName,
            int coinsAmount, long price, long commission,
            int orderId) {

        return CompletableFuture.supplyAsync(() -> {
            Connection conn = null;
            boolean originalAutoCommit = true;

            try {

                checkConnection();

                conn = connection;
                originalAutoCommit = conn.getAutoCommit();
                conn.setAutoCommit(false);


                if (!isValidUuid(buyerUuid) || !isValidUuid(sellerUuid)) {
                    plugin.getLogger().warning("Неверный UUID в транзакции покупки");
                    return new PurchaseTransactionResult(false, "INVALID_UUID", 0);
                }

                if (price <= 0 || coinsAmount <= 0) {
                    plugin.getLogger().warning("Неверная сумма в транзакции покупки");
                    return new PurchaseTransactionResult(false, "INVALID_AMOUNT", 0);
                }


                long buyerBalance = getBalanceForTransaction(buyerUuid);
                if (buyerBalance < price) {
                    conn.rollback();
                    return new PurchaseTransactionResult(false, "INSUFFICIENT_FUNDS", buyerBalance);
                }
                updateBalanceInTransaction(buyerUuid, buyerName, buyerBalance - price);


                long sellerBalance = getBalanceForTransaction(sellerUuid);
                long sellerPrice = price - commission;
                updateBalanceInTransaction(sellerUuid, sellerName, sellerBalance + sellerPrice);


                try (PreparedStatement stmt = conn.prepareStatement(
                        "UPDATE exchange_orders SET status = 'completed' WHERE id = ? AND status = 'active'")) {
                    stmt.setInt(1, orderId);
                    int affected = stmt.executeUpdate();
                    if (affected == 0) {
                        conn.rollback();
                        return new PurchaseTransactionResult(false, "ORDER_NOT_FOUND", 0);
                    }
                }


                try (PreparedStatement stmt = conn.prepareStatement(
                        "INSERT INTO transactions_log (buyer_uuid, buyer_name, seller_uuid, seller_name, coins_amount, price, commission) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
                    stmt.setString(1, buyerUuid);
                    stmt.setString(2, buyerName);
                    stmt.setString(3, sellerUuid);
                    stmt.setString(4, sellerName);
                    stmt.setInt(5, coinsAmount);
                    stmt.setLong(6, price);
                    stmt.setLong(7, commission);
                    stmt.executeUpdate();
                }


                updateStatsInTransaction(buyerUuid, buyerName, false, coinsAmount, price, conn);


                updateStatsInTransaction(sellerUuid, sellerName, true, coinsAmount, price, conn);

                conn.commit();
                return new PurchaseTransactionResult(true, "SUCCESS", buyerBalance - price);

            } catch (SQLException e) {
                if (conn != null) {
                    try {
                        conn.rollback();
                    } catch (SQLException ex) {
                        plugin.getLogger().severe("Ошибка отката транзакции: " + ex.getMessage());
                    }
                }
                plugin.getLogger().severe("Ошибка транзакции покупки: " + e.getMessage());
                e.printStackTrace();
                return new PurchaseTransactionResult(false, "DATABASE_ERROR", 0);
            } finally {
                if (conn != null) {
                    try {
                        conn.setAutoCommit(originalAutoCommit);
                    } catch (SQLException e) {
                        plugin.getLogger().warning("Ошибка восстановления autoCommit: " + e.getMessage());
                    }
                }
            }
        });
    }


    private long getBalanceForTransaction(String uuid) throws SQLException {
        String query = connection.getMetaData().getDatabaseProductName().contains("MySQL")
                ? "SELECT widgets FROM players_widgets WHERE uuid = ? FOR UPDATE"
                : "SELECT widgets FROM players_widgets WHERE uuid = ?";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, uuid);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("widgets");
                }
            }
        }
        return plugin.getConfig().getLong("widgets.starting_balance", 0);
    }


    private void updateBalanceInTransaction(String uuid, String username, long amount) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement("""
            INSERT INTO players_widgets (uuid, username, widgets, last_seen)
            VALUES (?, ?, ?, CURRENT_TIMESTAMP)
            ON CONFLICT(uuid) DO UPDATE SET widgets = ?, username = ?, last_seen = CURRENT_TIMESTAMP
        """)) {
            stmt.setString(1, uuid);
            stmt.setString(2, username);
            stmt.setLong(3, amount);
            stmt.setLong(4, amount);
            stmt.setString(5, username);
            stmt.executeUpdate();
        }
        widgetsCache.put(uuid, amount);
    }


    private void updateStatsInTransaction(String uuid, String username, boolean isSeller,
                                          int coinsAmount, long price, Connection conn) throws SQLException {
        int sold = isSeller ? coinsAmount : 0;
        int bought = isSeller ? 0 : coinsAmount;
        long earned = isSeller ? price : 0;
        long spent = isSeller ? 0 : price;

        try (PreparedStatement stmt = conn.prepareStatement("""
            INSERT INTO player_stats (uuid, username, total_sold, total_bought, total_earned, total_spent, orders_completed, last_updated)
            VALUES (?, ?, ?, ?, ?, ?, 1, CURRENT_TIMESTAMP)
            ON CONFLICT(uuid) DO UPDATE SET
                username = ?,
                total_sold = total_sold + ?,
                total_bought = total_bought + ?,
                total_earned = total_earned + ?,
                total_spent = total_spent + ?,
                orders_completed = orders_completed + 1,
                last_updated = CURRENT_TIMESTAMP
        """)) {
            stmt.setString(1, uuid);
            stmt.setString(2, username);
            stmt.setInt(3, sold);
            stmt.setInt(4, bought);
            stmt.setLong(5, earned);
            stmt.setLong(6, spent);
            stmt.setString(7, username);
            stmt.setInt(8, sold);
            stmt.setInt(9, bought);
            stmt.setLong(10, earned);
            stmt.setLong(11, spent);
            stmt.executeUpdate();
        }
    }


    public void shutdown() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Ошибка закрытия соединения: " + e.getMessage());
        }
    }


    public ZonedDateTime getCurrentMSKTime() {
        return ZonedDateTime.now(MSK_ZONE);
    }


    public ZonedDateTime getMSKTimePlusDays(int days) {
        return ZonedDateTime.now(MSK_ZONE).plusDays(days);
    }


    public static class PurchaseTransactionResult {
        private final boolean success;
        private final String errorCode;
        private final long newBalance;

        public PurchaseTransactionResult(boolean success, String errorCode, long newBalance) {
            this.success = success;
            this.errorCode = errorCode;
            this.newBalance = newBalance;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getErrorCode() {
            return errorCode;
        }

        public long getNewBalance() {
            return newBalance;
        }
    }
}
