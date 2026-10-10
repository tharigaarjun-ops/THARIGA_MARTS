package com.tharigamart.entity;

/** Payment outcome of an order (orders.payment_status / payments.status). */
public enum PaymentStatus {
    COMPLETED,
    FAILED,
    PENDING,
    REFUNDED
}
