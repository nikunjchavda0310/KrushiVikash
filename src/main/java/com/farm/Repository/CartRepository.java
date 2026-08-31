package com.farm.Repository;

import com.farm.Entity.CartItem;
import com.farm.Entity.Client;
import com.farm.Entity.Product;
import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface CartRepository extends JpaRepository<CartItem, Long> {
    List<CartItem> findByClientEmail(String email);
    Optional<CartItem> findByClientAndProduct(Client client, Product product);
    List<CartItem> findByClient(Client client);
    @Transactional
    void deleteByClient(Client client);


}
