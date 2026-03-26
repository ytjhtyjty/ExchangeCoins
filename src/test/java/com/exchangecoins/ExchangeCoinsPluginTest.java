package com.exchangecoins;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExchangeCoinsPluginTest {

    @Test
    void testPluginInstance() {
        assertNotNull(ExchangeCoinsPlugin.class);
    }

    @Test
    void testGetInstance_nullInitially() {
        assertNull(ExchangeCoinsPlugin.getInstance());
    }
}
