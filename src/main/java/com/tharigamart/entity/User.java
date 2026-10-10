package com.tharigamart.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Registered account. Roles: ROLE_BUYER, ROLE_SELLER, ROLE_ADMIN.
 * The legacy "full_name" column is kept in sync with "name" (the
 * display name used by the storefront).
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Display name (mapped to the real "name" column). */
    @Column(nullable = false, length = 255)
    private String name = "";

    /** Legacy NOT NULL column, kept in sync with "name". */
    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName = "";

    @Column(nullable = false, unique = true, length = 190)
    private String email;

    /** BCrypt encoded password. */
    @Column(nullable = false, length = 100)
    private String password;

    @Column(length = 20)
    private String phone;

    @Column(name = "avatar_url", length = 255)
    private String avatarUrl;

    @Column(nullable = false)
    private boolean enabled = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role = Role.ROLE_BUYER;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    public User() {
    }

    public User(String name, String email, String password, String phone, Role role) {
        this.name = name == null ? "" : name;
        this.fullName = this.name;
        this.email = email;
        this.password = password;
        this.phone = phone;
        this.role = role;
    }

    /** Display name: prefers "name", falls back to the legacy full name. */
    public String getDisplayName() {
        if (name != null && !name.isBlank()) {
            return name;
        }
        return fullName == null ? "" : fullName;
    }

    /** Keeps both name columns consistent. */
    public void setName(String name) {
        this.name = name == null ? "" : name;
        this.fullName = this.name.length() > 120 ? this.name.substring(0, 120) : this.name;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName == null ? "" : fullName;
        if (this.name == null || this.name.isBlank()) {
            this.name = this.fullName.length() > 255 ? this.fullName.substring(0, 255) : this.fullName;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
