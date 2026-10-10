package com.tharigamart.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Stationery category, e.g. Pencils, Notebooks, Craft Tools ... */
@Entity
@Table(name = "categories")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String name;

    @Column(nullable = false, unique = true, length = 90)
    private String slug;

    @Column(length = 255)
    private String description;

    /** Emoji shown on category tiles, e.g. 📚 (categories.icon_emoji). */
    @Column(name = "icon_emoji", length = 255)
    private String iconEmoji;

    /** Category banner image (categories.image_url). */
    @Column(name = "image_url", length = 255)
    private String imageUrl;

    /** Legacy Bootstrap Icons class (appended column, optional). */
    @Column(length = 60)
    private String icon;

    @Column(name = "display_order")
    private Integer displayOrder = 0;

    public Category() {
    }

    public Category(String name, String slug, String description, String iconEmoji, String imageUrl, int displayOrder) {
        this.name = name;
        this.slug = slug;
        this.description = description;
        this.iconEmoji = iconEmoji;
        this.imageUrl = imageUrl;
        this.displayOrder = displayOrder;
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

    public void setName(String name) {
        this.name = name;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIconEmoji() {
        return iconEmoji;
    }

    public void setIconEmoji(String iconEmoji) {
        this.iconEmoji = iconEmoji;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }
}
