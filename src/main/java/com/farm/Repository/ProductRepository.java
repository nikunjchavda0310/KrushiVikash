package com.farm.Repository;

import com.farm.Entity.Category;
import com.farm.Entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // --- CLIENT SIDE METHODS (Only shows active products) ---

    // Get all products for the main shop (ONLY ACTIVE)
    List<Product> findByActiveTrue();

    // Get products by category for customers (ONLY ACTIVE)
    List<Product> findByCategoryNameAndActiveTrue(String name);

    // Search specifically for active products only
    @Query("SELECT p FROM Product p WHERE " +
            "LOWER(p.productName) LIKE LOWER(CONCAT('%', :keywords, '%')) OR " +
            "LOWER(p.description) LIKE LOWER(CONCAT('%', :keywords, '%')) OR " +
            "LOWER(p.category.name) LIKE LOWER(CONCAT('%', :keywords, '%')) OR " +
            "CAST(p.price AS string) LIKE CONCAT('%', :keywords, '%') OR " +
            "LOWER(p.units) LIKE LOWER(CONCAT('%', :keywords, '%'))")
    List<Product> searchGlobalActive(@Param("keywords") String keywords);


    // --- FARMER SIDE METHODS (Shows everything, including hidden) ---

    // Get only Fruits for a specific farmer
    List<Product> findByFarmerIdAndCategoryName(Long farmerId, String categoryName);

    // Get everything for a farmer (for their main dashboard/management)
    List<Product> findByFarmerId(Long farmerId);

    // Check stock for a specific category
    List<Product> findByFarmerIdAndCategoryNameAndStockLessThan(Long farmerId, String categoryName, Integer stockLimit);


    // --- GENERAL METHODS ---

    List<Product> findByCategoryName(String name);

    List<Product> findByCategory(Category category);

    // Finds products where the name contains the search text (case-insensitive)
    List<Product> findByProductNameContainingIgnoreCase(String name);

    @Query("SELECT p FROM Product p WHERE " +
            "LOWER(p.productName) LIKE LOWER(CONCAT('%', :keywords, '%')) OR " +
            "LOWER(p.description) LIKE LOWER(CONCAT('%', :keywords, '%')) OR " +
            "LOWER(p.category.name) LIKE LOWER(CONCAT('%', :keywords, '%')) OR " +
            "CAST(p.price AS string) LIKE CONCAT('%', :keywords, '%') OR " +
            "LOWER(p.units) LIKE LOWER(CONCAT('%', :keywords, '%'))")
    List<Product> searchGlobal(@Param("keywords") String keywords);
}