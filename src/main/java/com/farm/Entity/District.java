package com.farm.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.List;

@Entity
@Table(name = "district", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"name", "state_id"})
})
public class District {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "District name cannot be empty")
    @Pattern(regexp = "^[A-Za-z\\s]+$", message = "District name must contain only letters")
    @Column(nullable = false)
    private String name;

    @ManyToOne
    @JoinColumn(name = "state_id", nullable = false)
    private State state;

    @JsonIgnore
    @OneToMany(mappedBy = "district", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Taluka> talukas;

    public District() {
    }

    public District(Long id, String name, State state, List<Taluka> talukas) {
        this.id = id;
        this.name = name;
        this.state = state;
        this.talukas = talukas;
    }

    // Automatically cleans the name before saving
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

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public List<Taluka> getTalukas() {
        return talukas;
    }

    public void setTalukas(List<Taluka> talukas) {
        this.talukas = talukas;
    }
}