package com.exchangecoins.menu;

import com.exchangecoins.ExchangeCoinsPlugin;
import com.exchangecoins.database.Order;
import com.exchangecoins.economy.OrderManager;
import com.exchangecoins.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.*;

public class ConfirmationMenu implements InventoryHolder {

    private final ExchangeCoinsPlugin plugin;
    private final Player buyer;
    private final Order order;
    private Inventory inventory;
    private static FileConfiguration confirmationConfig;


    private String menuTitle;
    private int menuSize;
    private Material cancelMaterial;
    private String cancelName;
    private List<String> cancelLore;
    private Set<Integer> cancelSlots;
    private Material itemMaterial;
    private String itemName;
    private List<String> itemLore;
    private int itemSlot;
    private Material confirmMaterial;
    private String confirmName;
    private List<String> confirmLore;
    private Set<Integer> confirmSlots;
    private Material fillerMaterial;
    private String fillerName;

    public ConfirmationMenu(ExchangeCoinsPlugin plugin, Player buyer, Order order) {
        this.plugin = plugin;
        this.buyer = buyer;
        this.order = order;
        loadConfirmationConfig(plugin);
        loadConfig();
        createInventory();
    }


    private static void loadConfirmationConfig(ExchangeCoinsPlugin plugin) {
        if (confirmationConfig != null) {
            return;
        }

        File confirmationFile = new File(plugin.getDataFolder(), "confirmation.yml");
        if (!confirmationFile.exists()) {
            plugin.saveResource("confirmation.yml", false);
        }

        confirmationConfig = YamlConfiguration.loadConfiguration(confirmationFile);
    }


    public static void reloadConfirmationConfig(ExchangeCoinsPlugin plugin) {
        File confirmationFile = new File(plugin.getDataFolder(), "confirmation.yml");
        confirmationConfig = YamlConfiguration.loadConfiguration(confirmationFile);
    }


