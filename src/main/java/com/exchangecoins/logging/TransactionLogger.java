package com.exchangecoins.logging;

import com.exchangecoins.ExchangeCoinsPlugin;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class TransactionLogger {

    private final ExchangeCoinsPlugin plugin;
    private final BlockingQueue<LogEntry> logQueue = new LinkedBlockingQueue<>();
    private final AtomicBoolean enabled = new AtomicBoolean(false);
    private Thread loggerThread;

    private static final ZoneId MSK_ZONE = ZoneId.of("Europe/Moscow");
    private static final DateTimeFormatter MSK_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter FILE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    public TransactionLogger(ExchangeCoinsPlugin plugin) {
        this.plugin = plugin;
    }


    public void enable() {
        if (enabled.getAndSet(true)) {
            return;
        }


        File logsDir = new File(plugin.getDataFolder(), "logs");
        if (!logsDir.exists()) {
            logsDir.mkdirs();
        }


        loggerThread = new Thread(this::processLogQueue, "ExchangeCoins-Logger");
        loggerThread.setDaemon(true);
        loggerThread.start();

        plugin.getLogger().info("Логгер транзакций включен");
    }


    public void disable() {
        if (!enabled.getAndSet(false)) {
            return;
        }


        processLogQueue();

        if (loggerThread != null) {
            loggerThread.interrupt();
            try {
                loggerThread.join(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        plugin.getLogger().info("Логгер транзакций отключен");
    }


    private void processLogQueue() {
        while (enabled.get() || !logQueue.isEmpty()) {
            try {
                LogEntry entry = logQueue.take();
                writeLog(entry);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                plugin.getLogger().warning("Ошибка записи лога: " + e.getMessage());
            }
        }
    }


    private void writeLog(LogEntry entry) {
        if (!plugin.getConfig().getBoolean("logging.enabled", true)) {
            return;
        }

        String fileName = "transactions_" + ZonedDateTime.now(MSK_ZONE).format(FILE_DATE_FORMATTER) + ".log";
        File logFile = new File(plugin.getDataFolder(), "logs/" + fileName);


        if (!logFile.exists()) {
            File parentDir = logFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            try {
                logFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Ошибка создания файла лога: " + e.getMessage());
                return;
            }
        }


        String logLine = formatLogLine(entry);


        try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true))) {
            writer.write(logLine);
            writer.newLine();
        } catch (IOException e) {
            plugin.getLogger().severe("Ошибка записи в лог файл: " + e.getMessage());
        }
    }


    private String formatLogLine(LogEntry entry) {
        String timestamp = ZonedDateTime.now(MSK_ZONE).format(MSK_FORMATTER);

        switch (entry.type) {
            case TRANSACTION:
                return String.format("[%s MSK] buyer:%s - seller:%s - coins:%d - price:%d",
                        timestamp,
                        entry.buyerName,
                        entry.sellerName,
                        entry.coinsAmount,
                        entry.price);

            case ORDER_EXPIRED:
                return String.format("[%s MSK] EXPIRED seller:%s - coins:%d - price:%d",
                        timestamp,
                        entry.sellerName,
                        entry.coinsAmount,
                        entry.price);

            case ORDER_CREATED:
                return String.format("[%s MSK] CREATED seller:%s - coins:%d - price:%d - id:%d",
                        timestamp,
                        entry.sellerName,
                        entry.coinsAmount,
                        entry.price,
                        entry.orderId);

            default:
                return String.format("[%s MSK] %s", timestamp, entry.message);
        }
    }


    public void logTransaction(String buyerName, String sellerName, int coinsAmount, long price) {
        if (!enabled.get()) {
            return;
        }
        logQueue.offer(new LogEntry(LogType.TRANSACTION, buyerName, sellerName, coinsAmount, price, 0, null));
    }


    public void logOrderExpired(String sellerName, int coinsAmount, long price) {
        if (!enabled.get()) {
            return;
        }
        logQueue.offer(new LogEntry(LogType.ORDER_EXPIRED, null, sellerName, coinsAmount, price, 0, null));
    }


    public void logOrderCreated(String sellerName, int coinsAmount, long price, int orderId) {
        if (!enabled.get()) {
            return;
        }
        logQueue.offer(new LogEntry(LogType.ORDER_CREATED, null, sellerName, coinsAmount, price, orderId, null));
    }


    public void logOrderDeleted(String sellerName, int coinsAmount, long price, int orderId, String adminName) {
        if (!enabled.get()) {
            return;
        }
        String message = String.format("DELETED by %s - seller:%s - coins:%d - price:%d - id:%d",
                adminName, sellerName, coinsAmount, price, orderId);
        logQueue.offer(new LogEntry(LogType.MESSAGE, null, null, 0, 0, 0, message));
    }


    public void logMessage(String message) {
        if (!enabled.get()) {
            return;
        }
        logQueue.offer(new LogEntry(LogType.MESSAGE, null, null, 0, 0, 0, message));
    }


    private enum LogType {
        TRANSACTION,
        ORDER_EXPIRED,
        ORDER_CREATED,
        MESSAGE
    }


    private static class LogEntry {
        final LogType type;
        final String buyerName;
        final String sellerName;
        final int coinsAmount;
        final long price;
        final int orderId;
        final String message;

        LogEntry(LogType type, String buyerName, String sellerName, int coinsAmount, long price, int orderId, String message) {
            this.type = type;
            this.buyerName = buyerName;
            this.sellerName = sellerName;
            this.coinsAmount = coinsAmount;
            this.price = price;
            this.orderId = orderId;
            this.message = message;
        }
    }
}
