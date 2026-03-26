package com.exchangecoins.utils;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты для утилиты PlaceholderUtils
 */
class PlaceholderUtilsTest {

    @Test
    void testReplacePlaceholders() {
        String text = "Привет, %name%! Твой баланс: %balance%";
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("%name%", "Игрок");
        placeholders.put("%balance%", "1000");
        
        String result = PlaceholderUtils.replacePlaceholders(text, placeholders);
        
        assertEquals("Привет, Игрок! Твой баланс: 1000", result);
    }

    @Test
    void testReplacePlaceholders_nullText() {
        Map<String, String> placeholders = new HashMap<>();
        String result = PlaceholderUtils.replacePlaceholders(null, placeholders);
        assertNull(result);
    }

    @Test
    void testReplacePlaceholders_nullMap() {
        String text = "Текст без замен";
        String result = PlaceholderUtils.replacePlaceholders(text, null);
        assertEquals("Текст без замен", result);
    }

    @Test
    void testReplacePlaceholder() {
        String text = "Баланс: %balance%";
        String result = PlaceholderUtils.replacePlaceholder(text, "%balance%", "500");
        assertEquals("Баланс: 500", result);
    }

    @Test
    void testFormatNumber() {
        // DecimalFormat использует неразрывный пробел (\u00A0), поэтому заменяем на обычный
        String result1 = PlaceholderUtils.formatNumber(1000).replace('\u00A0', ' ');
        assertEquals("1 000", result1);
        
        String result2 = PlaceholderUtils.formatNumber(1000000).replace('\u00A0', ' ');
        assertEquals("1 000 000", result2);
        
        assertEquals("123", PlaceholderUtils.formatNumber(123));
    }

    @Test
    void testFormatTime_positive() {
        long twoDaysInMs = 2L * 24 * 60 * 60 * 1000;
        String result = PlaceholderUtils.formatTime(twoDaysInMs);
        assertTrue(result.contains("2 дн."));
    }

    @Test
    void testFormatTime_zero() {
        String result = PlaceholderUtils.formatTime(0);
        assertEquals("&cИстекло", result);
    }

    @Test
    void testFormatTime_negative() {
        String result = PlaceholderUtils.formatTime(-1000);
        assertEquals("&cИстекло", result);
    }

    @Test
    void testGetMaterial_valid() {
        Material result = PlaceholderUtils.getMaterial("GOLD_INGOT", Material.STONE);
        assertEquals(Material.GOLD_INGOT, result);
    }

    @Test
    void testGetMaterial_invalid() {
        Material result = PlaceholderUtils.getMaterial("INVALID_MATERIAL", Material.STONE);
        assertEquals(Material.STONE, result);
    }

    @Test
    void testGetMaterial_null() {
        Material result = PlaceholderUtils.getMaterial(null, Material.STONE);
        assertEquals(Material.STONE, result);
    }
}