    private void loadConfig() {
        menuTitle = ColorUtils.colorize(confirmationConfig.getString("menu.title", "&6&lПодтверждение покупки"));
        menuSize = confirmationConfig.getInt("menu.size", 27);

        ConfigurationSection itemsSection = confirmationConfig.getConfigurationSection("items");
        if (itemsSection != null) {
            ConfigurationSection cancelSection = itemsSection.getConfigurationSection("cancel");
            if (cancelSection != null) {
                cancelMaterial = Material.valueOf(cancelSection.getString("material", "RED_STAINED_GLASS_PANE").toUpperCase());
                cancelName = ColorUtils.colorize(cancelSection.getString("name", "&c&l✖ Отменить"));
                cancelLore = cancelSection.getStringList("lore");
                cancelSlots = parseSlots(cancelSection.getStringList("slots"));
            } else {
                cancelMaterial = Material.RED_STAINED_GLASS_PANE;
                cancelName = "&c&l✖ Отменить";
                cancelLore = Arrays.asList("&7Нажмите, чтобы отменить покупку", "", "&cОтмена");
                cancelSlots = new HashSet<>(Arrays.asList(0, 1, 2, 9, 10, 11, 18, 19, 20));
            }

            ConfigurationSection itemSection = itemsSection.getConfigurationSection("item");
            if (itemSection != null) {
                itemMaterial = Material.valueOf(itemSection.getString("material", "GOLD_INGOT").toUpperCase());
                itemName = ColorUtils.colorize(itemSection.getString("name", "&6&lЗаказ #%order_id%"));
                itemLore = itemSection.getStringList("lore");
                itemSlot = parseSlot(itemSection.getString("slot", "13"));
            } else {
                itemMaterial = Material.GOLD_INGOT;
                itemName = "&6&lЗаказ #%order_id%";
                itemLore = Arrays.asList("", "&7Продавец: &f%seller%", "&7Коины: &e%coins_amount%", "&7Цена: &e%price% виджетов", "", "&a&l■ &aНажмите для подтверждения");
                itemSlot = 13;
            }

            ConfigurationSection confirmSection = itemsSection.getConfigurationSection("confirm");
            if (confirmSection != null) {
                confirmMaterial = Material.valueOf(confirmSection.getString("material", "LIME_STAINED_GLASS_PANE").toUpperCase());
                confirmName = ColorUtils.colorize(confirmSection.getString("name", "&a&l✔ Подтвердить"));
                confirmLore = confirmSection.getStringList("lore");
                confirmSlots = parseSlots(confirmSection.getStringList("slots"));
            } else {
                confirmMaterial = Material.LIME_STAINED_GLASS_PANE;
                confirmName = "&a&l✔ Подтвердить";
                confirmLore = Arrays.asList("&7Нажмите, чтобы подтвердить покупку", "", "&aЦена: &e%price% виджетов", "&aКоины: &e%coins_amount%");
                confirmSlots = new HashSet<>(Arrays.asList(6, 7, 8, 15, 16, 17, 24, 25, 26));
            }

            ConfigurationSection fillerSection = itemsSection.getConfigurationSection("filler");
            if (fillerSection != null) {
                fillerMaterial = Material.valueOf(fillerSection.getString("material", "GRAY_STAINED_GLASS_PANE").toUpperCase());
                fillerName = ColorUtils.colorize(fillerSection.getString("name", " "));
            } else {
                fillerMaterial = Material.GRAY_STAINED_GLASS_PANE;
                fillerName = " ";
            }
        } else {
            menuTitle = "&6&lПодтверждение покупки";
            menuSize = 27;
            cancelMaterial = Material.RED_STAINED_GLASS_PANE;
            cancelName = "&c&l✖ Отменить";
            cancelLore = Arrays.asList("&7Нажмите, чтобы отменить покупку", "", "&cОтмена");
            cancelSlots = new HashSet<>(Arrays.asList(0, 1, 2, 9, 10, 11, 18, 19, 20));
            itemMaterial = Material.GOLD_INGOT;
            itemName = "&6&lЗаказ #%order_id%";
            itemLore = Arrays.asList("", "&7Продавец: &f%seller%", "&7Коины: &e%coins_amount%", "&7Цена: &e%price% виджетов", "", "&a&l■ &aНажмите для подтверждения");
            itemSlot = 13;
            confirmMaterial = Material.LIME_STAINED_GLASS_PANE;
            confirmName = "&a&l✔ Подтвердить";
            confirmLore = Arrays.asList("&7Нажмите, чтобы подтвердить покупку", "", "&aЦена: &e%price% виджетов", "&aКоины: &e%coins_amount%");
            confirmSlots = new HashSet<>(Arrays.asList(6, 7, 8, 15, 16, 17, 24, 25, 26));
            fillerMaterial = Material.GRAY_STAINED_GLASS_PANE;
            fillerName = " ";
        }
    }


    private Set<Integer> parseSlots(List<String> slotStrings) {
        Set<Integer> slots = new HashSet<>();
        for (String slotStr : slotStrings) {
            slots.add(parseSlot(slotStr));
        }
        return slots;
    }


    private int parseSlot(String slotStr) {
        if (slotStr == null || slotStr.isEmpty()) {
            return 13;
        }

        if (slotStr.contains("_")) {
            try {
                String[] parts = slotStr.split("_");
                int row = Integer.parseInt(parts[0]);
                int col = Integer.parseInt(parts[1]);
                return row * 9 + col;
            } catch (NumberFormatException e) {

            }
        }

        try {
            return Integer.parseInt(slotStr.trim());
        } catch (NumberFormatException e) {
            return 13;
        }
    }

