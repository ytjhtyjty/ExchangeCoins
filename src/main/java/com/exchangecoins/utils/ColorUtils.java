package com.exchangecoins.utils;

import org.bukkit.ChatColor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ColorUtils {

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final Pattern AMPERSAND_PATTERN = Pattern.compile("&([A-Fa-f0-9k-orK-OR])");

    public static String colorize(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        Matcher hexMatcher = HEX_PATTERN.matcher(text);
        StringBuffer buffer = new StringBuffer();

        while (hexMatcher.find()) {
            String hex = hexMatcher.group(1);
            hexMatcher.appendReplacement(buffer, net.md_5.bungee.api.ChatColor.of("#" + hex).toString());
        }
        hexMatcher.appendTail(buffer);

        String result = buffer.toString();
        result = ChatColor.translateAlternateColorCodes('&', result);

        return result;
    }

    public static String stripColors(String text) {
        if (text == null) {
            return "";
        }
        return ChatColor.stripColor(colorize(text));
    }

    public static boolean isValidHex(String hex) {
        if (hex == null || hex.isEmpty()) {
            return false;
        }
        return hex.matches("#?[A-Fa-f0-9]{6}");
    }
}
