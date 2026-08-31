package com.farm.Repository;

import com.farm.Entity.State;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface StateRepository extends JpaRepository<State, Long> {

    /**
     * Used for Duplicate Check:
     * Returns true if a state with the same name exists, ignoring case sensitivity.
     */
    boolean existsByNameIgnoreCase(String name);

    /**
     * Optional: Useful if you need to find the specific State object by name.
     */
    Optional<State> findByNameIgnoreCase(String name);
}