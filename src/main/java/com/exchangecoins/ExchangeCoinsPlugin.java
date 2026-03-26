package com.exchangecoins;

import com.exchangecoins.commands.BurseCommand;
import com.exchangecoins.commands.EcoinsCommand;
import com.exchangecoins.database.DatabaseManager;
import com.exchangecoins.economy.OrderManager;
import com.exchangecoins.economy.WidgetsManager;
import com.exchangecoins.economy.BoostManager;
import com.exchangecoins.listeners.BurseListener;
import com.exchangecoins.logging.TransactionLogger;
import com.exchangecoins.placeholders.ExchangeCoinsPlaceholder;
import com.exchangecoins.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.black_ixx.playerpoints.PlayerPoints;
import org.black_ixx.playerpoints.PlayerPointsAPI;

public class ExchangeCoinsPlugin extends JavaPlugin {

    private static ExchangeCoinsPlugin instance;

    private PlayerPointsAPI playerPointsAPI;
    private DatabaseManager databaseManager;
    private WidgetsManager widgetsManager;
    private OrderManager orderManager;
    private BoostManager boostManager;
    private TransactionLogger transactionLogger;
    private BurseListener burseListener;

    private boolean placeholderAPIEnabled = false;

    @Override
    public void onEnable() {
        instance = this;


        saveDefaultConfig();
        saveResource("confirmation.yml", false);

        transactionLogger = new TransactionLogger(this);
        transactionLogger.enable();

        getLogger().info("Запуск ExchangeCoins...");

        if (!checkDependencies()) {
            getLogger().severe("Отключаюсь из-за отсутствующих зависимостей!");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        databaseManager = new DatabaseManager(this);
        databaseManager.initialize();

        widgetsManager = new WidgetsManager(this);
        orderManager = new OrderManager(this);
        boostManager = new BoostManager(this);

        registerCommands();

        registerListeners();

        registerPlaceholderAPI();

        startExpiredOrdersTask();

        getLogger().info("ExchangeCoins успешно запущен!");
    }

    @Override
    public void onDisable() {
        if (transactionLogger != null) {
            transactionLogger.disable();
        }

        if (databaseManager != null) {
            databaseManager.shutdown();
        }

        getLogger().info("ExchangeCoins отключен!");
    }

    private boolean checkDependencies() {
        if (Bukkit.getPluginManager().getPlugin("PlayerPoints") == null) {
            getLogger().severe("PlayerPoints не найден! Установите PlayerPoints.");
            return false;
        }

        playerPointsAPI = PlayerPoints.getInstance().getAPI();
        getLogger().info("PlayerPoints найден!");

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            placeholderAPIEnabled = true;
            getLogger().info("PlaceholderAPI найден! Интеграция включена.");
        } else {
            getLogger().info("PlaceholderAPI не найден. Интеграция отключена.");
        }

        return true;
    }

    private void registerCommands() {
        BurseCommand burseCommand = new BurseCommand(this);
        EcoinsCommand ecoinsCommand = new EcoinsCommand(this);

        getCommand("burse").setExecutor(burseCommand);
        getCommand("burse").setTabCompleter(burseCommand);

        getCommand("ecoins").setExecutor(ecoinsCommand);
        getCommand("ecoins").setTabCompleter(ecoinsCommand);
    }

    private void registerListeners() {
        burseListener = new BurseListener(this);
        Bukkit.getPluginManager().registerEvents(burseListener, this);
    }

    private void registerPlaceholderAPI() {
        if (placeholderAPIEnabled) {
            try {
                ExchangeCoinsPlaceholder placeholder = new ExchangeCoinsPlaceholder(this);
                placeholder.register();
                getLogger().info("PlaceholderAPI зарегистрирован!");
            } catch (Exception e) {
                getLogger().warning("Не удалось зарегистрировать PlaceholderAPI: " + e.getMessage());
            }
        }
    }

    private void startExpiredOrdersTask() {
        long delay = 20L * 60 * 60;
        long period = 20L * 60 * 60;

        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            if (orderManager != null) {
                orderManager.removeExpiredOrders();
            }
        }, delay, period);
    }

    public static ExchangeCoinsPlugin getInstance() {
        return instance;
    }

    public PlayerPointsAPI getPlayerPointsAPI() {
        return playerPointsAPI;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public WidgetsManager getWidgetsManager() {
        return widgetsManager;
    }

    public OrderManager getOrderManager() {
        return orderManager;
    }

    public BoostManager getBoostManager() {
        return boostManager;
    }

    public TransactionLogger getTransactionLogger() {
        return transactionLogger;
    }

    public BurseListener getBurseListener() {
        return burseListener;
    }

    public boolean isPlaceholderAPIEnabled() {
        return placeholderAPIEnabled;
    }

    public int getCoinsAmount(int points) {
        return points;
    }

    public int getPointsAmount(int coins) {
        return coins;
    }
}
