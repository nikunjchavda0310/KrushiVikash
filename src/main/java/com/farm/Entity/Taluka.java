package com.farm.Entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Entity
// Prevents duplicate Taluka names within the same District
@Table(name = "taluka", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"name", "district_id"})
})
public class Taluka {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Taluka name cannot be empty")
    @Pattern(regexp = "^[A-Za-z\\s]+$", message = "Taluka name must contain only letters")
    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "district_id", nullable = false)
    private District district;

    public Taluka() {
    }

    public Taluka(Long id, String name, District district) {
        this.id = id;
        this.name = name;
        this.district = district;
    }

    // Ensures data consistency by trimming leading/trailing spaces
    @PrePersist
    @PreUpdate
    protected void onCreateUpdate() {
        if (this.name != null) {
            this.name = this.name.trim();
        }
    }

    // Getters and Setters
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

    public District getDistrict() {
        return district;
    }

    public void setDistrict(District district) {
        this.district = district;
    }
}