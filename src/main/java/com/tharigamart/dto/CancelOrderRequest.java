package com.tharigamart.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Payload for cancelling an order (buyer). */
public class CancelOrderRequest {

    @Size(max = 255, message = "Reason is too long")
    private String reason;

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
