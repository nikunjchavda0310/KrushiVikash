package com.farm.Repository;

import com.farm.Entity.Client;
import com.farm.Entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByClientOrderByOrderDateDesc(Client client);

    @Query("SELECT DISTINCT o FROM Order o JOIN o.orderItems oi WHERE oi.product.farmer.id = :farmerId ORDER BY o.orderDate DESC")
    List<Order> findByFarmerId(@Param("farmerId") Long farmerId);

    @Query("SELECT DISTINCT o FROM Order o " +
            "JOIN o.orderItems oi " +
            "WHERE oi.product.farmer.id = :farmerId " +
            "ORDER BY o.orderDate DESC")
    List<Order> getOrdersByFarmerId(@Param("farmerId") Long farmerId);
}
