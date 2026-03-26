package com.exchangecoins.utils;

import org.bukkit.configuration.ConfigurationSection;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class PlaceholderUtils {

    private static final DecimalFormat NUMBER_FORMAT = new DecimalFormat("#,###");
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy HH:mm");


    public static String replacePlaceholders(String text, Map<String, String> placeholders) {
        if (text == null || placeholders == null) {
            return text;
        }

        String result = text;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue() != null ? entry.getValue() : "null");
        }

        return result;
    }


    public static String replacePlaceholder(String text, String placeholder, String value) {
        if (text == null) {
            return null;
        }
        return text.replace(placeholder, value != null ? value : "null");
    }


    public static String formatNumber(long number) {
        return NUMBER_FORMAT.format(number);
    }


    public static String formatTime(long milliseconds) {
        if (milliseconds <= 0) {
            return "&cИстекло";
        }

        long days = milliseconds / (1000 * 60 * 60 * 24);
        long hours = (milliseconds % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60);
        long minutes = (milliseconds % (1000 * 60 * 60)) / (1000 * 60);

        StringBuilder result = new StringBuilder();
        if (days > 0) {
            result.append(days).append(" дн. ");
        }
        if (hours > 0 || days > 0) {
            result.append(hours).append(" ч. ");
        }
        if (minutes > 0 || hours > 0 || days > 0) {
            result.append(minutes).append(" мин.");
        }

        return result.length() > 0 ? result.toString() : "&aСейчас";
    }


    public static List<String> getStringList(ConfigurationSection section, String path) {
        if (section == null || !section.contains(path)) {
            return Collections.emptyList();
        }

        List<?> list = section.getList(path);
        if (list == null) {
            return Collections.emptyList();
        }

        List<String> result = new ArrayList<>();
        for (Object obj : list) {
            if (obj instanceof String) {
                result.add((String) obj);
            }
        }

        return result;
    }


    public static String getString(ConfigurationSection section, String path, String def) {
        if (section == null) {
            return def;
        }
        return section.getString(path, def);
    }


    public static int getInt(ConfigurationSection section, String path, int def) {
        if (section == null) {
            return def;
        }
        return section.getInt(path, def);
    }


    public static long getLong(ConfigurationSection section, String path, long def) {
        if (section == null) {
            return def;
        }
        return section.getLong(path, def);
    }


    public static boolean getBoolean(ConfigurationSection section, String path, boolean def) {
        if (section == null) {
            return def;
        }
        return section.getBoolean(path, def);
    }


    public static org.bukkit.Material getMaterial(String materialName, org.bukkit.Material def) {
        if (materialName == null || materialName.isEmpty()) {
            return def;
        }

        try {
            return org.bukkit.Material.valueOf(materialName.toUpperCase());
        } catch (IllegalArgumentException e) {
            return def;
        }
    }
}
