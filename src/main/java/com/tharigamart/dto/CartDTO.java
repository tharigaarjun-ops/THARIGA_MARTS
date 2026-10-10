package com.tharigamart.dto;

import java.time.LocalDateTime;
import java.util.List;

/** Server-side shopping cart for the signed-in user. */
public class CartDTO {

    private Long id;
    private LocalDateTime updatedAt;
    private List<CartItemDTO> items;

    public CartDTO() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<CartItemDTO> getItems() {
        return items;
    }

    public void setItems(List<CartItemDTO> items) {
        this.items = items;
    }
}
