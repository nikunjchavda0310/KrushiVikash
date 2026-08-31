package com.farm.Controller;

import com.farm.Entity.Category;
import com.farm.Repository.CategoryRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/admin/categories")
public class CategoryController {

    @Autowired
    private CategoryRepository categoryRepo;

    @GetMapping("/manage-fragment")
    public String manageCategories(Model model) {
        model.addAttribute("categories", categoryRepo.findAll());
        return "admin/fragments/category-manage :: category-manage";
    }

    @PostMapping("/save")
    public String saveCategory(@RequestParam("name") String name,
                               Model model,
                               HttpServletResponse response) {
        String cleanName = name.trim();

        // Check if category name already exists (Simple check)
        boolean exists = categoryRepo.existsByNameIgnoreCase(cleanName);

        if (exists) {
            model.addAttribute("error", "The category '" + cleanName + "' already exists!");
            response.setHeader("X-Status", "error");
        } else {
            Category category = new Category();
            category.setName(cleanName);
            categoryRepo.save(category);
            model.addAttribute("success", "Category saved successfully!");
            response.setHeader("X-Status", "success");
        }

        model.addAttribute("categories", categoryRepo.findAll());
        return "admin/fragments/category-manage :: category-manage";
    }

    @GetMapping("/delete/{id}")
    @Transactional // Ensures the entire operation is atomic
    public String deleteCategory(@PathVariable("id") Long id, Model model) {
        try {
            // 1. Check if products exist in this category before deleting
            // This is much faster and cleaner than waiting for a database error
            Category category = categoryRepo.findById(id)
                    .orElseThrow(() -> new RuntimeException("Category not found"));

            // Assuming you added the List<Product> products to your Category entity:
            if (category.getProducts() != null && !category.getProducts().isEmpty()) {
                model.addAttribute("error", "Cannot delete: This category contains " +
                        category.getProducts().size() + " products.");
            } else {
                categoryRepo.delete(category);
                model.addAttribute("success", "Category deleted successfully!");
            }
        } catch (Exception e) {
            // Log the actual error for debugging
            System.out.println("Delete Error: " + e.getMessage());
            model.addAttribute("error", "An unexpected error occurred while deleting.");
        }

        // Refresh the list for the fragment
        model.addAttribute("categories", categoryRepo.findAll());
        return "admin/fragments/category-manage :: category-manage";
    }
}