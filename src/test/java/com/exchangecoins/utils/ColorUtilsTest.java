package com.exchangecoins.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ColorUtilsTest {

    @Test
    void testColorize_withAmpersandCodes() {
        String input = "&cКрасный &aЗелёный &eЖёлтый";
        String result = ColorUtils.colorize(input);
        
        assertNotNull(result);
        assertTrue(result.contains("Красный"));
        assertTrue(result.contains("Зелёный"));
        assertTrue(result.contains("Жёлтый"));
    }

    @Test
    void testColorize_withHexCode() {
        String input = "&#FF0000Красный HEX";
        String result = ColorUtils.colorize(input);
        
        assertNotNull(result);
        assertTrue(result.contains("Красный HEX"));
    }

    @Test
    void testColorize_nullInput() {
        String result = ColorUtils.colorize(null);
        assertEquals("", result);
    }

    @Test
    void testColorize_emptyInput() {
        String result = ColorUtils.colorize("");
        assertEquals("", result);
    }

    @Test
    void testStripColors() {
        String input = "&cКрасный текст";
        String result = ColorUtils.stripColors(input);
        
        assertNotNull(result);
        assertEquals("Красный текст", result);
    }

    @Test
    void testStripColors_nullInput() {
        String result = ColorUtils.stripColors(null);
        assertEquals("", result);
    }

    @Test
    void testIsValidHex_valid() {
        assertTrue(ColorUtils.isValidHex("#FF0000"));
        assertTrue(ColorUtils.isValidHex("FF0000"));
        assertTrue(ColorUtils.isValidHex("#ff0000"));
    }

    @Test
    void testIsValidHex_invalid() {
        assertFalse(ColorUtils.isValidHex("#FFF"));
        assertFalse(ColorUtils.isValidHex("GGG000"));
        assertFalse(ColorUtils.isValidHex(null));
        assertFalse(ColorUtils.isValidHex(""));
    }
}
