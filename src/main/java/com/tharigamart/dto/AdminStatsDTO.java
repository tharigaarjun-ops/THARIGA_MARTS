package com.tharigamart.dto;

import java.math.BigDecimal;

/** Summary numbers for the admin dashboard. */
public class AdminStatsDTO {

    private long totalUsers;
    private long totalProducts;
    private long totalOrders;
    private long totalSellers;
    private BigDecimal totalRevenue;
    private long pendingOrders;
    private long completedPayments;

    public AdminStatsDTO() {
    }

    public AdminStatsDTO(long totalUsers, long totalProducts, long totalOrders,
                             long totalSellers, BigDecimal totalRevenue,
                             long pendingOrders, long completedPayments) {
        this.totalUsers = totalUsers;
        this.totalProducts = totalProducts;
        this.totalOrders = totalOrders;
        this.totalSellers = totalSellers;
        this.totalRevenue = totalRevenue;
        this.pendingOrders = pendingOrders;
        this.completedPayments = completedPayments;
    }

    public long getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(long totalUsers) {
        this.totalUsers = totalUsers;
    }

    public long getTotalProducts() {
        return totalProducts;
    }

    public void setTotalProducts(long totalProducts) {
        this.totalProducts = totalProducts;
    }

    public long getTotalOrders() {
        return totalOrders;
    }

    public void setTotalOrders(long totalOrders) {
        this.totalOrders = totalOrders;
    }

    public long getTotalSellers() {
        return totalSellers;
    }

    public void setTotalSellers(long totalSellers) {
        this.totalSellers = totalSellers;
    }

    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(BigDecimal totalRevenue) {
        this.totalRevenue = totalRevenue;
    }

    public long getPendingOrders() {
        return pendingOrders;
    }

    public void setPendingOrders(long pendingOrders) {
        this.pendingOrders = pendingOrders;
    }

    public long getCompletedPayments() {
        return completedPayments;
    }

    public void setCompletedPayments(long completedPayments) {
        this.completedPayments = completedPayments;
    }
}
