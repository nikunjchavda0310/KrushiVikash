package com.farm.Services;

import com.farm.Entity.Farmer;
import com.farm.Entity.Product;
import com.farm.Repository.FarmerRepository;
import com.farm.Repository.OrderItemRepository;
import com.farm.Repository.WishlistRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FarmerService {

    @Autowired
    private FarmerRepository farmerRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private WishlistRepository wishlistRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    public void saveFarmer(Farmer farmer) {
        farmer.setPassword(passwordEncoder.encode(farmer.getPassword()));
        farmerRepository.save(farmer);
    }

    public List<Farmer> getAllFarmers() {
        return farmerRepository.findByActiveTrue();
    }

    public void updateFarmer(Long id, Farmer updatedFarmer) {
        Farmer existingFarmer = farmerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Farmer not found with id: " + id));

        existingFarmer.setName(updatedFarmer.getName());
        existingFarmer.setEmail(updatedFarmer.getEmail());
        existingFarmer.setContact(updatedFarmer.getContact());

        if(updatedFarmer.getImage() != null) {
            existingFarmer.setImage(updatedFarmer.getImage());
        }

        farmerRepository.save(existingFarmer);
    }

    // In FarmerService.java
    // In FarmerService.java
    @Transactional
    public void deleteFarmer(Long id) {
        Farmer farmer = farmerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Farmer not found"));

        // Step 1: Clean up Product Dependencies (OrderItems AND Wishlist)
        for (Product product : farmer.getProducts()) {
            // Delete from OrderItems
            orderItemRepository.deleteByProduct(product);

            // DELETE FROM WISHLIST (This fixes your current error)
            wishlistRepository.deleteByProduct(product);

            // Clear internal lists to help Hibernate
            if (product.getOrderItems() != null) {
                product.getOrderItems().clear();
            }
        }

        // Step 2: Clear Farmer's product list (triggers orphanRemoval)
        farmer.getProducts().clear();

        // Step 3: Clear other dependencies
        if (farmer.getMessages() != null) {
            farmer.getMessages().clear();
        }
        farmer.setVerification(null);

        // Step 4: Flush and Delete
        farmerRepository.saveAndFlush(farmer);
        farmerRepository.delete(farmer);
    }
    public Farmer getFarmerById(Long id) {
        return farmerRepository.findById(id).orElseThrow();
    }
}
