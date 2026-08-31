package com.farm.Controller;

import com.farm.Entity.Product;
import com.farm.Entity.Wishlist;
import com.farm.Repository.ClientRepository;
import com.farm.Repository.ProductRepository;
import com.farm.Repository.WishlistRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/client/wishlist")
public class WishlistController {

    @Autowired
    private WishlistRepository wishlistRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ClientRepository clientRepo;

    @GetMapping
    public String showWishlist(Model model, Principal principal) {
        if (principal == null) return "redirect:/login";

        List<Wishlist> items = wishlistRepository.findByClientEmail(principal.getName());

        double totalValue = items.stream()
                .mapToDouble(item -> item.getProduct().getPrice())
                .sum();

        model.addAttribute("wishlistItems", items);
        model.addAttribute("totalValue", totalValue);

        // Required for the heart icons on the wishlist page itself
        List<Long> wishlistedIds = items.stream().map(i -> i.getProduct().getId()).toList();
        model.addAttribute("wishlistedIds", wishlistedIds);

        return "client/wishlist";
    }

    @PostMapping("/add")
    public String toggleWishlist(@RequestParam("pid") Long productId,
                                 Principal principal,
                                 HttpServletRequest request,
                                 RedirectAttributes ra) {

        if (principal == null) {
            ra.addFlashAttribute("error", "Please login to manage your wishlist.");
            return "redirect:/login";
        }

        String email = principal.getName();

        // TOGGLE LOGIC: Check if it already exists
        Optional<Wishlist> existingItem = wishlistRepository.findByProductIdAndClientEmail(productId, email);

        if (existingItem.isPresent()) {
            // SECOND CLICK: Remove from wishlist
            wishlistRepository.delete(existingItem.get());
            ra.addFlashAttribute("message", "Removed from wishlist.");
        } else {
            // FIRST CLICK: Add to wishlist
            Product product = productRepository.findById(productId).orElse(null);
            if (product == null) {
                ra.addFlashAttribute("error", "Product not found.");
            } else {
                Wishlist item = new Wishlist();
                item.setProduct(product);
                item.setClient(clientRepo.findByEmail(email).orElse(null));
                wishlistRepository.save(item);
                ra.addFlashAttribute("message", "Added to wishlist!");
            }
        }

        String referer = request.getHeader("Referer");
        return (referer != null) ? "redirect:" + referer : "redirect:/client/shop";
    }

    @GetMapping("/delete_all")
    @Transactional
    public String deleteAllItems(Principal principal, RedirectAttributes ra) {
        if (principal == null) return "redirect:/login";

        // FIX: Only delete items for the logged-in user
        List<Wishlist> userItems = wishlistRepository.findByClientEmail(principal.getName());
        wishlistRepository.deleteAll(userItems);

        ra.addFlashAttribute("message", "Your wishlist has been cleared.");
        return "redirect:/client/wishlist";
    }

    @GetMapping("/delete")
    @Transactional
    public String deleteItem(@RequestParam("id") Long wishlistId,
                             Principal principal,
                             RedirectAttributes ra) {
        if (principal == null) return "redirect:/login";

        // Security check: Ensure the item belongs to the logged-in user
        Optional<Wishlist> item = wishlistRepository.findById(wishlistId);

        if (item.isPresent() && item.get().getClient().getEmail().equals(principal.getName())) {
            wishlistRepository.deleteById(wishlistId);
            ra.addFlashAttribute("message", "Item removed from wishlist.");
        } else {
            ra.addFlashAttribute("error", "Could not remove item.");
        }

        return "redirect:/client/wishlist";
    }
}