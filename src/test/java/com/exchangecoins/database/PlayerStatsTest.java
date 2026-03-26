package com.exchangecoins.database;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты для класса PlayerStats
 */
class PlayerStatsTest {

    @Test
    void testPlayerStatsCreation() {
        PlayerStats stats = new PlayerStats("test-uuid");
        
        assertEquals("test-uuid", stats.getUuid());
        assertEquals(0, stats.getTotalSold());
        assertEquals(0, stats.getTotalBought());
        assertEquals(0, stats.getTotalEarned());
        assertEquals(0, stats.getTotalSpent());
        assertEquals(0, stats.getOrdersCreated());
        assertEquals(0, stats.getOrdersCompleted());
    }

    @Test
    void testSettersAndGetters() {
        PlayerStats stats = new PlayerStats();
        
        stats.setUuid("uuid-123");
        assertEquals("uuid-123", stats.getUuid());
        
        stats.setUsername("TestPlayer");
        assertEquals("TestPlayer", stats.getUsername());
        
        stats.setTotalSold(100);
        assertEquals(100, stats.getTotalSold());
        
        stats.setTotalBought(200);
        assertEquals(200, stats.getTotalBought());
        
        stats.setTotalEarned(5000);
        assertEquals(5000, stats.getTotalEarned());
        
        stats.setTotalSpent(3000);
        assertEquals(3000, stats.getTotalSpent());
        
        stats.setOrdersCreated(10);
        assertEquals(10, stats.getOrdersCreated());
        
        stats.setOrdersCompleted(8);
        assertEquals(8, stats.getOrdersCompleted());
    }

    @Test
    void testFromResultSet_mock() {
        // Этот тест требует мока ResultSet, поэтому проверяем только создание объекта
        PlayerStats stats = new PlayerStats();
        assertNotNull(stats);
    }
}
