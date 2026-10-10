package com.tharigamart.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Payload for seller profile create / update. */
public class SellerProfileRequest {

    @NotBlank(message = "Store name is required")
    @Size(max = 255, message = "Store name is too long")
    private String storeName;

    @Size(max = 1000, message = "Store description is too long")
    private String storeDescription;

    @Size(max = 500, message = "Business address is too long")
    private String businessAddress;

    @Size(max = 255, message = "GSTIN is too long")
    private String gstin;

    public String getStoreName() {
        return storeName;
    }

    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }

    public String getStoreDescription() {
        return storeDescription;
    }

    public void setStoreDescription(String storeDescription) {
        this.storeDescription = storeDescription;
    }

    public String getBusinessAddress() {
        return businessAddress;
    }

    public void setBusinessAddress(String businessAddress) {
        this.businessAddress = businessAddress;
    }

    public String getGstin() {
        return gstin;
    }

    public void setGstin(String gstin) {
        this.gstin = gstin;
    }
}
