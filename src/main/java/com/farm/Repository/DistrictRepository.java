package com.farm.Repository;

import com.farm.Entity.District;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DistrictRepository extends JpaRepository<District, Long> {

    // Existing method to fetch districts for the dropdowns
    List<District> findByStateId(Long stateId);

    /**
     * STRICT DUPLICATE CHECK:
     * Checks if a district name already exists within a specific State.
     * Use this in the Controller before saving.
     */
    boolean existsByNameIgnoreCaseAndStateId(String name, Long stateId);
}