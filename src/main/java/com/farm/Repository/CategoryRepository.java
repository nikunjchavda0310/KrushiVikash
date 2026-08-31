package com.farm.Repository;

import com.farm.Entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {







    boolean existsByNameIgnoreCase(String name);
}