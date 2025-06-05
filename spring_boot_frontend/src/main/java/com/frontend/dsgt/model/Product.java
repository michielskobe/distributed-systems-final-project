package com.frontend.dsgt.model;

public class Product {

    private String id;
    private String name;
    private String quantity;
    private String description;
    private String imageUrl;

    private String category;

    public Product() {
    }

    public Product(String id, String name, String quantity, String description, String imageUrl, String category) {
        this.id = id;
        this.name = name;
        this.quantity = quantity;
        this.description = description;
        this.imageUrl = imageUrl;
        this.category = category;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getQuantity() {
        return quantity;
    }

    public void setQuantity(String quantity) {
        this.quantity = quantity;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getCategory() {
        return category;
    }
    public void setCategory(String category) {
        this.category = category;
    }

}
