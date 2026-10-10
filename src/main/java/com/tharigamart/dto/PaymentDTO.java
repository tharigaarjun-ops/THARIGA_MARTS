package com.tharigamart.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Payment record for an order. */
public class PaymentDTO {

    private Long id;
    private String transactionId;
    private String paymentMethod;
    private BigDecimal amount;
    private String status;
    private String paymentGatewayRef;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;

    public PaymentDTO() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPaymentGatewayRef() {
        return paymentGatewayRef;
    }

    public void setPaymentGatewayRef(String paymentGatewayRef) {
        this.paymentGatewayRef = paymentGatewayRef;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
