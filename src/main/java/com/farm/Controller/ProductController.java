package com.farm.Controller;

import com.farm.Entity.Category;
import com.farm.Entity.Farmer;
import com.farm.Entity.Product;
import com.farm.Repository.CategoryRepository;
import com.farm.Repository.FarmerRepository;
import com.farm.Repository.ProductRepository;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/farmer/products")
public class ProductController {

    @Autowired
    private ProductRepository productRepo;

    @Autowired
    private CategoryRepository categoryRepo;

    @Autowired
    private FarmerRepository farmerRepo;

    // 1. VIEW PRODUCTS BY CATEGORY
    @GetMapping("/category/{categoryId}")
    public String viewCategoryProducts(@PathVariable Long categoryId, Principal principal, Model model) {
        Farmer farmer = farmerRepo.findByEmail(principal.getName()).orElse(null);
        Category category = categoryRepo.findById(categoryId).orElse(null);

        if (category != null && farmer != null) {
            List<Product> products = productRepo.findByFarmerIdAndCategoryName(farmer.getId(), category.getName());

            model.addAttribute("products", products);
            model.addAttribute("selectedCategory", category);

            // --- THE FIX ---
            // Thymeleaf needs an empty product object for the th:object="${product}" in the modal
            model.addAttribute("product", new Product());
        }
        return "farmer/product-manage :: product-list";
    }

    // 2. SAVE OR UPDATE PRODUCT

    @PostMapping("/save")
    public String saveProduct(
            @RequestParam("productJson") String productJson, // The "Suitcase" containing all text data
            @RequestParam(value = "productImages", required = false) MultipartFile[] files,
            Principal principal,
            HttpServletResponse response,
            Model model) throws IOException {

        // 1. Manually convert JSON string to Product object
        ObjectMapper objectMapper = new ObjectMapper();
        Product product = objectMapper.readValue(productJson, Product.class);

        // 2. Extract categoryId manually from the JSON (sent from JS)
        // We use a Map to grab it easily since it's not a direct field in the Product entity
        var dataMap = objectMapper.readValue(productJson, java.util.Map.class);
        Long categoryId = Long.valueOf(dataMap.get("categoryId").toString());

        Farmer farmer = farmerRepo.findByEmail(principal.getName()).orElse(null);
        Category category = categoryRepo.findById(categoryId).orElse(null);

        // 3. MANUAL VALIDATION (Since @Valid doesn't work on raw Strings)
        // Inside your saveProduct method
        boolean hasErrors = false;

// Description validation (minimum 15 characters)
        if (product.getDescription() == null || product.getDescription().trim().length() < 15) {
            model.addAttribute("descError", "Description is too short (min 15 chars).");
            hasErrors = true;
        }

        if (hasErrors) {
            // Send a special header so JS knows this isn't a "Success" but a "Fix this" message
            response.setHeader("X-Status", "validation-error");
            model.addAttribute("selectedCategory", category);
            model.addAttribute("product", product); // Keeps your typed data in the modal

            return "farmer/product-manage :: product-list";
        }        // 4. LOGIC TO SAVE
        if (farmer != null && category != null) {
            // Multi-image saving logic
            if (files != null && files.length >= 3 && !files[0].isEmpty()) {
                StringBuilder imageNames = new StringBuilder();
                for (MultipartFile file : files) {
                    if (!file.isEmpty()) {
                        String fileName = saveImage(file);
                        if (imageNames.length() > 0) imageNames.append(",");
                        imageNames.append(fileName);
                    }
                }
                product.setImage(imageNames.toString());
            } else if (product.getId() != null) {
                // Preserve old images if editing and no new ones uploaded
                productRepo.findById(product.getId()).ifPresent(p -> product.setImage(p.getImage()));
            }

            product.setFarmer(farmer);
            product.setCategory(category);
            productRepo.save(product);

            response.setHeader("X-Status", "success");
            String msg = (product.getId() != null) ? "Product updated successfully!" : "Product added successfully!";
            model.addAttribute("success", msg);

            // Refresh UI
            model.addAttribute("products", productRepo.findByFarmerIdAndCategoryName(farmer.getId(), category.getName()));
            model.addAttribute("selectedCategory", category);
            model.addAttribute("product", new Product());
        } else {
            model.addAttribute("error", "Session error: Farmer or Category not found.");
        }

        return "farmer/product-manage :: product-list";
    }
    // 3. DELETE PRODUCT
    @DeleteMapping("/delete/{id}")
    public String deleteProduct(@PathVariable Long id, Principal principal, HttpServletResponse response, Model model) {
        Product product = productRepo.findById(id).orElse(null);

        if (product != null) {
            Category category = product.getCategory();
            productRepo.delete(product);
            response.setHeader("X-Status", "deleted");

            Farmer farmer = farmerRepo.findByEmail(principal.getName()).orElse(null);
            if (farmer != null) {
                List<Product> products = productRepo.findByFarmerIdAndCategoryName(farmer.getId(), category.getName());
                model.addAttribute("products", products);
                model.addAttribute("selectedCategory", category);
                // Also add an empty product here to prevent template errors during the refresh
                model.addAttribute("product", new Product());
            }
        }
        return "farmer/product-manage :: product-list";
    }

    private String saveImage(MultipartFile file) {
        try {
            String uploadDir = System.getProperty("user.dir") + File.separator + "uploads" + File.separator;
            File directory = new File(uploadDir);
            if (!directory.exists()) directory.mkdirs();

            String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
            Path path = Paths.get(uploadDir + fileName);
            Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
            return fileName;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @PatchMapping("/toggle-status/{id}")
    @ResponseBody
    public ResponseEntity<?> toggleProductStatus(@PathVariable Long id, Principal principal) {
        // 1. Fetch Product
        Product product = productRepo.findById(id).orElse(null);
        if (product == null) {
            return ResponseEntity.status(HttpServletResponse.SC_NOT_FOUND).body("Product not found");
        }

        // 2. Security Check: Only the owner can hide/show their product
        if (!product.getFarmer().getEmail().equals(principal.getName())) {
            return ResponseEntity.status(HttpServletResponse.SC_FORBIDDEN).body("Unauthorized");
        }

        // 3. Flip the boolean status
        product.setActive(!product.isActive());
        productRepo.save(product);

        // 4. Return the new status so the UI can update the badge/icon color
        return ResponseEntity.ok(java.util.Map.of(
                "active", product.isActive(),
                "message", product.isActive() ? "Product is now Visible" : "Product is now Hidden"
        ));
    }
}