package com.tharigamart.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Payload for admin user-role management. */
public class UpdateUserRoleRequest {

    @NotBlank(message = "Role is required")
    @Pattern(regexp = "^(ROLE_BUYER|ROLE_SELLER|ROLE_ADMIN|BUYER|SELLER|ADMIN)$",
            message = "Role must be ROLE_BUYER, ROLE_SELLER or ROLE_ADMIN")
    private String role;

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
