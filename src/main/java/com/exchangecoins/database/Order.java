package com.exchangecoins.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class Order {

    private int id;
    private UUID sellerUuid;
    private String sellerName;
    private int coinsAmount;
    private long price;
    private ZonedDateTime createdAt;
    private ZonedDateTime expiresAt;
    private String status;

    private static final ZoneId MSK_ZONE = ZoneId.of("Europe/Moscow");
    private static final DateTimeFormatter DB_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public Order() {
    }

    public Order(UUID sellerUuid, String sellerName, int coinsAmount, long price, ZonedDateTime expiresAt) {
        this.sellerUuid = sellerUuid;
        this.sellerName = sellerName;
        this.coinsAmount = coinsAmount;
        this.price = price;
        this.expiresAt = expiresAt;
        this.status = "active";
    }

    public static Order fromResultSet(ResultSet rs) throws SQLException {
        Order order = new Order();
        order.setId(rs.getInt("id"));
        order.setSellerUuid(UUID.fromString(rs.getString("seller_uuid")));
        order.setSellerName(rs.getString("seller_name"));
        order.setCoinsAmount(rs.getInt("coins_amount"));
        order.setPrice(rs.getLong("price"));

        String createdAtStr = rs.getString("created_at");
        String expiresAtStr = rs.getString("expires_at");

        if (createdAtStr != null) {
            LocalDateTime createdLocal = LocalDateTime.parse(createdAtStr, DB_FORMATTER);
            order.setCreatedAt(createdLocal.atZone(MSK_ZONE));
        }
        if (expiresAtStr != null) {
            LocalDateTime expiresLocal = LocalDateTime.parse(expiresAtStr, DB_FORMATTER);
            order.setExpiresAt(expiresLocal.atZone(MSK_ZONE));
        }

        order.setStatus(rs.getString("status"));

        return order;
    }

    public boolean isExpired() {
        return expiresAt != null && ZonedDateTime.now().isAfter(expiresAt);
    }

    public long getRemainingTimeMillis() {
        if (expiresAt == null) {
            return 0;
        }
        long millis = ZonedDateTime.now().until(expiresAt, java.time.temporal.ChronoUnit.MILLIS);
        return Math.max(0, millis);
    }

    public String getFormattedTimeLeft() {
        long millis = getRemainingTimeMillis();
        if (millis <= 0) {
            return "&cИстекло";
        }

        long days = millis / (1000 * 60 * 60 * 24);
        long hours = (millis % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60);
        long minutes = (millis % (1000 * 60 * 60)) / (1000 * 60);

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

        return result.length() > 0 ? "&a" + result : "&aСейчас";
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public UUID getSellerUuid() {
        return sellerUuid;
    }

    public void setSellerUuid(UUID sellerUuid) {
        this.sellerUuid = sellerUuid;
    }

    public String getSellerName() {
        return sellerName;
    }

    public void setSellerName(String sellerName) {
        this.sellerName = sellerName;
    }

    public int getCoinsAmount() {
        return coinsAmount;
    }

    public void setCoinsAmount(int coinsAmount) {
        this.coinsAmount = coinsAmount;
    }

    public long getPrice() {
        return price;
    }

    public void setPrice(long price) {
        this.price = price;
    }

    public ZonedDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(ZonedDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public ZonedDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(ZonedDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
