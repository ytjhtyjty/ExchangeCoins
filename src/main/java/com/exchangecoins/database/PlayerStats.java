package com.exchangecoins.database;

import java.sql.ResultSet;
import java.sql.SQLException;

public class PlayerStats {

    private String uuid;
    private String username;
    private int totalSold;
    private int totalBought;
    private long totalEarned;
    private long totalSpent;
    private int ordersCreated;
    private int ordersCompleted;

    public PlayerStats() {
    }

    public PlayerStats(String uuid) {
        this.uuid = uuid;
    }


    public static PlayerStats fromResultSet(ResultSet rs) throws SQLException {
        PlayerStats stats = new PlayerStats();
        stats.setUuid(rs.getString("uuid"));
        stats.setUsername(rs.getString("username"));
        stats.setTotalSold(rs.getInt("total_sold"));
        stats.setTotalBought(rs.getInt("total_bought"));
        stats.setTotalEarned(rs.getLong("total_earned"));
        stats.setTotalSpent(rs.getLong("total_spent"));
        stats.setOrdersCreated(rs.getInt("orders_created"));
        stats.setOrdersCompleted(rs.getInt("orders_completed"));
        return stats;
    }



    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public int getTotalSold() {
        return totalSold;
    }

    public void setTotalSold(int totalSold) {
        this.totalSold = totalSold;
    }

    public int getTotalBought() {
        return totalBought;
    }

    public void setTotalBought(int totalBought) {
        this.totalBought = totalBought;
    }

    public long getTotalEarned() {
        return totalEarned;
    }

    public void setTotalEarned(long totalEarned) {
        this.totalEarned = totalEarned;
    }

    public long getTotalSpent() {
        return totalSpent;
    }

    public void setTotalSpent(long totalSpent) {
        this.totalSpent = totalSpent;
    }

    public int getOrdersCreated() {
        return ordersCreated;
    }

    public void setOrdersCreated(int ordersCreated) {
        this.ordersCreated = ordersCreated;
    }

    public int getOrdersCompleted() {
        return ordersCompleted;
    }

    public void setOrdersCompleted(int ordersCompleted) {
        this.ordersCompleted = ordersCompleted;
    }
}
