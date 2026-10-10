package com.tharigamart.dto;

import java.math.BigDecimal;

/** Summary numbers for the seller dashboard. */
public class SellerStatsDTO {

    private long productCount;
    private long orderCount;
    private long itemCount;
    private BigDecimal revenue;

    public SellerStatsDTO() {
    }

    public SellerStatsDTO(long productCount, long orderCount, long itemCount, BigDecimal revenue) {
        this.productCount = productCount;
        this.orderCount = orderCount;
        this.itemCount = itemCount;
        this.revenue = revenue;
    }

    public long getProductCount() {
        return productCount;
    }

    public void setProductCount(long productCount) {
        this.productCount = productCount;
    }

    public long getOrderCount() {
        return orderCount;
    }

    public void setOrderCount(long orderCount) {
        this.orderCount = orderCount;
    }

    public long getItemCount() {
        return itemCount;
    }

    public void setItemCount(long itemCount) {
        this.itemCount = itemCount;
    }

    public BigDecimal getRevenue() {
        return revenue;
    }

    public void setRevenue(BigDecimal revenue) {
        this.revenue = revenue;
    }
}
