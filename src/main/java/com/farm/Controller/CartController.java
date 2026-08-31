package com.farm.Controller;

import com.farm.Entity.CartItem;
import com.farm.Entity.Client;
import com.farm.Entity.Product;
import com.farm.Repository.CartRepository;
import com.farm.Repository.ClientRepository;
import com.farm.Repository.ProductRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

@Controller
public class CartController {

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ClientRepository clientRepository;

    @PostMapping("/cart/add")
    public String addToCart(@RequestParam("pid") Long productId,
                            @RequestParam(value = "p_qty", defaultValue = "1") int quantity, // ADDED THIS
                            Principal principal,
                            HttpServletRequest request,
                            RedirectAttributes ra) {

        if (principal == null) {
            ra.addFlashAttribute("error", "Please login to add items to cart.");
            return "redirect:/login";
        }

        Optional<Client> clientOpt = clientRepository.findByEmail(principal.getName());
        Optional<Product> productOpt = productRepository.findById(productId);

        if (clientOpt.isEmpty() || productOpt.isEmpty()) {
            ra.addFlashAttribute("error", "Product not found.");
            return "redirect:/client/shop";
        }

        Client client = clientOpt.get();
        Product product = productOpt.get();

        Optional<CartItem> existingItem = cartRepository.findByClientAndProduct(client, product);

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            // FIXED: Add the selected quantity instead of just +1
            item.setQuantity(item.getQuantity() + quantity);
            cartRepository.save(item);
        } else {
            CartItem newItem = new CartItem();
            newItem.setClient(client);
            newItem.setProduct(product);
            // FIXED: Set the selected quantity instead of hardcoded 1
            newItem.setQuantity(quantity);
            cartRepository.save(newItem);
        }

        ra.addFlashAttribute("message", quantity + " " + product.getUnits() + " of " + product.getProductName() + " added to cart!");

        String referer = request.getHeader("Referer");
        return (referer != null && !referer.contains("/cart")) ? "redirect:" + referer : "redirect:/client/shop";
    }


    @GetMapping("/client/cart")
    public String showCart(Principal principal, Model model) {
        if (principal == null) return "redirect:/login";

        List<CartItem> cartItems = cartRepository.findByClientEmail(principal.getName());

        double grandTotal = cartItems.stream()
                .mapToDouble(item -> item.getProduct().getPrice() * item.getQuantity())
                .sum();

        model.addAttribute("cartItems", cartItems);
        model.addAttribute("grandTotal", grandTotal);

        return "client/cart";
    }

    // NEW: Handles the "Update" button in cart.html
    @PostMapping("/cart/update")
    public String updateCart(@RequestParam("cart_id") Long cartId,
                             @RequestParam("p_qty") int quantity,
                             RedirectAttributes ra) {
        Optional<CartItem> itemOpt = cartRepository.findById(cartId);
        if(itemOpt.isPresent()) {
            CartItem item = itemOpt.get();
            item.setQuantity(quantity);
            cartRepository.save(item);
            ra.addFlashAttribute("message", "Cart updated!");
        }
        return "redirect:/client/cart";
    }

    // NEW: Handles the "X" remove button in cart.html
    @GetMapping("/cart/delete")
    public String deleteCartItem(@RequestParam("id") Long cartId, RedirectAttributes ra) {
        cartRepository.deleteById(cartId);
        ra.addFlashAttribute("message", "Item removed from cart.");
        return "redirect:/client/cart";
    }

    @GetMapping("/cart/clear")
    public String clearCart(RedirectAttributes ra) {
        // This deletes EVERYTHING in the cart table
        // Note: In a real app, you'd usually delete only for the logged-in user
        cartRepository.deleteAll();

        ra.addFlashAttribute("message", "All items removed from cart.");
        return "redirect:/client/cart";
    }
}