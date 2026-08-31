package com.farm.Controller;

import com.farm.Entity.*;
import com.farm.Repository.*;
import com.farm.Services.ClientService;
import com.farm.Services.FarmerService;
import com.farm.Services.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private ClientService clientService;

    @Autowired
    private FarmerService farmerService;

    @Autowired
    private AdminRepository adminRepo;

    @Autowired
    private FarmerVerificationRepository verificationRepo;

    @Autowired
    private FarmerRepository farmerRepo;

    @Autowired
    private MessageRepository messageRepo;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private ContactRepository contactRepo;

    @Autowired
    private OrderRepository orderRepository;



    // Change this in your Controller(s)
    private static final String UPLOAD_DIR = System.getProperty("user.dir") + File.separator + "uploads" + File.separator;

    @GetMapping("/index")
    public String adminDashboard(
            @RequestParam(value = "farmerId", required = false) Long farmerId,
            @RequestParam(value="categorySuccess", required=false) String categorySuccess,
            Model model, Principal principal) {

        long pendingCount = farmerRepo.countByVerifiedFalse();
        model.addAttribute("pendingFarmersCount", pendingCount);




        if(categorySuccess != null) {
            model.addAttribute("successMsg", "Category saved successfully!");
        }

        if (principal == null) return "redirect:/login";

        Admin admin = adminRepo.findByEmail(principal.getName()).orElse(null);
        if (admin == null) return "redirect:/login?error=invalidAdminSession";

        if (farmerId != null) {
            Optional<FarmerVerification> verification = verificationRepo.findByFarmerId(farmerId);
            if (verification.isPresent()) {
                Farmer selectedFarmer = verification.get().getFarmer(); // Get the actual Farmer entity

                List<FarmerVerification> verificationList = new ArrayList<>();
                verificationList.add(verification.get());
                model.addAttribute("verificationList", verificationList);
                model.addAttribute("showPanel", true);

                // --- NEW: Fetch full conversation for THIS specific farmer ---
                // This gets both Admin and Farmer messages in chronological order
                List<Message> conversation = messageRepo.findByFarmerOrderByCreatedAtAsc(selectedFarmer);
                model.addAttribute("conversation", conversation);

            } else {
                model.addAttribute("errorMsg", "No land records found for this farmer.");
                model.addAttribute("showPanel", false);
            }
        } else {
            model.addAttribute("showPanel", false);
        }

        // --- GLOBAL NOTIFICATION LOGIC (For the Top Bell Icon) ---
        List<Message> farmerReplies = messageRepo.findTop20BySenderTypeOrderByCreatedAtDesc("FARMER");
        long unreadCount = farmerReplies.stream().filter(m -> !m.isRead()).count();

        model.addAttribute("adminNotifications", farmerReplies);
        model.addAttribute("notifCount", unreadCount);

        model.addAttribute("currentAdmin", admin);
        model.addAttribute("title", "Admin Control Panel");
        return "admin/index";
    }// Endpoint for Admin to clear their notifications (Farmer Replies)
    @PostMapping("/mark-replies-read")
    @ResponseBody
    public ResponseEntity<String> markRepliesAsRead() {
        List<Message> unreadReplies = messageRepo.findBySenderTypeAndIsReadFalse("FARMER");
        for (Message m : unreadReplies) {
            m.setRead(true);
        }
        messageRepo.saveAll(unreadReplies);
        return ResponseEntity.ok("Admin notifications cleared");
    }

    @PostMapping("/update-status-accept/{id}")
    @Transactional
    public String acceptStatus(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        FarmerVerification v = verificationRepo.findById(id).orElse(null);
        if (v != null) {
            v.setStatus("APPROVED");
            v.getFarmer().setVerified(true);
            verificationRepo.save(v);
            farmerRepo.save(v.getFarmer());

            Message msg = new Message();
            msg.setContent("Your land verification has been APPROVED. You can now start selling.");
            msg.setFarmer(v.getFarmer());
            msg.setSenderType("ADMIN"); // Correctly identifies admin as sender
            msg.setRead(false);
            msg.setCreatedAt(LocalDateTime.now());
            messageRepo.save(msg);

            redirectAttributes.addFlashAttribute("successMsg", "Farmer Account Approved!");
            return "redirect:/admin/index?farmerId=" + v.getFarmer().getId();
        }
        return "redirect:/admin/index";
    }

    @PostMapping("/update-status-reject/{id}")
    @Transactional
    public String rejectStatus(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        FarmerVerification v = verificationRepo.findById(id).orElse(null);
        if (v != null) {
            v.setStatus("REJECTED");
            v.getFarmer().setVerified(false);
            verificationRepo.save(v);
            farmerRepo.save(v.getFarmer());

            Message msg = new Message();
            msg.setContent("Your land verification was REJECTED. Please check land records and re-submit.");
            msg.setFarmer(v.getFarmer());
            msg.setSenderType("ADMIN"); // Correctly identifies admin as sender
            msg.setRead(false);
            msg.setCreatedAt(LocalDateTime.now());
            messageRepo.save(msg);

            redirectAttributes.addFlashAttribute("successMsg", "Farmer Account Rejected.");
            return "redirect:/admin/index?farmerId=" + v.getFarmer().getId();
        }
        return "redirect:/admin/index";
    }

    @PostMapping("/send-message/{id}")
    @Transactional
    public String sendMessage(@PathVariable Long id, @RequestParam("adminMessage") String adminMessage, RedirectAttributes redirectAttributes) {
        FarmerVerification v = verificationRepo.findById(id).orElse(null);
        if (v == null) return "redirect:/admin/index";

        try {
            v.setAdminRemark(adminMessage);
            verificationRepo.save(v);

            Message msg = new Message();
            msg.setContent(adminMessage);
            msg.setFarmer(v.getFarmer());
            msg.setSenderType("ADMIN"); // Correctly identifies admin as sender
            msg.setRead(false);
            msg.setCreatedAt(LocalDateTime.now());
            messageRepo.save(msg);

            notificationService.sendNotificationEmail(v.getFarmer().getEmail(), v.getStatus(), adminMessage);
            redirectAttributes.addFlashAttribute("successMsg", "Message saved to history and Email sent!");
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMsg", "Database Error: " + e.getMessage());
        }
        return "redirect:/admin/index?farmerId=" + v.getFarmer().getId();
    }

    // --- REMAINDER OF METHODS ---

    @GetMapping("/profile-fragment")
    public String getClientTable(Model model) {
        // 1. Fetch all clients from the service
        List<Client> clients = clientService.getAllClients();

        // 2. Loop through each client to calculate their unread contact messages
        for (Client client : clients) {
            // This count comes from the ContactMessage table
            long count = contactRepo.countByClientIdAndSeenFalse(client.getId());
            client.setUnreadMessages(count);
        }

        model.addAttribute("clients", clients);

        // This matches the file name in the error you saw earlier
        return "admin/fragments/client-table-fragment";
    }

    @GetMapping("/verify-fragment")
    public String getFarmerTable(Model model) {
        List<Farmer> farmers = farmerService.getAllFarmers();
        farmers.removeIf(java.util.Objects::isNull);
        model.addAttribute("farmers", farmers);
        return "admin/fragments/farmer-table-fragment";
    }

    @GetMapping("/edit-form/{type}/{id}")
    public String getEditForm(@PathVariable String type, @PathVariable Long id, Model model) {
        if ("client".equalsIgnoreCase(type)) {
            model.addAttribute("client", clientService.getClientById(id));
            return "admin/fragments/customer-edit-form-fragment";
        } else {
            model.addAttribute("farmer", farmerService.getFarmerById(id));
            return "admin/fragments/farmer-edit-form-fragment";
        }
    }

    @PostMapping("/update/client/{id}")
    public String updateClient(@PathVariable Long id,
                               @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                               @RequestParam(value = "verified", defaultValue = "false") boolean verified,
                               @ModelAttribute("client") Client incomingData) throws IOException {

        // 1. Handle the image saving if a file exists
        if (imageFile != null && !imageFile.isEmpty()) {
            incomingData.setImage(saveImage(imageFile));
        }

        // 2. Set the verified boolean into the incomingData object
        incomingData.setVerified(verified);

        // 3. Call the service (The service handles fetching the existing record and saving)
        clientService.updateClient(id, incomingData);

        return "redirect:/admin/index?clientUpdateSuccess=true";
    }

    @PostMapping("/update/farmer/{id}")
    public String updateFarmer(@PathVariable Long id, @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                               @RequestParam(value = "verified", defaultValue = "false") boolean verified,
                               @ModelAttribute Farmer farmerData) throws IOException {
        Farmer existingFarmer = farmerService.getFarmerById(id);
        existingFarmer.setName(farmerData.getName());
        existingFarmer.setContact(farmerData.getContact());
        existingFarmer.setGender(farmerData.getGender());
        existingFarmer.setVerified(verified);
        if (imageFile != null && !imageFile.isEmpty()) existingFarmer.setImage(saveImage(imageFile));
        farmerService.updateFarmer(id, existingFarmer);
        return "redirect:/admin/index?updateSuccess=true";
    }

    @GetMapping("/delete/{userType}/{id}")
    @Transactional // Add this here
    public String deleteUser(@PathVariable String userType, @PathVariable Long id) {
        if ("farmer".equalsIgnoreCase(userType)) {
            farmerService.deleteFarmer(id);
        } else {
            clientService.deleteClient(id);
        }
        return "redirect:/admin/index";
    }

    private String saveImage(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) return "default.png";
        String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        Path path = Paths.get(UPLOAD_DIR + fileName);
        if (!Files.exists(path.getParent())) Files.createDirectories(path.getParent());
        Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
        return fileName;
    }

    @GetMapping("/orders/farmer/{farmerId}")
    public String getFarmerOrders(@PathVariable Long farmerId, Model model) {
        List<Order> farmerOrders = orderRepository.getOrdersByFarmerId(farmerId);
        model.addAttribute("orders", farmerOrders);
        model.addAttribute("farmerName", farmerService.getFarmerById(farmerId).getName());

        // This MUST match the path to your fragment file
        return "admin/fragments/order-list :: orderTable";
    }

    @GetMapping("/orders/{orderId}/items")
    @ResponseBody
    public List<Map<String, Object>> getOrderItems(
            @PathVariable Long orderId,
            @RequestParam Long farmerId) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        return order.getOrderItems().stream()
                .filter(item -> item.getProduct().getFarmer().getId().equals(farmerId))
                .map(item -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("productName", item.getProduct().getProductName());

                    // --- FIX FOR ARRAY IMAGE STRING ---
                    String rawImage = item.getProduct().getImage();
                    String cleanImage = "no-image.png";

                    if (rawImage != null && !rawImage.isEmpty()) {
                        // Remove brackets and quotes if it's a JSON array string
                        cleanImage = rawImage.replace("[", "")
                                .replace("]", "")
                                .replace("\"", "")
                                .split(",")[0].trim(); // Take the first image only
                    }
                    map.put("productImage", cleanImage);
                    // -----------------------------------

                    map.put("price", item.getPriceAtPurchase());
                    map.put("quantity", item.getQuantity());
                    map.put("subTotal", item.getPriceAtPurchase() * item.getQuantity());
                    return map;
                }).collect(Collectors.toList());
    }


}