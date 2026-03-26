package com.exchangecoins.economy;

import com.exchangecoins.ExchangeCoinsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class WidgetsManager {

    private final ExchangeCoinsPlugin plugin;

    public WidgetsManager(ExchangeCoinsPlugin plugin) {
        this.plugin = plugin;
    }


    public CompletableFuture<Long> getBalance(UUID uuid) {
        return plugin.getDatabaseManager().getWidgets(uuid.toString());
    }


    public long getBalanceSync(UUID uuid) {
        try {
            return getBalance(uuid).join();
        } catch (Exception e) {
            return plugin.getConfig().getLong("widgets.starting_balance", 0);
        }
    }


    public CompletableFuture<Void> setBalance(Player player, long amount) {
        long maxBalance = plugin.getConfig().getLong("widgets.max_balance", Long.MAX_VALUE);
        amount = Math.min(amount, maxBalance);
        amount = Math.max(amount, 0);

        return plugin.getDatabaseManager().setWidgets(
                player.getUniqueId().toString(),
                player.getName(),
                amount
        );
    }


    public CompletableFuture<Void> setBalance(UUID uuid, String username, long amount) {
        long maxBalance = plugin.getConfig().getLong("widgets.max_balance", Long.MAX_VALUE);
        amount = Math.min(amount, maxBalance);
        amount = Math.max(amount, 0);

        return plugin.getDatabaseManager().setWidgets(uuid.toString(), username, amount);
    }


    public CompletableFuture<Long> addBalance(Player player, long amount) {
        return plugin.getDatabaseManager().addWidgets(
                player.getUniqueId().toString(),
                player.getName(),
                amount
        );
    }


    public CompletableFuture<Long> addBalance(UUID uuid, String username, long amount) {
        return plugin.getDatabaseManager().addWidgets(uuid.toString(), username, amount);
    }


    public CompletableFuture<Long> removeBalance(Player player, long amount) {
        return plugin.getDatabaseManager().removeWidgets(
                player.getUniqueId().toString(),
                player.getName(),
                amount
        );
    }


    public CompletableFuture<Long> removeBalance(UUID uuid, String username, long amount) {
        return plugin.getDatabaseManager().removeWidgets(uuid.toString(), username, amount);
    }


    public CompletableFuture<Boolean> hasBalance(Player player, long amount) {
        return plugin.getDatabaseManager().hasWidgets(player.getUniqueId().toString(), amount);
    }


    public CompletableFuture<Boolean> hasBalance(UUID uuid, long amount) {
        return plugin.getDatabaseManager().hasWidgets(uuid.toString(), amount);
    }


    public CompletableFuture<Boolean> transfer(Player from, Player to, long amount) {
        return transfer(from.getUniqueId(), from.getName(), to.getUniqueId(), to.getName(), amount);
    }


    public CompletableFuture<Boolean> transfer(UUID fromUuid, String fromName,
                                                UUID toUuid, String toName, long amount) {
        return hasBalance(fromUuid, amount).thenApply(hasFunds -> {
            if (!hasFunds) {
                return false;
            }

            removeBalance(fromUuid, fromName, amount).join();
            addBalance(toUuid, toName, amount).join();

            return true;
        });
    }


    public CompletableFuture<Boolean> give(String targetName, long amount) {
        Player player = Bukkit.getPlayer(targetName);

        if (player != null) {
            return addBalance(player, amount).thenApply(newBalance -> true);
        } else {

            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(targetName);
            if (offlinePlayer.hasPlayedBefore()) {
                return addBalance(offlinePlayer.getUniqueId(), targetName, amount)
                        .thenApply(newBalance -> true);
            }
            return CompletableFuture.completedFuture(false);
        }
    }


    public CompletableFuture<Boolean> take(String targetName, long amount) {
        Player player = Bukkit.getPlayer(targetName);

        if (player != null) {
            return removeBalance(player, amount).thenApply(newBalance -> newBalance >= 0);
        } else {

            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(targetName);
            if (offlinePlayer.hasPlayedBefore()) {
                return getBalance(offlinePlayer.getUniqueId()).thenApply(balance -> {
                    if (balance < amount) {
                        return false;
                    }
                    return removeBalance(offlinePlayer.getUniqueId(), targetName, amount).join() >= 0;
                });
            }
            return CompletableFuture.completedFuture(false);
        }
    }


    public CompletableFuture<Boolean> set(String targetName, long amount) {
        Player player = Bukkit.getPlayer(targetName);

        if (player != null) {
            return setBalance(player, amount).thenApply(v -> true);
        } else {

            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(targetName);
            if (offlinePlayer.hasPlayedBefore()) {
                return setBalance(offlinePlayer.getUniqueId(), targetName, amount)
                        .thenApply(v -> true);
            }
            return CompletableFuture.completedFuture(false);
        }
    }


    public CompletableFuture<Long> getBalanceByName(String targetName) {
        Player player = Bukkit.getPlayer(targetName);

        if (player != null) {
            return getBalance(player.getUniqueId());
        } else {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(targetName);
            if (offlinePlayer.hasPlayedBefore()) {
                return getBalance(offlinePlayer.getUniqueId());
            }
            return CompletableFuture.completedFuture(-1L);
        }
    }
}
