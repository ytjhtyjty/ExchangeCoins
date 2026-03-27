package com.exchangecoins.commands;

import com.exchangecoins.ExchangeCoinsPlugin;
import com.exchangecoins.database.PlayerStats;
import com.exchangecoins.economy.OrderManager;
import com.exchangecoins.menu.ConfirmationMenu;
import com.exchangecoins.utils.ColorUtils;
import com.exchangecoins.utils.PlaceholderUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class EcoinsCommand implements CommandExecutor, TabCompleter {

    private final ExchangeCoinsPlugin plugin;
    private final Set<String> subcommands = new HashSet<>(Arrays.asList(
            "order", "balance", "give", "take", "set", "reload", "stats", "help", "cancel", "delete", "myorders"
    ));

    public EcoinsCommand(ExchangeCoinsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String prefix = ColorUtils.colorize(plugin.getConfig().getString("messages.prefix", "&8[&6ExchangeCoins&8] "));

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subcommand = args[0].toLowerCase();

        switch (subcommand) {
            case "order":
                handleOrder(sender, args, prefix);
                break;
            case "balance":
                handleBalance(sender, args, prefix);
                break;
            case "give":
                handleGive(sender, args, prefix);
                break;
            case "take":
                handleTake(sender, args, prefix);
                break;
            case "set":
                handleSet(sender, args, prefix);
                break;
            case "reload":
                handleReload(sender, prefix);
                break;
            case "stats":
                handleStats(sender, args, prefix);
                break;
            case "help":
                sendHelp(sender);
                break;
            case "cancel":
                handleCancel(sender, args, prefix);
                break;
            case "delete":
                handleDelete(sender, args, prefix);
                break;
            case "myorders":
                handleMyOrders(sender, args, prefix);
                break;
            default:
                String unknownMsg = plugin.getConfig().getString("messages.unknown_command", "&cНеизвестная команда!");
                sender.sendMessage(prefix + ColorUtils.colorize(unknownMsg));
                sendHelp(sender);
        }

        return true;
    }

    private void handleOrder(CommandSender sender, String[] args, String prefix) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ColorUtils.colorize("&cЭту команду может использовать только игрок!"));
            return;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("exchangecoins.order")) {
            String noPermMsg = plugin.getConfig().getString("messages.no_permission", "&cУ вас нет прав на это!");
            player.sendMessage(prefix + ColorUtils.colorize(noPermMsg));
            return;
        }

        if (args.length < 3) {
            player.sendMessage(prefix + ColorUtils.colorize("&cИспользование: &e/ecoins order <количество> <цена>"));
            return;
        }

        int coinsAmount;
        long price;

        try {
            coinsAmount = Integer.parseInt(args[1]);
            price = Long.parseLong(args[2]);
        } catch (NumberFormatException e) {
            String invalidMsg = plugin.getConfig().getString("messages.invalid_amount", "&cНекорректное количество!");
            player.sendMessage(prefix + ColorUtils.colorize(invalidMsg));
            return;
        }

        plugin.getOrderManager().createOrder(player, coinsAmount, price).thenAccept(result -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (result.isSuccess()) {
                    String msg = plugin.getConfig().getString("messages.order_created", "&aВаш заказ создан! ID: &e%id%");
                    msg = msg.replace("%id%", String.valueOf(result.getOrder().getId()));
                    player.sendMessage(prefix + ColorUtils.colorize(msg));


                    if (plugin.getConfig().getBoolean("logging.log_orders", true)) {
                        plugin.getTransactionLogger().logOrderCreated(
                                player.getName(),
                                coinsAmount,
                                price,
                                result.getOrder().getId()
                        );
                    }
                } else {
                    sendOrderError(player, result.getStatus(), prefix);
                }
            });
        });
    }

    private void handleBalance(CommandSender sender, String[] args, String prefix) {
        if (!sender.hasPermission("exchangecoins.balance")) {
            String noPermMsg = plugin.getConfig().getString("messages.no_permission", "&cУ вас нет прав на это!");
            sender.sendMessage(prefix + ColorUtils.colorize(noPermMsg));
            return;
        }

        String targetName;

        if (args.length > 1) {
            if (!sender.hasPermission("exchangecoins.admin")) {
                String noPermMsg = plugin.getConfig().getString("messages.no_permission", "&cУ вас нет прав на это!");
                sender.sendMessage(prefix + ColorUtils.colorize(noPermMsg));
                return;
            }
            targetName = args[1];
        } else {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ColorUtils.colorize("&cИспользуйте: &e/ecoins balance <ник>"));
                return;
            }
            targetName = ((Player) sender).getName();
        }

        plugin.getWidgetsManager().getBalanceByName(targetName).thenAccept(balance -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (balance < 0) {
                    String notFoundMsg = plugin.getConfig().getString("messages.player_not_found", "&cИгрок не найден!");
                    sender.sendMessage(prefix + ColorUtils.colorize(notFoundMsg));
                    return;
                }

                String msg = plugin.getConfig().getString("messages.balance_info", "&7Баланс &f%player%&7: &e%balance% виджетов");
                msg = msg.replace("%player%", targetName)
                        .replace("%balance%", PlaceholderUtils.formatNumber(balance));
                sender.sendMessage(prefix + ColorUtils.colorize(msg));
            });
        });
    }

    private void handleGive(CommandSender sender, String[] args, String prefix) {
        if (!sender.hasPermission("exchangecoins.give")) {
            String noPermMsg = plugin.getConfig().getString("messages.no_permission", "&cУ вас нет прав на это!");
            sender.sendMessage(prefix + ColorUtils.colorize(noPermMsg));
            return;
        }

        if (args.length < 3) {
            sender.sendMessage(prefix + ColorUtils.colorize("&cИспользование: &e/ecoins give <ник> <сумма>"));
            return;
        }

        String targetName = args[1];
        long amount;

        try {
            amount = Long.parseLong(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(prefix + ColorUtils.colorize("&cНекорректная сумма!"));
            return;
        }

        if (amount <= 0) {
            sender.sendMessage(prefix + ColorUtils.colorize("&cСумма должна быть больше 0!"));
            return;
        }

        plugin.getWidgetsManager().give(targetName, amount).thenAccept(success -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (success) {
                    String msg = plugin.getConfig().getString("messages.give_success", "&aВы выдали &e%amount% &aвиджетов игроку &e%player%");
                    msg = msg.replace("%amount%", PlaceholderUtils.formatNumber(amount))
                            .replace("%player%", targetName);
                    sender.sendMessage(prefix + ColorUtils.colorize(msg));
                } else {
                    String notFoundMsg = plugin.getConfig().getString("messages.player_not_found", "&cИгрок не найден!");
                    sender.sendMessage(prefix + ColorUtils.colorize(notFoundMsg));
                }
            });
        });
    }

    private void handleTake(CommandSender sender, String[] args, String prefix) {
        if (!sender.hasPermission("exchangecoins.take")) {
            String noPermMsg = plugin.getConfig().getString("messages.no_permission", "&cУ вас нет прав на это!");
            sender.sendMessage(prefix + ColorUtils.colorize(noPermMsg));
            return;
        }

        if (args.length < 3) {
            sender.sendMessage(prefix + ColorUtils.colorize("&cИспользование: &e/ecoins take <ник> <сумма>"));
            return;
        }

        String targetName = args[1];
        long amount;

        try {
            amount = Long.parseLong(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(prefix + ColorUtils.colorize("&cНекорректная сумма!"));
            return;
        }

        if (amount <= 0) {
            sender.sendMessage(prefix + ColorUtils.colorize("&cСумма должна быть больше 0!"));
            return;
        }

        plugin.getWidgetsManager().take(targetName, amount).thenAccept(success -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (success) {
                    String msg = plugin.getConfig().getString("messages.take_success", "&aВы сняли &e%amount% &aвиджетов у игрока &e%player%");
                    msg = msg.replace("%amount%", PlaceholderUtils.formatNumber(amount))
                            .replace("%player%", targetName);
                    sender.sendMessage(prefix + ColorUtils.colorize(msg));
                } else {
                    String notFoundMsg = plugin.getConfig().getString("messages.player_not_found", "&cИгрок не найден!");
                    sender.sendMessage(prefix + ColorUtils.colorize(notFoundMsg));
                }
            });
        });
    }


    private void handleSet(CommandSender sender, String[] args, String prefix) {
        if (!sender.hasPermission("exchangecoins.set")) {
            String noPermMsg = plugin.getConfig().getString("messages.no_permission", "&cУ вас нет прав на это!");
            sender.sendMessage(prefix + ColorUtils.colorize(noPermMsg));
            return;
        }

        if (args.length < 3) {
            sender.sendMessage(prefix + ColorUtils.colorize("&cИспользование: &e/ecoins set <ник> <сумма>"));
            return;
        }

        String targetName = args[1];
        long amount;

        try {
            amount = Long.parseLong(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(prefix + ColorUtils.colorize("&cНекорректная сумма!"));
            return;
        }

        if (amount < 0) {
            sender.sendMessage(prefix + ColorUtils.colorize("&cСумма не может быть отрицательной!"));
            return;
        }

        plugin.getWidgetsManager().set(targetName, amount).thenAccept(success -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (success) {
                    String msg = plugin.getConfig().getString("messages.set_success", "&aВы установили баланс &e%player% &aна &e%amount% виджетов");
                    msg = msg.replace("%amount%", PlaceholderUtils.formatNumber(amount))
                            .replace("%player%", targetName);
                    sender.sendMessage(prefix + ColorUtils.colorize(msg));
                } else {
                    String notFoundMsg = plugin.getConfig().getString("messages.player_not_found", "&cИгрок не найден!");
                    sender.sendMessage(prefix + ColorUtils.colorize(notFoundMsg));
                }
            });
        });
    }


    private void handleReload(CommandSender sender, String prefix) {
        if (!sender.hasPermission("exchangecoins.reload")) {
            String noPermMsg = plugin.getConfig().getString("messages.no_permission", "&cУ вас нет прав на это!");
            sender.sendMessage(prefix + ColorUtils.colorize(noPermMsg));
            return;
        }

        plugin.reloadConfig();


        if (plugin.getBoostManager() != null) {
            plugin.getBoostManager().reloadBoosts();
        }


        ConfirmationMenu.reloadConfirmationConfig(plugin);

        String msg = plugin.getConfig().getString("messages.reload_success", "&aКонфигурация перезаружена!");
        sender.sendMessage(prefix + ColorUtils.colorize(msg));
        sender.sendMessage(prefix + ColorUtils.colorize("&7Загружено бустов: &e" + plugin.getBoostManager().getAllBoosts().size()));
        sender.sendMessage(prefix + ColorUtils.colorize("&7Меню подтверждения: &econfirmation.yml"));
    }


    private void handleStats(CommandSender sender, String[] args, String prefix) {
        if (!sender.hasPermission("exchangecoins.balance")) {
            String noPermMsg = plugin.getConfig().getString("messages.no_permission", "&cУ вас нет прав на это!");
            sender.sendMessage(prefix + ColorUtils.colorize(noPermMsg));
            return;
        }

        String targetName;

        if (args.length > 1) {
            targetName = args[1];
        } else {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ColorUtils.colorize("&cИспользуйте: &e/ecoins stats <ник>"));
                return;
            }
            targetName = ((Player) sender).getName();
        }

        Player target = Bukkit.getPlayer(targetName);
        UUID targetUuid;

        if (target != null) {
            targetUuid = target.getUniqueId();
        } else {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(targetName);
            if (!offlinePlayer.hasPlayedBefore()) {
                String notFoundMsg = plugin.getConfig().getString("messages.player_not_found", "&cИгрок не найден!");
                sender.sendMessage(prefix + ColorUtils.colorize(notFoundMsg));
                return;
            }
            targetUuid = offlinePlayer.getUniqueId();
        }

        plugin.getDatabaseManager().getStats(targetUuid.toString()).thenAccept(stats -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                String titleMsg = plugin.getConfig().getString("stats.stats_title", "&6&l=== Статистика игрока ===");
                String lineMsg = plugin.getConfig().getString("stats.stats_line",
                        "&7Продано: &e%sold% &7| Куплено: &e%bought% &7| Заработано: &e%earned%");

                sender.sendMessage(prefix + ColorUtils.colorize(titleMsg));
                sender.sendMessage(ColorUtils.colorize(lineMsg
                        .replace("%sold%", PlaceholderUtils.formatNumber(stats.getTotalSold()))
                        .replace("%bought%", PlaceholderUtils.formatNumber(stats.getTotalBought()))
                        .replace("%earned%", PlaceholderUtils.formatNumber(stats.getTotalEarned()))
                ));
            });
        });
    }


    private void handleCancel(CommandSender sender, String[] args, String prefix) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ColorUtils.colorize("&cЭту команду может использовать только игрок!"));
            return;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("exchangecoins.order")) {
            String noPermMsg = plugin.getConfig().getString("messages.no_permission", "&cУ вас нет прав на это!");
            player.sendMessage(prefix + ColorUtils.colorize(noPermMsg));
            return;
        }

        if (args.length < 2) {
            player.sendMessage(prefix + ColorUtils.colorize("&cИспользование: &e/ecoins cancel <id>"));
            return;
        }

        int orderId;
        try {
            orderId = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage(prefix + ColorUtils.colorize("&cНеверный ID заказа!"));
            return;
        }

        plugin.getOrderManager().cancelOrder(player, orderId).thenAccept(success -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (success) {
                    player.sendMessage(prefix + ColorUtils.colorize("&aЗаказ #" + orderId + " успешно отменён!"));
                } else {
                    player.sendMessage(prefix + ColorUtils.colorize("&cЗаказ не найден или он не ваш!"));
                }
            });
        });
    }


    private void handleDelete(CommandSender sender, String[] args, String prefix) {
        if (!sender.hasPermission("exchangecoins.admin")) {
            String noPermMsg = plugin.getConfig().getString("messages.no_permission", "&cУ вас нет прав на это!");
            sender.sendMessage(prefix + ColorUtils.colorize(noPermMsg));
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(prefix + ColorUtils.colorize("&cИспользование: &e/ecoins delete <id>"));
            return;
        }

        int orderId;
        try {
            orderId = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage(prefix + ColorUtils.colorize("&cНеверный ID заказа!"));
            return;
        }

        plugin.getDatabaseManager().getOrder(orderId).thenAccept(order -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (order == null) {
                    sender.sendMessage(prefix + ColorUtils.colorize("&cЗаказ не найден!"));
                    return;
                }

                if (!"active".equals(order.getStatus())) {
                    sender.sendMessage(prefix + ColorUtils.colorize("&cЗаказ уже не активен!"));
                    return;
                }


                plugin.getPlayerPointsAPI().give(order.getSellerUuid(), order.getCoinsAmount());


                plugin.getDatabaseManager().deleteOrder(orderId).thenAccept(v -> {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        sender.sendMessage(prefix + ColorUtils.colorize("&aЗаказ #" + orderId + " успешно удалён!"));
                        sender.sendMessage(prefix + ColorUtils.colorize("&7Продавцу возвращено &e" + order.getCoinsAmount() + " &7коинов."));


                        if (plugin.getConfig().getBoolean("logging.log_orders", true)) {
                            plugin.getTransactionLogger().logOrderDeleted(
                                    order.getSellerName(),
                                    order.getCoinsAmount(),
                                    order.getPrice(),
                                    orderId,
                                    sender.getName()
                            );
                        }
                    });
                });
            });
        });
    }


    private void handleMyOrders(CommandSender sender, String[] args, String prefix) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ColorUtils.colorize("&cЭту команду может использовать только игрок!"));
            return;
        }

        Player player = (Player) sender;

        plugin.getDatabaseManager().getPlayerOrders(player.getUniqueId()).thenAccept(orders -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (orders == null || orders.isEmpty()) {
                    player.sendMessage(prefix + ColorUtils.colorize("&7У вас нет активных заказов!"));
                    return;
                }

                player.sendMessage(ColorUtils.colorize("&8&m------------------------------------"));
                player.sendMessage(prefix + ColorUtils.colorize("&6&lВаши заказы"));
                player.sendMessage(ColorUtils.colorize(""));

                for (com.exchangecoins.database.Order order : orders) {
                    String orderInfo = ColorUtils.colorize("&eЗаказ #" + order.getId() +
                            " &7- &f" + order.getCoinsAmount() + " &7коинов за &f" + order.getPrice() + " &7виджетов");
                    player.sendMessage(orderInfo);
                }

                player.sendMessage(ColorUtils.colorize("&8&m------------------------------------"));
            });
        });
    }


    private void sendOrderError(Player player, OrderManager.OrderStatus status, String prefix) {
        String message;

        switch (status) {
            case INVALID_AMOUNT:
                message = plugin.getConfig().getString("messages.invalid_amount", "&cНекорректное количество!");
                break;
            case INVALID_PRICE:
                message = plugin.getConfig().getString("messages.invalid_price", "&cНекорректная цена!");
                break;
            case MAX_ORDERS_REACHED:
                int maxOrders = plugin.getConfig().getInt("orders.max_orders_per_player", 5);
                message = plugin.getConfig().getString("messages.max_orders_reached", "&cВы достигли максимального количества заказов!");
                message = message.replace("%max%", String.valueOf(maxOrders));
                break;
            case INSUFFICIENT_COINS:
                message = plugin.getConfig().getString("messages.insufficient_coins", "&cНедостаточно коинов PlayerPoints!");
                break;
            case COOLDOWN_ACTIVE:
                long remainingSeconds = plugin.getOrderManager().getRemainingCooldown(player.getUniqueId());
                message = plugin.getConfig().getString("messages.cooldown_active", "&cПодождите &e%seconds% &cсекунд перед созданием нового заказа!");
                message = message.replace("%seconds%", String.valueOf(remainingSeconds));
                break;
            default:
                message = "&cПроизошла ошибка при создании заказа!";
        }

        player.sendMessage(prefix + ColorUtils.colorize(message));
    }


    private void sendHelp(CommandSender sender) {
        String prefix = ColorUtils.colorize(plugin.getConfig().getString("messages.prefix", "&8[&6ExchangeCoins&8] "));

        sender.sendMessage(ColorUtils.colorize("&8&m------------------------------------"));
        sender.sendMessage(prefix + ColorUtils.colorize("&6&lExchangeCoins &7- Помощь"));
        sender.sendMessage(ColorUtils.colorize(""));
        sender.sendMessage(prefix + ColorUtils.colorize("&e/burse &7- Открыть меню биржи"));
        sender.sendMessage(prefix + ColorUtils.colorize("&e/ecoins order <кол-во> <цена> &7- Создать заказ"));
        sender.sendMessage(prefix + ColorUtils.colorize("&e/ecoins cancel <id> &7- Отменить свой заказ"));
        sender.sendMessage(prefix + ColorUtils.colorize("&e/ecoins myorders &7- Список ваших заказов"));
        sender.sendMessage(prefix + ColorUtils.colorize("&e/ecoins balance [ник] &7- Проверить баланс"));
        sender.sendMessage(prefix + ColorUtils.colorize("&e/ecoins stats [ник] &7- Статистика игрока"));

        if (sender.hasPermission("exchangecoins.admin")) {
            sender.sendMessage(ColorUtils.colorize(""));
            sender.sendMessage(prefix + ColorUtils.colorize("&c&lАдминистративные команды:"));
            sender.sendMessage(prefix + ColorUtils.colorize("&e/ecoins delete <id> &7- Удалить заказ"));
            sender.sendMessage(prefix + ColorUtils.colorize("&e/ecoins give <ник> <сумма> &7- Выдать виджеты"));
            sender.sendMessage(prefix + ColorUtils.colorize("&e/ecoins take <ник> <сумма> &7- Снять виджеты"));
            sender.sendMessage(prefix + ColorUtils.colorize("&e/ecoins set <ник> <сумма> &7- Установить баланс"));
            sender.sendMessage(prefix + ColorUtils.colorize("&e/ecoins reload &7- Перезагрузить конфиг"));
        }

        sender.sendMessage(ColorUtils.colorize("&8&m------------------------------------"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            return subcommands.stream()
                    .filter(sub -> sub.startsWith(partial))
                    .filter(sub -> hasPermission(sender, sub))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            String subcommand = args[0].toLowerCase();
            if (subcommand.equals("order")) {
                return Collections.singletonList("<количество>");
            }
            if (Arrays.asList("give", "take", "set", "balance", "stats").contains(subcommand)) {
                if (sender.hasPermission("exchangecoins.admin") || subcommand.equals("balance") || subcommand.equals("stats")) {
                    return Bukkit.getOnlinePlayers().stream()
                            .map(Player::getName)
                            .collect(Collectors.toList());
                }
            }
        }

        if (args.length == 3) {
            String subcommand = args[0].toLowerCase();
            if (Arrays.asList("give", "take", "set").contains(subcommand)) {
                return Collections.singletonList("<сумма>");
            }
            if (subcommand.equals("order")) {
                return Collections.singletonList("<цена>");
            }
        }

        return Collections.emptyList();
    }


    private boolean hasPermission(CommandSender sender, String subcommand) {
        switch (subcommand) {
            case "order":
                return sender.hasPermission("exchangecoins.order");
            case "balance":
            case "stats":
                return sender.hasPermission("exchangecoins.balance");
            case "give":
            case "take":
            case "set":
                return sender.hasPermission("exchangecoins.admin");
            case "reload":
                return sender.hasPermission("exchangecoins.reload");
            default:
                return true;
        }
    }
}
