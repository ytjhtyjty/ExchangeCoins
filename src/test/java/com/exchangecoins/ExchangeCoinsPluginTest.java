package com.exchangecoins;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Базовые тесты для ExchangeCoinsPlugin
 * Примечание: Полноценное тестирование плагина требует MockBukkit
 */
class ExchangeCoinsPluginTest {

    @Test
    void testPluginInstance() {
        // Проверяем, что класс существует и может быть загружен
        assertNotNull(ExchangeCoinsPlugin.class);
    }

    @Test
    void testGetInstance_nullInitially() {
        // getInstance будет null до включения плагина
        assertNull(ExchangeCoinsPlugin.getInstance());
    }
}
