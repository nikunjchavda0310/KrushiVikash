package com.farm.Entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "farmers")
public class Farmer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    @Column(unique = true, nullable = false)
    private String email;
    private String password;
    private String image;
    private String contact;
    private String gender;
    private boolean verified = false;
    private String otp;

    private boolean active = true;

    // Existing relationship for messages
    @OneToMany(mappedBy = "farmer", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<Message> messages = new ArrayList<>();

    // NEW: Relationship for Land Verification
    // mappedBy must match the variable name "farmer" in your FarmerVerification entity
    @OneToOne(mappedBy = "farmer", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private FarmerVerification verification;

    @OneToMany(mappedBy = "farmer", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<Product> products = new ArrayList<>();

    public Farmer() {
    }

    public Farmer(Long id, String name, String email, String password, String image, String contact, String gender, boolean verified, String otp, List<Message> messages, FarmerVerification verification, List<Product> products) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.password = password;
        this.image = image;
        this.contact = contact;
        this.gender = gender;
        this.verified = verified;
        this.otp = otp;
        this.messages = messages;
        this.verification = verification;
        this.products = products;
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

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getContact() {
        return contact;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public String getOtp() {
        return otp;
    }

    public void setOtp(String otp) {
        this.otp = otp;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }

    // ... Keep your existing Getters and Setters ...

    // Add Getter and Setter for Verification
    public FarmerVerification getVerification() {
        return verification;
    }

    public void setVerification(FarmerVerification verification) {
        this.verification = verification;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public List<Product> getProducts() {
        return products;
    }

    public void setProducts(List<Product> products) {
        this.products = products;
    }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}