package com.farm.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.List;

@Entity
@Table(name = "state", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"name"})
})
public class State {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "State name cannot be empty")
    @Pattern(regexp = "^[A-Za-z\\s]+$", message = "State name must contain only letters")
    @Column(nullable = false, unique = true)
    private String name;

    @JsonIgnore
    @OneToMany(mappedBy = "state", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<District> districts;

    public State() {
    }

    public State(Long id, String name, List<District> districts) {
        this.id = id;
        this.name = name;
        this.districts = districts;
    }

    // This logic ensures that any data saved is trimmed of extra spaces
    // and consistent before it hits the database.
    @PrePersist
    @PreUpdate
    protected void onCreateUpdate() {
        if (this.name != null) {
            this.name = this.name.trim();
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

    public void setName(String name) {
        this.name = name;
    }

    public List<District> getDistricts() {
        return districts;
    }

    public void setDistricts(List<District> districts) {
        this.districts = districts;
    }
}