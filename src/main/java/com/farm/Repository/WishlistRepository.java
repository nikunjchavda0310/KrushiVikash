package com.farm.Repository;

import com.farm.Entity.Client;
import com.farm.Entity.Product;
import com.farm.Entity.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, Long> {

    // 1. Used by the Controller to find the item for deletion (The Toggle-Off)
    Optional<Wishlist> findByProductIdAndClientEmail(Long productId, String email);

    // 2. Used to check if the heart should be Red or Gray in HTML
    boolean existsByProductIdAndClientEmail(Long productId, String email);

    // 3. To display the wishlist page for the logged-in user
    List<Wishlist> findByClientEmail(String email);

    // 4. Used by GlobalControllerAdvice for the navbar badge count
    long countByClientEmail(String email);

    // Kept for backward compatibility or other logic
    boolean existsByProductId(Long productId);

    @Transactional
    void deleteByProduct(Product product);


    void deleteByClient(Client client);
}