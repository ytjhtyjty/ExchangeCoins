package com.exchangecoins.placeholders;

import com.exchangecoins.ExchangeCoinsPlugin;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ExchangeCoinsPlaceholder extends PlaceholderExpansion {

    private final ExchangeCoinsPlugin plugin;

    public ExchangeCoinsPlaceholder(ExchangeCoinsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "exchangecoins";
    }

    @Override
    public @NotNull String getAuthor() {
        return "ExchangeCoins Team";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player.getPlayer() == null && !player.hasPlayedBefore()) {
            return null;
        }

        String[] parts = params.toLowerCase().split("_");

        if (parts.length == 0) {
            return null;
        }

        switch (parts[0]) {
            case "widgets":
            case "widget":
                return getWidgetsPlaceholder(player);

            case "active":
                if (parts.length > 1 && parts[1].equals("orders")) {
                    return getActiveOrdersPlaceholder(player);
                }
                break;

            case "stats":
                return getStatsPlaceholder(player, parts);

            case "page":
                return getTotalPagesPlaceholder();

            case "current":
                if (parts.length > 1 && parts[1].equals("page")) {
                    return getCurrentPagePlaceholder(player);
                }
                break;
        }

        return null;
    }


    private String getWidgetsPlaceholder(OfflinePlayer player) {
        try {
            long balance = plugin.getWidgetsManager().getBalance(player.getUniqueId()).join();
            return String.valueOf(balance);
        } catch (Exception e) {
            return "0";
        }
    }


    private String getActiveOrdersPlaceholder(OfflinePlayer player) {
        try {
            int count = plugin.getDatabaseManager().getPlayerOrdersCount(player.getUniqueId()).join();
            return String.valueOf(count);
        } catch (Exception e) {
            return "0";
        }
    }


    private String getStatsPlaceholder(OfflinePlayer player, String[] parts) {
        if (parts.length < 2) {
            return null;
        }

        try {
            var stats = plugin.getDatabaseManager().getStats(player.getUniqueId().toString()).join();

            switch (parts[1]) {
                case "sold":
                    return String.valueOf(stats.getTotalSold());
                case "bought":
                    return String.valueOf(stats.getTotalBought());
                case "earned":
                    return String.valueOf(stats.getTotalEarned());
                case "spent":
                    return String.valueOf(stats.getTotalSpent());
                case "created":
                    return String.valueOf(stats.getOrdersCreated());
                case "completed":
                    return String.valueOf(stats.getOrdersCompleted());
            }
        } catch (Exception e) {
            return "0";
        }

        return null;
    }


    private String getTotalPagesPlaceholder() {
        try {
            int totalOrders = plugin.getOrderManager().getActiveOrders().join().size();
            int itemsPerPage = plugin.getConfig().getInt("menu.pagination.items_per_page", 45);
            int totalPages = Math.max(1, (int) Math.ceil((double) totalOrders / itemsPerPage));
            return String.valueOf(totalPages);
        } catch (Exception e) {
            return "1";
        }
    }


    private String getCurrentPagePlaceholder(OfflinePlayer player) {


        return "1";
    }
}
