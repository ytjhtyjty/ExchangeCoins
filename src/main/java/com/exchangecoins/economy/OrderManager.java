package com.exchangecoins.economy;

import com.exchangecoins.ExchangeCoinsPlugin;
import com.exchangecoins.database.Order;
import com.exchangecoins.database.PlayerStats;
import com.exchangecoins.database.DatabaseManager;
import com.exchangecoins.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerEvent;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class OrderManager {

    private final ExchangeCoinsPlugin plugin;
    private final Map<UUID, Long> playerCooldowns = new java.util.concurrent.ConcurrentHashMap<>();

    public OrderManager(ExchangeCoinsPlugin plugin) {
        this.plugin = plugin;
    }

    public long getRemainingCooldown(UUID playerUuid) {
        Long lastOrderTime = playerCooldowns.get(playerUuid);
        if (lastOrderTime == null) {
            return 0;
        }
        long cooldownSeconds = plugin.getConfig().getLong("orders.order_cooldown_seconds", 30);
        long elapsed = (System.currentTimeMillis() - lastOrderTime) / 1000;
        return Math.max(0, cooldownSeconds - elapsed);
    }

    public void updateCooldown(UUID playerUuid) {
        playerCooldowns.put(playerUuid, System.currentTimeMillis());
    }


    public CompletableFuture<OrderResult> createOrder(Player player, int coinsAmount, long price) {
        return CompletableFuture.supplyAsync(() -> {
            try {

                long remainingCooldown = getRemainingCooldown(player.getUniqueId());
                if (remainingCooldown > 0) {
                    return new OrderResult(OrderStatus.COOLDOWN_ACTIVE);
                }

                int minAmount = plugin.getConfig().getInt("orders.min_order_amount", 1);
                int maxAmount = plugin.getConfig().getInt("orders.max_order_amount", 1000000);
                long minPrice = plugin.getConfig().getLong("orders.min_price", 1);
                long maxPrice = plugin.getConfig().getLong("orders.max_price", 1000000000L);

                if (coinsAmount < minAmount || coinsAmount > maxAmount) {
                    plugin.getLogger().warning(String.format(
                        "Игрок %s попытался создать заказ с неверным количеством: %d (допустимо: %d-%d)",
                        player.getName(), coinsAmount, minAmount, maxAmount
                    ));
                    return new OrderResult(OrderStatus.INVALID_AMOUNT);
                }

                if (price < minPrice || price > maxPrice) {
                    plugin.getLogger().warning(String.format(
                        "Игрок %s попытался создать заказ с неверной ценой: %d (допустимо: %d-%d)",
                        player.getName(), price, minPrice, maxPrice
                    ));
                    return new OrderResult(OrderStatus.INVALID_PRICE);
                }


                int maxOrders = plugin.getBoostManager().getMaxSlotsForPlayer(player);
                int currentOrders = plugin.getDatabaseManager().getPlayerOrdersCount(player.getUniqueId()).join();

                if (currentOrders >= maxOrders) {
                    return new OrderResult(OrderStatus.MAX_ORDERS_REACHED);
                }


                int playerPoints = plugin.getPlayerPointsAPI().look(player.getUniqueId());
                if (playerPoints < coinsAmount) {
                    return new OrderResult(OrderStatus.INSUFFICIENT_COINS);
                }


                int expireDays = plugin.getBoostManager().getExpireDaysForPlayer(player);
                ZonedDateTime expiresAt = plugin.getDatabaseManager().getMSKTimePlusDays(expireDays);

                Order order = new Order(
                        player.getUniqueId(),
                        player.getName(),
                        coinsAmount,
                        price,
                        expiresAt
                );


                plugin.getPlayerPointsAPI().give(player.getUniqueId(), -coinsAmount);


                int orderId = plugin.getDatabaseManager().createOrder(order).join();

                if (orderId == -1) {

                    plugin.getPlayerPointsAPI().give(player.getUniqueId(), coinsAmount);
                    plugin.getLogger().severe("Не удалось сохранить заказ в БД для игрока " + player.getName());
                    return new OrderResult(OrderStatus.DATABASE_ERROR);
                }

                order.setId(orderId);


                plugin.getDatabaseManager().incrementOrdersCreated(player.getUniqueId().toString());

                updateCooldown(player.getUniqueId());

                return new OrderResult(OrderStatus.SUCCESS, order);
            } catch (Exception e) {
                plugin.getLogger().severe("Критическая ошибка при создании заказа: " + e.getMessage());
                e.printStackTrace();
                return new OrderResult(OrderStatus.DATABASE_ERROR);
            }
        });
    }


    public CompletableFuture<PurchaseResult> purchaseOrder(Player buyer, int orderId) {
        return CompletableFuture.supplyAsync(() -> {

            Order order = plugin.getDatabaseManager().getOrder(orderId).join();

            if (order == null) {
                return new PurchaseResult(PurchaseStatus.ORDER_NOT_FOUND);
            }

            if (!"active".equals(order.getStatus())) {
                return new PurchaseResult(PurchaseStatus.ORDER_NOT_ACTIVE);
            }

            if (order.isExpired()) {
                plugin.getDatabaseManager().deleteOrder(orderId);
                return new PurchaseResult(PurchaseStatus.ORDER_EXPIRED);
            }

            if (order.getSellerUuid().equals(buyer.getUniqueId())) {
                return new PurchaseResult(PurchaseStatus.CANNOT_BUY_OWN);
            }


            int sellerPoints = plugin.getPlayerPointsAPI().look(order.getSellerUuid());
            if (sellerPoints < order.getCoinsAmount()) {
                return new PurchaseResult(PurchaseStatus.SELLER_NO_COINS);
            }


            double commissionPercent = plugin.getConfig().getDouble("orders.commission_percent", 0);
            long commission = (long) (order.getPrice() * commissionPercent / 100);


            DatabaseManager.PurchaseTransactionResult dbResult = plugin.getDatabaseManager()
                    .executePurchase(
                            buyer.getUniqueId().toString(),
                            buyer.getName(),
                            order.getSellerUuid().toString(),
                            order.getSellerName(),
                            order.getCoinsAmount(),
                            order.getPrice(),
                            commission,
                            orderId
                    ).join();

            if (!dbResult.isSuccess()) {
                return mapDbErrorToStatus(dbResult.getErrorCode());
            }


            plugin.getPlayerPointsAPI().give(buyer.getUniqueId(), order.getCoinsAmount());
            plugin.getPlayerPointsAPI().give(order.getSellerUuid(), -order.getCoinsAmount());


            if (plugin.getConfig().getBoolean("logging.log_transactions", true)) {
                plugin.getTransactionLogger().logTransaction(
                        buyer.getName(),
                        order.getSellerName(),
                        order.getCoinsAmount(),
                        order.getPrice()
                );
            }


            sendPurchaseNotifications(buyer, order, commission);

            return new PurchaseResult(PurchaseStatus.SUCCESS, order, commission);
        });
    }


    private PurchaseResult mapDbErrorToStatus(String errorCode) {
        PurchaseStatus status = switch (errorCode) {
            case "INSUFFICIENT_FUNDS" -> PurchaseStatus.INSUFFICIENT_FUNDS;
            case "ORDER_NOT_FOUND" -> PurchaseStatus.ORDER_NOT_FOUND;
            default -> PurchaseStatus.DATABASE_ERROR;
        };
        return new PurchaseResult(status);
    }


    private void sendPurchaseNotifications(Player buyer, Order order, long commission) {
        boolean notificationsEnabled = plugin.getConfig().getBoolean("notifications.enabled", true);
        boolean notifyOnPurchase = plugin.getConfig().getBoolean("notifications.notify_on_purchase", true);

        if (!notificationsEnabled || !notifyOnPurchase) {
            return;
        }

        String prefix = ColorUtils.colorize(plugin.getConfig().getString("messages.prefix", "&8[&6ExchangeCoins&8] "));


        String purchaseMsg = plugin.getConfig().getString("messages.order_purchased",
                "&aВы купили &e%coins% &aкоинов за &e%price% &aвиджетов!");
        purchaseMsg = purchaseMsg.replace("%coins%", String.valueOf(order.getCoinsAmount()))
                .replace("%price%", String.valueOf(order.getPrice()));
        buyer.sendMessage(prefix + ColorUtils.colorize(purchaseMsg));


        Player seller = Bukkit.getPlayer(order.getSellerUuid());
        if (seller != null && !seller.equals(buyer)) {
            String soldMsg = plugin.getConfig().getString("messages.order_sold",
                    "&aВаши коины купили! Вы получили &e%price% &aвиджетов!");
            soldMsg = soldMsg.replace("%price%", String.valueOf(order.getPrice() - commission));
            seller.sendMessage(prefix + ColorUtils.colorize(soldMsg));

            if (commission > 0) {
                seller.sendMessage(prefix + ColorUtils.colorize("&7Комиссия биржи: &e" + commission + " виджетов"));
            }
        }
    }


    public CompletableFuture<List<Order>> getActiveOrders() {
        return plugin.getDatabaseManager().getActiveOrders();
    }


    public CompletableFuture<List<Order>> getPlayerOrders(UUID playerUuid) {
        return plugin.getDatabaseManager().getPlayerOrders(playerUuid);
    }


    public CompletableFuture<Void> removeExpiredOrders() {
        return plugin.getDatabaseManager().removeExpiredOrders().thenAccept(expiredOrders -> {
            if (expiredOrders.isEmpty()) {
                return;
            }

            boolean notificationsEnabled = plugin.getConfig().getBoolean("notifications.enabled", true);
            String prefix = ColorUtils.colorize(plugin.getConfig().getString("messages.prefix", "&8[&6ExchangeCoins&8] "));
            String expiredMsg = plugin.getConfig().getString("messages.order_expired",
                    "&cВаш заказ истёк! Возвращаем &e%coins% &cкоинов.");

            for (Order order : expiredOrders) {

                plugin.getPlayerPointsAPI().give(order.getSellerUuid(), order.getCoinsAmount());


                if (notificationsEnabled) {
                    Player seller = Bukkit.getPlayer(order.getSellerUuid());
                    if (seller != null) {
                        String msg = expiredMsg.replace("%coins%", String.valueOf(order.getCoinsAmount()));
                        seller.sendMessage(prefix + ColorUtils.colorize(msg));
                    }
                }


                if (plugin.getConfig().getBoolean("logging.log_orders", true)) {
                    plugin.getTransactionLogger().logOrderExpired(
                            order.getSellerName(),
                            order.getCoinsAmount(),
                            order.getPrice()
                    );
                }
            }
        });
    }


    public CompletableFuture<Boolean> cancelOrder(Player player, int orderId) {
        return plugin.getDatabaseManager().getOrder(orderId).thenApply(order -> {
            if (order == null || !order.getSellerUuid().equals(player.getUniqueId())) {
                return false;
            }

            if (!"active".equals(order.getStatus())) {
                return false;
            }


            plugin.getPlayerPointsAPI().give(player.getUniqueId(), order.getCoinsAmount());


            plugin.getDatabaseManager().deleteOrder(orderId);

            return true;
        });
    }


    public static class OrderResult {
        private final OrderStatus status;
        private final Order order;

        public OrderResult(OrderStatus status) {
            this.status = status;
            this.order = null;
        }

        public OrderResult(OrderStatus status, Order order) {
            this.status = status;
            this.order = order;
        }

        public OrderStatus getStatus() {
            return status;
        }

        public Order getOrder() {
            return order;
        }

        public boolean isSuccess() {
            return status == OrderStatus.SUCCESS;
        }
    }


    public enum OrderStatus {
        SUCCESS,
        INVALID_AMOUNT,
        INVALID_PRICE,
        MAX_ORDERS_REACHED,
        INSUFFICIENT_COINS,
        COOLDOWN_ACTIVE,
        DATABASE_ERROR
    }


    public static class PurchaseResult {
        private final PurchaseStatus status;
        private final Order order;
        private final long commission;

        public PurchaseResult(PurchaseStatus status) {
            this.status = status;
            this.order = null;
            this.commission = 0;
        }

        public PurchaseResult(PurchaseStatus status, Order order, long commission) {
            this.status = status;
            this.order = order;
            this.commission = commission;
        }

        public PurchaseStatus getStatus() {
            return status;
        }

        public Order getOrder() {
            return order;
        }

        public long getCommission() {
            return commission;
        }

        public boolean isSuccess() {
            return status == PurchaseStatus.SUCCESS;
        }
    }


    public enum PurchaseStatus {
        SUCCESS,
        ORDER_NOT_FOUND,
        ORDER_NOT_ACTIVE,
        ORDER_EXPIRED,
        INSUFFICIENT_FUNDS,
        CANNOT_BUY_OWN,
        SELLER_NO_COINS,
        DATABASE_ERROR
    }
}
