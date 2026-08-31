package com.farm.Entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "products") // All categories live here, but are "labeled"
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Product name is required")
    @Size(min = 2, max = 50, message = "Name must be between 2 and 50 characters")
    @Column(nullable = false)
    private String productName;

    @NotBlank(message = "Description cannot be empty")
    @Size(min = 10, max = 500, message = "Description should be 10-500 characters")
    @Column(columnDefinition = "TEXT")
    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "1.0", message = "Price must be at least 1.0")
    private Double price;

    @NotNull(message = "Stock is required")
    @Min(value = 0, message = "Stock cannot be negative")
    private Integer stock;

    @NotBlank(message = "Please specify units (e.g., kg)")
    private String units;

    @Column(length = 1000)
    private String image;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    // Relationships (Farmer and Category)
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "farmer_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Farmer farmer;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Category category;

    // Add this to Product.java
    @OneToMany(mappedBy = "product", cascade = {CascadeType.REMOVE, CascadeType.MERGE}, orphanRemoval = true)
    private List<OrderItem> orderItems = new ArrayList<>();


    // Default Constructor
    public Product() {}

    // Full Constructor
    public Product(Long id, Farmer farmer, Category category, String productName, String description, Double price, Integer stock, String units, String image, boolean active) {
        this.id = id;
        this.farmer = farmer;
        this.category = category;
        this.productName = productName;
        this.description = description;
        this.price = price;
        this.stock = stock;
        this.units = units;
        this.image = image;
        this.active = active;
    }

    // Getters and Setters ...

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Farmer getFarmer() {
        return farmer;
    }

    public void setFarmer(Farmer farmer) {
        this.farmer = farmer;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public String getUnits() {
        return units;
    }

    public void setUnits(String units) {
        this.units = units;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public List<OrderItem> getOrderItems() {
        return orderItems;
    }

    public void setOrderItems(List<OrderItem> orderItems) {
        this.orderItems = orderItems;
    }
}