package com.exchangecoins.database;

import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты для класса Order
 */
class OrderTest {

    @Test
    void testOrderCreation() {
        UUID sellerUuid = UUID.randomUUID();
        String sellerName = "TestSeller";
        int coinsAmount = 1000;
        long price = 5000;
        ZonedDateTime expiresAt = ZonedDateTime.now().plusDays(7);

        Order order = new Order(sellerUuid, sellerName, coinsAmount, price, expiresAt);

        assertEquals(sellerUuid, order.getSellerUuid());
        assertEquals(sellerName, order.getSellerName());
        assertEquals(coinsAmount, order.getCoinsAmount());
        assertEquals(price, order.getPrice());
        assertEquals("active", order.getStatus());
    }

    @Test
    void testIsExpired_notExpired() {
        Order order = new Order();
        order.setExpiresAt(ZonedDateTime.now().plusDays(7));
        assertFalse(order.isExpired());
    }

    @Test
    void testIsExpired_expired() {
        Order order = new Order();
        order.setExpiresAt(ZonedDateTime.now().minusDays(1));
        assertTrue(order.isExpired());
    }

    @Test
    void testGetFormattedTimeLeft() {
        Order order = new Order();
        order.setExpiresAt(ZonedDateTime.now().plusDays(2).plusHours(3).plusMinutes(30));
        
        String result = order.getFormattedTimeLeft();
        assertTrue(result.contains("дн."));
        assertTrue(result.contains("ч."));
    }

    @Test
    void testGetFormattedTimeLeft_expired() {
        Order order = new Order();
        order.setExpiresAt(ZonedDateTime.now().minusHours(1));
        
        String result = order.getFormattedTimeLeft();
        assertEquals("&cИстекло", result);
    }

    @Test
    void testSettersAndGetters() {
        Order order = new Order();
        
        order.setId(123);
        assertEquals(123, order.getId());
        
        UUID uuid = UUID.randomUUID();
        order.setSellerUuid(uuid);
        assertEquals(uuid, order.getSellerUuid());
        
        order.setSellerName("NewName");
        assertEquals("NewName", order.getSellerName());
        
        order.setCoinsAmount(500);
        assertEquals(500, order.getCoinsAmount());
        
        order.setPrice(2500);
        assertEquals(2500, order.getPrice());
        
        order.setStatus("completed");
        assertEquals("completed", order.getStatus());
    }
}
