package com.farm.Controller;

import com.farm.Entity.*;
import com.farm.Repository.*;
import com.farm.Services.ClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/client") // This prefixes all mappings in this file with /client
public class ClientController {

    @Autowired
    private ClientRepository clientRepo;

    @Autowired
    private ClientService clientService;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private WishlistRepository wishlistRepo;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CartRepository cartRepository;

    private void addWishlistStatus(Model model, Principal principal) {
        if (principal != null) {
            List<Wishlist> wishlist = wishlistRepo.findByClientEmail(principal.getName());
            List<Long> wishlistedIds = wishlist.stream()
                    .map(item -> item.getProduct().getId())
                    .toList();
            model.addAttribute("wishlistedIds", wishlistedIds);
        }
    }

    @GetMapping("/index")
    public String clientHome(Model model, Principal principal) {
        List<Category> categoryList = categoryRepository.findAll();
        model.addAttribute("categoryList", categoryList);
        model.addAttribute("productList", productRepository.findByActiveTrue());

        addWishlistStatus(model, principal); // ADDED
        return "client/index";
    }

    // Inside ClientController.java

    @GetMapping("/shop")
    public String showAllProducts(Model model, Principal principal) {
        List<Product> activeProducts = productRepository.findByActiveTrue();
        List<Category> allCategories = categoryRepository.findAll();

        model.addAttribute("productList", activeProducts);
        model.addAttribute("categoryList", allCategories);
        model.addAttribute("currentCategory", null);

        addWishlistStatus(model, principal); // ADDED
        return "client/shop";
    }

    @GetMapping("/orders") // Keep your original mapping
    public String clientOrders(Principal principal, Model model) {
        // 1. Get the logged-in Client securely
        String email = principal.getName();
        Client client = clientRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 2. Fetch the orders (Sorted so new ones are at the top)
        List<Order> orders = orderRepository.findByClientOrderByOrderDateDesc(client);

        // 3. Add to the model
        model.addAttribute("orders", orders);

        // 4. Return YOUR existing file name
        return "client/orders";
    }

    @GetMapping("/about")
    public String clientAbout() {
        return "client/about";
    }





    @GetMapping("/category")
    public String showCategory(@RequestParam("type") String type, Model model, Principal principal) {
        List<Product> filteredProducts = productRepository.findByCategoryNameAndActiveTrue(type);
        List<Category> allCategories = categoryRepository.findAll();

        model.addAttribute("productList", filteredProducts);
        model.addAttribute("categoryList", allCategories);
        model.addAttribute("currentCategory", type);

        addWishlistStatus(model, principal); // ADDED
        return "client/shop";
    }


    @GetMapping("/checkout")
    public String showCheckout(Model model, Principal principal) {
        // 1. Get the logged-in client
        String email = principal.getName();
        Client client = clientRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Client not found"));

        // 2. Fetch the Cart Items using the standard method name
        // Replace 'getCartItems' with 'findByClient'
        List<CartItem> cartItems = cartRepository.findByClient(client);

        // 3. Calculate the Grand Total manually in Java
        double grandTotal = cartItems.stream()
                .mapToDouble(item -> item.getProduct().getPrice() * item.getQuantity())
                .sum();

        // 4. Add data to the model for the HTML page
        model.addAttribute("client", client);
        model.addAttribute("cartItems", cartItems);
        model.addAttribute("grandTotal", grandTotal);

        return "client/checkout";
    }

    @GetMapping("/order-details/{id}")
    @ResponseBody
    public List<java.util.Map<String, Object>> getOrderDetails(@PathVariable Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        List<OrderItem> items = order.getOrderItems();
        List<java.util.Map<String, Object>> response = new java.util.ArrayList<>();

        for (OrderItem item : items) {
            java.util.Map<String, Object> map = new java.util.HashMap<>();

            // Use a fallback name if the product name is null
            String name = (item.getProduct() != null) ? item.getProduct().getProductName() : "Unknown Product";

            // Use the product's live price if the order item price is null
            Double price = (item.getPriceAtPurchase() != null && item.getPriceAtPurchase() > 0)
                    ? item.getPriceAtPurchase()
                    : (item.getProduct() != null ? item.getProduct().getPrice() : 0.0);

            Integer qty = (item.getQuantity() != null && item.getQuantity() > 0) ? item.getQuantity() : 1;

            map.put("productName", name);
            map.put("price", price);
            map.put("quantity", qty);

            response.add(map);
        }
        return response;
    }

    @GetMapping("/search")
    public String searchProducts(@RequestParam(value = "keywords", required = false) String keywords, Model model, Principal principal) {
        List<Product> searchResults;

        if (keywords != null && !keywords.trim().isEmpty()) {
            searchResults = productRepository.searchGlobalActive(keywords);
        } else {
            searchResults = new ArrayList<>();
        }

        model.addAttribute("productList", searchResults);
        model.addAttribute("keywords", keywords);

        addWishlistStatus(model, principal); // ADDED
        return "client/search_page";
    }







    @GetMapping("/user_profile_update")
    public String showUserProfileUpdate() {
        return "client/user_profile_update"; // templates/user_profile_update.html
    }

    @GetMapping("/product/view")
    public String viewProduct(@RequestParam("id") Long id, Model model, Principal principal) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid product Id:" + id));

        if (!product.isActive()) {
            return "redirect:/client/shop?error=Product+is+currently+unavailable";
        }

        List<Product> related = productRepository.findByCategory(product.getCategory());
        List<Product> filteredRelated = (related != null) ? related.stream()
                .filter(p -> p.isActive() && !p.getId().equals(id))
                .limit(4)
                .toList() : new ArrayList<>();

        model.addAttribute("product", product);
        model.addAttribute("relatedProducts", filteredRelated);

        addWishlistStatus(model, principal); // ADDED
        return "client/view_page";
    }

    @ModelAttribute("currentUser")
    public Client addCurrentUserToModel(Principal principal) {
        if (principal != null) {
            return clientRepo.findByEmail(principal.getName()).orElse(new Client());
        }
        return new Client();
    }

    private String saveImage(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) return null;

        // Save to the ROOT uploads folder (outside src) so MvcConfig sees it instantly
        String uploadDir = System.getProperty("user.dir") + "/uploads/";
        String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        Path path = Paths.get(uploadDir + fileName);

        if (!Files.exists(path.getParent())) {
            Files.createDirectories(path.getParent());
        }

        Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
        return fileName;
    }

    @PostMapping("/update_profile")
    public String updateClientProfile(
            @ModelAttribute Client clientData,
            Principal principal,
            @RequestParam("imageFile") MultipartFile imageFile) throws IOException {

        String email = principal.getName();
        Client currentClient = clientService.getClientByEmail(email);

        // Image handling logic as established before
        if (imageFile != null && !imageFile.isEmpty()) {
            clientData.setImage(saveImage(imageFile));
        } else {
            clientData.setImage(currentClient.getImage());
        }

        // This now carries the 'contact' field automatically
        clientService.updateClient(currentClient.getId(), clientData);

        return "redirect:/client/index?message=Profile Updated";
    }
}