package com.farm.Repository;

import com.farm.Entity.Taluka;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TalukaRepository extends JpaRepository<Taluka, Long> {

    // Existing method to fetch talukas for a specific district
    List<Taluka> findByDistrictId(Long districtId);

    /**
     * STRICT DUPLICATE CHECK:
     * Checks if a Taluka name already exists within a specific District.
     * Use this in the Controller to block duplicate entries.
     */
    boolean existsByNameIgnoreCaseAndDistrictId(String name, Long districtId);
}