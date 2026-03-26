package com.exchangecoins.menu;

import com.exchangecoins.ExchangeCoinsPlugin;
import com.exchangecoins.database.Order;
import com.exchangecoins.economy.OrderManager;
import com.exchangecoins.utils.ColorUtils;
import com.exchangecoins.utils.PlaceholderUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class BurseMenu implements InventoryHolder {

    private final ExchangeCoinsPlugin plugin;
    private final Player viewer;
    private Inventory inventory;
    private final Map<Integer, Order> orderSlots = new HashMap<>();
    private final Set<Integer> fillerSlots = new HashSet<>();


    private ItemStack cachedFiller;
    private ItemStack cachedPreviousPage;
    private ItemStack cachedNextPage;
    private ItemStack cachedRefresh;


    private int currentPage = 0;
    private int totalPages = 1;
    private int itemsPerPage;

    private String title;
    private int size;
    private Material orderMaterial;
    private String orderName;
    private List<String> orderLore;
    private Material fillerMaterial;
    private String fillerName;


    private Material previousPageMaterial;
    private String previousPageName;
    private String previousPageDisabledName;
    private Material nextPageMaterial;
    private String nextPageName;
    private String nextPageDisabledName;
    private Material refreshMaterial;
    private String refreshName;
    private int previousPageSlot;
    private int nextPageSlot;
    private int refreshSlot;

    public BurseMenu(ExchangeCoinsPlugin plugin, Player viewer) {
        this.plugin = plugin;
        this.viewer = viewer;
        loadConfig();
    }

    private void loadConfig() {
        ConfigurationSection menuSection = plugin.getConfig().getConfigurationSection("menu");

        title = ColorUtils.colorize(PlaceholderUtils.getString(menuSection, "title", "&6&lБиржа Коинов"));
        size = PlaceholderUtils.getInt(menuSection, "size", 54);


        ConfigurationSection paginationSection = menuSection.getConfigurationSection("pagination");
        if (paginationSection != null) {
            itemsPerPage = paginationSection.getInt("items_per_page", 45);
        } else {
            itemsPerPage = 45;
        }

        ConfigurationSection itemsSection = menuSection.getConfigurationSection("items");
        if (itemsSection != null) {
            ConfigurationSection orderSection = itemsSection.getConfigurationSection("order_slot");
            if (orderSection != null) {
                orderMaterial = PlaceholderUtils.getMaterial(
                        orderSection.getString("material", "GOLD_INGOT"),
                        Material.GOLD_INGOT
                );
                orderName = ColorUtils.colorize(orderSection.getString("name", "&eЗаказ #&6%order_id%"));
                orderLore = PlaceholderUtils.getStringList(orderSection, "lore");
            }

            ConfigurationSection fillerSection = itemsSection.getConfigurationSection("filler");
            if (fillerSection != null) {
                fillerMaterial = PlaceholderUtils.getMaterial(
                        fillerSection.getString("material", "GRAY_STAINED_GLASS_PANE"),
                        Material.GRAY_STAINED_GLASS_PANE
                );
                fillerName = ColorUtils.colorize(fillerSection.getString("name", " "));

                if (fillerSection.contains("slots")) {
                    List<?> slotsList = fillerSection.getList("slots");
                    if (slotsList != null) {
                        for (Object slot : slotsList) {
                            if (slot instanceof Number) {
                                fillerSlots.add(((Number) slot).intValue());
                            }
                        }
                    }
                }
            }


            loadNavigationButton(itemsSection, "previous_page", () -> {
                previousPageMaterial = Material.ARROW;
                previousPageName = "&e&l← Предыдущая";
                previousPageSlot = 48;
            });

            loadNavigationButton(itemsSection, "refresh", () -> {
                refreshMaterial = Material.CLOCK;
                refreshName = "&e&lОбновить";
                refreshSlot = 49;
            });

            loadNavigationButton(itemsSection, "next_page", () -> {
                nextPageMaterial = Material.ARROW;
                nextPageName = "&e&lСледующая →";
                nextPageSlot = 50;
            });
        }
    }

    private void loadNavigationButton(ConfigurationSection itemsSection, String buttonName, Runnable defaults) {
        ConfigurationSection buttonSection = itemsSection.getConfigurationSection(buttonName);
        if (buttonSection != null) {
            String materialName = buttonSection.getString("material", "");
            int slot = buttonSection.getInt("slot", 0);
            String name = buttonSection.getString("name", "");
            String disabledName = buttonSection.getString("disabled_name", "");

            switch (buttonName) {
                case "previous_page":
                    previousPageMaterial = PlaceholderUtils.getMaterial(materialName, Material.ARROW);
                    previousPageName = ColorUtils.colorize(name.isEmpty() ? "&e&l← Предыдущая" : name);
                    previousPageDisabledName = ColorUtils.colorize(disabledName.isEmpty() ? "&7&l← Предыдущая" : disabledName);
                    previousPageSlot = slot == 0 ? 48 : slot;
                    break;
                case "refresh":
                    refreshMaterial = PlaceholderUtils.getMaterial(materialName, Material.CLOCK);
                    refreshName = ColorUtils.colorize(name.isEmpty() ? "&e&lОбновить" : name);
                    refreshSlot = slot == 0 ? 49 : slot;
                    break;
                case "next_page":
                    nextPageMaterial = PlaceholderUtils.getMaterial(materialName, Material.ARROW);
                    nextPageName = ColorUtils.colorize(name.isEmpty() ? "&e&lСледующая →" : name);
                    nextPageDisabledName = ColorUtils.colorize(disabledName.isEmpty() ? "&7&lСледующая" : disabledName);
                    nextPageSlot = slot == 0 ? 50 : slot;
                    break;
            }
        } else {
            defaults.run();
        }
    }

    public void open() {
        createInventory();
        loadOrdersAndOpen();
    }

    public void loadOrders() {
        if (inventory == null) {
            createInventory();
        }
        loadOrdersAndOpen();
    }

    public void refresh() {
        currentPage = 0;
        loadOrders();
    }

    public void nextPage() {
        if (currentPage < totalPages - 1) {
            currentPage++;
            loadOrders();
        }
    }

    public void previousPage() {
        if (currentPage > 0) {
            currentPage--;
            loadOrders();
        }
    }

    private void createInventory() {
        inventory = Bukkit.createInventory(this, size, title);

        cachedFiller = null;
        cachedPreviousPage = null;
        cachedNextPage = null;
        cachedRefresh = null;
        fillFillerSlots();
        fillNavigationButtons();
    }

    private void fillFillerSlots() {
        if (fillerMaterial == null || fillerMaterial == Material.AIR) {
            return;
        }


        if (cachedFiller == null) {
            cachedFiller = createFiller();
        }

        if (!fillerSlots.isEmpty()) {
            for (int slot : fillerSlots) {
                if (slot >= 0 && slot < size) {
                    inventory.setItem(slot, cachedFiller);
                }
            }
        } else {
            for (int i = 0; i < size; i++) {
                inventory.setItem(i, cachedFiller);
            }
        }
    }

    private void fillNavigationButtons() {

        if (cachedPreviousPage == null) {
            cachedPreviousPage = createNavigationItem(previousPageMaterial, previousPageName);
        }
        if (cachedNextPage == null) {
            cachedNextPage = createNavigationItem(nextPageMaterial, nextPageName);
        }
        if (cachedRefresh == null) {
            cachedRefresh = createNavigationItem(refreshMaterial, refreshName);
        }

        inventory.setItem(previousPageSlot, cachedPreviousPage);
        inventory.setItem(refreshSlot, cachedRefresh);
        inventory.setItem(nextPageSlot, cachedNextPage);
    }

    private ItemStack createNavigationItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createFiller() {
        ItemStack item = new ItemStack(fillerMaterial);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(fillerName);
            item.setItemMeta(meta);
        }
        return item;
    }

    private void loadOrdersAndOpen() {
        plugin.getOrderManager().getActiveOrders().thenAccept(orders -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                try {
                    orderSlots.clear();


                    clearOrderSlots();


                    int actualOrders = orders != null ? orders.size() : 0;
                    totalPages = Math.max(1, (int) Math.ceil((double) actualOrders / itemsPerPage));

                    if (currentPage >= totalPages) {
                        currentPage = totalPages - 1;
                    }

                    if (orders != null && !orders.isEmpty()) {
                        int fromIndex = currentPage * itemsPerPage;
                        int toIndex = Math.min(fromIndex + itemsPerPage, orders.size());

                        if (fromIndex < orders.size()) {
                            List<Order> pageOrders = orders.subList(fromIndex, toIndex);
                            placeOrdersOnPage(pageOrders);
                        }
                    }


                    updateNavigationButtons();

                    viewer.openInventory(inventory);
                } catch (Exception e) {
                    plugin.getLogger().severe("Ошибка загрузки заказов: " + e.getMessage());
                    e.printStackTrace();
                    viewer.sendMessage(ColorUtils.colorize("&cПроизошла ошибка при загрузке заказов!"));
                    viewer.openInventory(inventory);
                }
            });
        }).exceptionally(ex -> {
            plugin.getLogger().severe("Ошибка получения заказов: " + ex.getMessage());
            ex.printStackTrace();
            Bukkit.getScheduler().runTask(plugin, () -> {
                viewer.sendMessage(ColorUtils.colorize("&cПроизошла ошибка при загрузке заказов!"));
                viewer.openInventory(inventory);
            });
            return null;
        });
    }

    private void placeOrdersOnPage(List<Order> pageOrders) {
        int slot = 0;
        for (Order order : pageOrders) {
            while (slot < size && (fillerSlots.contains(slot) || isNavigationSlot(slot))) {
                slot++;
            }

            if (slot >= size) {
                break;
            }

            ItemStack orderItem = createOrderItem(order);
            inventory.setItem(slot, orderItem);
            orderSlots.put(slot, order);
            slot++;
        }
    }

    private void clearOrderSlots() {
        for (int i = 0; i < size; i++) {
            if (!fillerSlots.contains(i) && !isNavigationSlot(i)) {
                inventory.setItem(i, null);
            }
        }
    }

    private boolean isNavigationSlot(int slot) {
        return slot == previousPageSlot || slot == nextPageSlot || slot == refreshSlot;
    }

    private void updateNavigationButtons() {


        String prevName = currentPage > 0 ? previousPageName : previousPageDisabledName;
        ItemStack prevItem = createNavigationItem(previousPageMaterial, prevName);
        inventory.setItem(previousPageSlot, prevItem);


        String nextName = currentPage < totalPages - 1 ? nextPageName : nextPageDisabledName;
        ItemStack nextItem = createNavigationItem(nextPageMaterial, nextName);
        inventory.setItem(nextPageSlot, nextItem);


        String refreshNameWithPlaceholders = refreshName
                .replace("%page%", String.valueOf(currentPage + 1))
                .replace("%total_pages%", String.valueOf(totalPages));
        ItemStack refreshItem = createNavigationItem(refreshMaterial, refreshNameWithPlaceholders);
        inventory.setItem(refreshSlot, refreshItem);
    }

    private ItemStack createOrderItem(Order order) {
        ItemStack item = new ItemStack(orderMaterial);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            String name = orderName.replace("%order_id%", String.valueOf(order.getId()));
            meta.setDisplayName(name);

            List<String> lore = new ArrayList<>();
            for (String line : orderLore) {
                lore.add(ColorUtils.colorize(line
                        .replace("%seller%", order.getSellerName())
                        .replace("%coins_amount%", String.valueOf(order.getCoinsAmount()))
                        .replace("%price%", String.valueOf(order.getPrice()))
                        .replace("%time_left%", order.getFormattedTimeLeft())
                ));
            }
            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }

        return item;
    }

    public void handleClick(InventoryClickEvent event) {
        int slot = event.getSlot();


        if (slot == previousPageSlot) {
            previousPage();
            event.setCancelled(true);
            return;
        }

        if (slot == nextPageSlot) {
            nextPage();
            event.setCancelled(true);
            return;
        }

        if (slot == refreshSlot) {
            refresh();
            event.setCancelled(true);
            return;
        }


        if (!orderSlots.containsKey(slot)) {
            return;
        }

        Order order = orderSlots.get(slot);
        if (order == null) {
            return;
        }


        ConfirmationMenu confirmationMenu = new ConfirmationMenu(plugin, viewer, order);
        confirmationMenu.open();

        event.setCancelled(true);
    }

    private void sendMessage(OrderManager.PurchaseStatus status) {
        String prefix = ColorUtils.colorize(plugin.getConfig().getString("messages.prefix", "&8[&6ExchangeCoins&8] "));
        String message;

        switch (status) {
            case INSUFFICIENT_FUNDS:
                message = plugin.getConfig().getString("messages.insufficient_funds", "&cНедостаточно виджетов!");
                break;
            case ORDER_NOT_FOUND:
            case ORDER_NOT_ACTIVE:
            case ORDER_EXPIRED:
                message = "&cЭтот заказ больше недоступен!";
                break;
            case CANNOT_BUY_OWN:
                message = "&cВы не можете купить свой собственный заказ!";
                break;
            case SELLER_NO_COINS:
                message = "&cУ продавца больше нет коинов!";
                break;
            default:
                message = plugin.getConfig().getString("messages.unknown_command", "&cПроизошла ошибка!");
        }

        viewer.sendMessage(prefix + ColorUtils.colorize(message));
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public Player getViewer() {
        return viewer;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public int getTotalPages() {
        return totalPages;
    }

    private static List<String> colorize(List<String> lines) {
        if (lines == null) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>();
        for (String line : lines) {
            result.add(ColorUtils.colorize(line));
        }
        return result;
    }
}
