package com.exchangecoins.commands;

import com.exchangecoins.ExchangeCoinsPlugin;
import com.exchangecoins.menu.BurseMenu;
import com.exchangecoins.utils.ColorUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class BurseCommand implements CommandExecutor, TabCompleter {

    private final ExchangeCoinsPlugin plugin;

    public BurseCommand(ExchangeCoinsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ColorUtils.colorize("&cЭту команду может использовать только игрок!"));
            return true;
        }

        Player player = (Player) sender;


        if (!player.hasPermission("exchangecoins.burse")) {
            String prefix = ColorUtils.colorize(plugin.getConfig().getString("messages.prefix", "&8[&6ExchangeCoins&8] "));
            String noPermMsg = plugin.getConfig().getString("messages.no_permission", "&cУ вас нет прав на это!");
            player.sendMessage(prefix + ColorUtils.colorize(noPermMsg));
            return true;
        }


        BurseMenu menu = new BurseMenu(plugin, player);
        menu.open();

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}
