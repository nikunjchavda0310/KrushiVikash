package com.farm.Repository;

import com.farm.Entity.FarmerVerification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FarmerVerificationRepository extends JpaRepository<FarmerVerification, Long> {
    Optional<FarmerVerification> findByFarmerId(Long farmerId);

    boolean existsByRegNumber(String regNumber);

    Optional<FarmerVerification> findByRegNumber(String regNumber);
}