    private void createInventory() {
        inventory = Bukkit.createInventory(this, menuSize, menuTitle);
        fillFiller();

        ItemStack cancelItem = createConfirmationItem(cancelMaterial, cancelName, cancelLore);
        for (int slot : cancelSlots) {
            if (slot >= 0 && slot < menuSize) {
                inventory.setItem(slot, cancelItem);
            }
        }

        ItemStack item = createOrderItem();
        inventory.setItem(itemSlot, item);

        ItemStack confirmItem = createConfirmationItem(confirmMaterial, confirmName, confirmLore);
        for (int slot : confirmSlots) {
            if (slot >= 0 && slot < menuSize) {
                inventory.setItem(slot, confirmItem);
            }
        }
    }

    private void fillFiller() {
        ItemStack filler = createFillerItem();
        for (int i = 0; i < menuSize; i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, filler);
            }
        }
    }

    private ItemStack createConfirmationItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtils.colorize(name));
            List<String> coloredLore = new ArrayList<>();
            for (String line : lore) {
                coloredLore.add(ColorUtils.colorize(line));
            }
            meta.setLore(coloredLore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createFillerItem() {
        ItemStack item = new ItemStack(fillerMaterial);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtils.colorize(fillerName));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createOrderItem() {
        ItemStack item = new ItemStack(itemMaterial);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String name = itemName.replace("%order_id%", String.valueOf(order.getId()));
            meta.setDisplayName(ColorUtils.colorize(name));

            List<String> lore = new ArrayList<>();
            for (String line : itemLore) {
                lore.add(ColorUtils.colorize(line
                        .replace("%seller%", order.getSellerName())
                        .replace("%coins_amount%", String.valueOf(order.getCoinsAmount()))
                        .replace("%price%", String.valueOf(order.getPrice()))
                ));
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    public void open() {
        buyer.openInventory(inventory);
    }

    public void handleClick(InventoryClickEvent event) {
        int slot = event.getSlot();

        if (cancelSlots.contains(slot)) {
            buyer.closeInventory();
            String prefix = ColorUtils.colorize(plugin.getConfig().getString("messages.prefix", "&8[&6ExchangeCoins&8] "));
            String cancelledMsg = confirmationConfig.getString("messages.purchase_cancelled", "&c✖ Покупка отменена.");
            buyer.sendMessage(prefix + ColorUtils.colorize(cancelledMsg));
            event.setCancelled(true);
            return;
        }

        if (confirmSlots.contains(slot)) {
            buyer.closeInventory();
            confirmPurchase();
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true);
    }

    private void confirmPurchase() {
        String prefix = ColorUtils.colorize(plugin.getConfig().getString("messages.prefix", "&8[&6ExchangeCoins&8] "));

        plugin.getOrderManager().purchaseOrder(buyer, order.getId()).thenAccept(result -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (result.isSuccess()) {
                    String confirmedMsg = confirmationConfig.getString("messages.purchase_confirmed", "&a✓ Заказ #%order_id% успешно куплен!");
                    String commission = result.getCommission() > 0 ?
                            " &7(Комиссия: &e" + result.getCommission() + "&7)" : "";
                    buyer.sendMessage(prefix + ColorUtils.colorize(confirmedMsg.replace("%order_id%", String.valueOf(order.getId())) + commission));
                } else {
                    String failedMsg = confirmationConfig.getString("messages.purchase_failed", "&c✖ Не удалось купить заказ: %error%");
                    buyer.sendMessage(prefix + ColorUtils.colorize(failedMsg.replace("%error%", getErrorMessage(result.getStatus()))));
                }
            });
        });
    }

    private String getErrorMessage(OrderManager.PurchaseStatus status) {
        switch (status) {
            case INSUFFICIENT_FUNDS: return "&cНедостаточно виджетов!";
            case ORDER_NOT_FOUND:
            case ORDER_NOT_ACTIVE:
            case ORDER_EXPIRED: return "&cЭтот заказ больше недоступен!";
            case CANNOT_BUY_OWN: return "&cВы не можете купить свой собственный заказ!";
            case SELLER_NO_COINS: return "&cУ продавца больше нет коинов!";
            default: return "&cПроизошла ошибка!";
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public Player getBuyer() {
        return buyer;
    }

    public Order getOrder() {
        return order;
    }
}
