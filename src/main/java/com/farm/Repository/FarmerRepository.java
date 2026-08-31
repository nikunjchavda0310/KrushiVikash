package com.farm.Repository;

import com.farm.Entity.Farmer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FarmerRepository extends JpaRepository<Farmer, Long> {

    Optional<Farmer> findByEmail(String email);

    boolean existsByEmail(String email);

    // In FarmerRepository.java
    List<Farmer> findByActiveTrue();

    long countByVerifiedFalse();
}
