package com.farm.Repository;

import com.farm.Entity.OrderItem;
import com.farm.Entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {


    @Query("SELECT oi FROM OrderItem oi WHERE oi.product.farmer.id = :farmerId")
    List<OrderItem> findByProductFarmerId(@Param("farmerId") Long farmerId);

    long countByProductFarmerIdAndOrderStatus(Long id, String pending);

    @Modifying
    @Transactional
    @Query("DELETE FROM OrderItem oi WHERE oi.product = :product")
    void deleteByProduct(Product product);
}
