package com.exchangecoins.economy;

import com.exchangecoins.ExchangeCoinsPlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BoostManager {

    private final ExchangeCoinsPlugin plugin;
    private final Map<String, BoostConfig> boostConfigs = new HashMap<>();
    private final Map<UUID, String> playerActiveBoosts = new ConcurrentHashMap<>();

    public BoostManager(ExchangeCoinsPlugin plugin) {
        this.plugin = plugin;
        loadBoostConfigs();
        registerBoostPermissions();
    }


    private void loadBoostConfigs() {
        ConfigurationSection boostsSection = plugin.getConfig().getConfigurationSection("boosts");
        if (boostsSection == null) {
            plugin.getLogger().info("Секция boosts не найдена в конфиге");
            return;
        }

        for (String boostName : boostsSection.getKeys(false)) {
            ConfigurationSection boostSection = boostsSection.getConfigurationSection(boostName);
            if (boostSection != null) {
                String permission = boostSection.getString("permission", "");
                int maxSlots = boostSection.getInt("max-slots", 0);
                int expireDays = boostSection.getInt("expire_days", 6);

                boostConfigs.put(boostName, new BoostConfig(boostName, permission, maxSlots, expireDays));
                plugin.getLogger().info("Загружен буст: " + boostName + " (слотов: " + maxSlots + ", дней: " + expireDays + ")");
            }
        }

        plugin.getLogger().info("Загружено бустов: " + boostConfigs.size());
    }


    private void registerBoostPermissions() {
        for (BoostConfig config : boostConfigs.values()) {
            String permissionName = config.getPermission();
            if (permissionName != null && !permissionName.isEmpty()) {
                try {
                    Permission permission = new Permission(permissionName, "Буст: " + config.getName(), PermissionDefault.FALSE);
                    plugin.getServer().getPluginManager().addPermission(permission);
                    plugin.getLogger().fine("Зарегистрировано право: " + permissionName);
                } catch (IllegalArgumentException e) {

                    plugin.getLogger().fine("Право уже существует: " + permissionName);
                }
            }
        }
    }


    public void reloadBoosts() {
        boostConfigs.clear();
        loadBoostConfigs();


        for (BoostConfig config : boostConfigs.values()) {
            String permissionName = config.getPermission();
            if (permissionName != null && !permissionName.isEmpty()) {
                try {
                    Permission permission = new Permission(permissionName, "Буст: " + config.getName(), PermissionDefault.FALSE);
                    plugin.getServer().getPluginManager().addPermission(permission);
                } catch (IllegalArgumentException e) {

                }
            }
        }
    }


    public BoostConfig getBestAvailableBoost(Player player) {
        BoostConfig best = null;

        for (BoostConfig config : boostConfigs.values()) {

            if (config.getPermission() == null || config.getPermission().isEmpty() ||
                player.hasPermission(config.getPermission())) {


                if (best == null || config.getMaxSlots() > best.getMaxSlots()) {
                    best = config;
                }
            }
        }

        return best;
    }


    public BoostConfig getBoostByName(String name) {
        return boostConfigs.get(name);
    }


    public int getMaxSlotsForPlayer(Player player) {
        BoostConfig boost = getBestAvailableBoost(player);
        if (boost != null) {
            return boost.getMaxSlots();
        }

        return plugin.getConfig().getInt("orders.max_orders_per_player", 5);
    }


    public int getExpireDaysForPlayer(Player player) {
        BoostConfig boost = getBestAvailableBoost(player);
        if (boost != null) {
            return boost.getExpireDays();
        }

        return plugin.getConfig().getInt("orders.default_expire_days", 6);
    }


    public void activateBoost(UUID playerUuid, String boostName) {
        playerActiveBoosts.put(playerUuid, boostName);
    }


    public String getPlayerActiveBoost(UUID playerUuid) {
        return playerActiveBoosts.get(playerUuid);
    }


    public void deactivateBoost(UUID playerUuid) {
        playerActiveBoosts.remove(playerUuid);
    }


    public Collection<BoostConfig> getAllBoosts() {
        return boostConfigs.values();
    }


    public static class BoostConfig {
        private final String name;
        private final String permission;
        private final int maxSlots;
        private final int expireDays;

        public BoostConfig(String name, String permission, int maxSlots, int expireDays) {
            this.name = name;
            this.permission = permission;
            this.maxSlots = maxSlots;
            this.expireDays = expireDays;
        }

        public String getName() {
            return name;
        }

        public String getPermission() {
            return permission;
        }

        public int getMaxSlots() {
            return maxSlots;
        }

        public int getExpireDays() {
            return expireDays;
        }

        @Override
        public String toString() {
            return "BoostConfig{" +
                    "name='" + name + '\'' +
                    ", maxSlots=" + maxSlots +
                    ", expireDays=" + expireDays +
                    '}';
        }
    }
}
