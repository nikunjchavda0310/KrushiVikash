package com.farm.Controller;

import com.farm.Entity.Farmer;
import com.farm.Entity.Order;
import com.farm.Entity.OrderItem;
import com.farm.Repository.FarmerRepository;
import com.farm.Repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/farmer") // Best practice to group farmer routes
public class ViewOrdersController {

    @Autowired
    private FarmerRepository farmerRepo;

    @Autowired
    private OrderRepository orderRepo;

    // Matches your JS: loadContent('/farmer/view-orders-fragment', this)
    @GetMapping("/view-orders-fragment")
    public String viewFarmerOrders(Model model, Principal principal) {
        if (principal == null) return "redirect:/login";

        String email = principal.getName();
        Farmer farmer = farmerRepo.findByEmail(email).orElse(null);

        if (farmer != null) {
            // 1. Fetch orders from repository
            List<Order> farmerOrders = orderRepo.findByFarmerId(farmer.getId());

            // 2. SORTING LOGIC: Latest Date First (Descending)
            // This ensures the 22-Mar-2026 order appears above the 10-Mar-2026 order
            if (farmerOrders != null && !farmerOrders.isEmpty()) {
                farmerOrders.sort((o1, o2) -> o2.getOrderDate().compareTo(o1.getOrderDate()));
            }

            model.addAttribute("orders", farmerOrders);

            // 3. Pass the ID for HTML filtering (The "Your Earning" calculation)
            model.addAttribute("loggedInFarmerId", farmer.getId());
        }

        return "farmer/view-orders-fragment";
    }

    // Handles the AJAX status update
    @PostMapping("/update-order-status/{orderId}")
    @ResponseBody
    public String updateStatus(@PathVariable Long orderId, Principal principal) {
        String email = principal.getName();
        Farmer farmer = farmerRepo.findByEmail(email).orElseThrow();
        Order order = orderRepo.findById(orderId).orElse(null);

        if (order != null) {
            // 1. Mark ONLY this farmer's items as PAID
            for (OrderItem item : order.getOrderItems()) {
                if (item.getProduct().getFarmer().getId().equals(farmer.getId())) {
                    item.setStatus("VERIFIED_PAID");
                }
            }

            // 2. Check if the entire order is now finished (all farmers verified)
            boolean allItemsFinished = order.getOrderItems().stream()
                    .allMatch(item -> "VERIFIED_PAID".equals(item.getStatus()));

            if (allItemsFinished) {
                order.setStatus("VERIFIED_PAID");
            } else {
                order.setStatus("PARTIALLY_VERIFIED");
            }

            orderRepo.save(order); // This saves the order and all updated items
            return "success";
        }
        return "error";
    }

    @GetMapping("/order-details/{id}")
    public String getOrderDetails(@PathVariable Long id, Model model, Principal principal) {
        Order order = orderRepo.findById(id).orElse(null);

        // We must get the farmer again here so the modal knows who is looking!
        String email = principal.getName();
        Farmer farmer = farmerRepo.findByEmail(email).orElse(null);

        model.addAttribute("order", order);
        model.addAttribute("loggedInFarmerId", farmer.getId()); // This fixes the 'null'

        return "farmer/order-details-modal :: details";
    }

    @GetMapping("/print-invoice/{id}")
    public String printFarmerInvoice(@PathVariable Long id, Model model, Principal principal) {
        Order order = orderRepo.findById(id).orElse(null);
        String email = principal.getName();
        Farmer farmer = farmerRepo.findByEmail(email).orElse(null);

        if (order != null && farmer != null) {
            model.addAttribute("order", order);
            model.addAttribute("farmer", farmer);
            model.addAttribute("loggedInFarmerId", farmer.getId());
            return "farmer/invoice-print"; // We will create this HTML file next
        }
        return "farmer/view-orders-fragment";
    }
}