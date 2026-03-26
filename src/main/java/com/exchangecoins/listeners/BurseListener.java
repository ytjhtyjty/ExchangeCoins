package com.exchangecoins.listeners;

import com.exchangecoins.ExchangeCoinsPlugin;
import com.exchangecoins.menu.BurseMenu;
import com.exchangecoins.menu.ConfirmationMenu;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BurseListener implements Listener {

    private final ExchangeCoinsPlugin plugin;
    private final Map<UUID, BurseMenu> openMenus = new HashMap<>();

    public BurseListener(ExchangeCoinsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof org.bukkit.entity.Player)) {
            return;
        }

        org.bukkit.entity.Player player = (org.bukkit.entity.Player) event.getWhoClicked();


        if (event.getInventory().getHolder() instanceof BurseMenu) {
            event.setCancelled(true);

            BurseMenu menu = (BurseMenu) event.getInventory().getHolder();


            if (menu.getViewer().getUniqueId().equals(player.getUniqueId())) {
                menu.handleClick(event);
            }
        }


        if (event.getInventory().getHolder() instanceof ConfirmationMenu) {
            event.setCancelled(true);

            ConfirmationMenu menu = (ConfirmationMenu) event.getInventory().getHolder();


            if (menu.getBuyer().getUniqueId().equals(player.getUniqueId())) {
                menu.handleClick(event);
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof BurseMenu) {
            event.setCancelled(true);
        }
        if (event.getInventory().getHolder() instanceof ConfirmationMenu) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof BurseMenu) {
            UUID playerId = event.getPlayer().getUniqueId();
            openMenus.remove(playerId);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {

        plugin.getDatabaseManager().updateUsername(
                event.getPlayer().getUniqueId(),
                event.getPlayer().getName()
        );
    }


    public void addMenu(UUID playerId, BurseMenu menu) {
        openMenus.put(playerId, menu);
    }


    public BurseMenu getMenu(UUID playerId) {
        return openMenus.get(playerId);
    }


    public void refreshAllMenus() {
        for (BurseMenu menu : openMenus.values()) {
            if (menu != null) {
                menu.loadOrders();
            }
        }
    }


    public void refreshMenu(UUID playerId) {
        BurseMenu menu = openMenus.get(playerId);
        if (menu != null) {
            menu.loadOrders();
        }
    }
}
