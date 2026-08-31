package com.farm.Controller;

import com.farm.Entity.*;
import com.farm.Repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/farmer")
public class FarmerController {

    @Autowired
    private FarmerRepository farmerRepo;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private StateRepository stateRepo;

    @Autowired
    private DistrictRepository districtRepo;

    @Autowired
    private TalukaRepository talukaRepo;

    @Autowired
    private FarmerVerificationRepository verificationRepo;

    @Autowired
    private MessageRepository messageRepo;

    @Autowired
    private CategoryRepository categoryRepo;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private FarmerVerificationRepository farmerVerificationRepository;

    /**
     * DASHBOARD MAIN PAGE
     */
    @GetMapping("/index")
    public String index(
            @RequestParam(value = "loginSuccess", required = false) String loginSuccess,
            @RequestParam(value = "updateSuccess", required = false) String updateSuccess,
            @RequestParam(value = "error", required = false) String error,
            Model model,
            Principal principal) {



        String email = principal.getName();
        Optional<Farmer> farmer1 = farmerRepo.findByEmail(email);

        if (farmer1.isPresent()) {
            // Count orders that the farmer hasn't "processed" yet
            // Adjust the status string ("PENDING") to match your database
            long pendingCount = orderItemRepository.countByProductFarmerIdAndOrderStatus(farmer1.get().getId(), "PENDING");

            model.addAttribute("pendingOrders", pendingCount);
            model.addAttribute("farmer", farmer1.get());
        }

        // 1. Security Check
        if (principal == null) return "redirect:/login";

        // 2. Fetch Farmer Data
        Farmer farmer = farmerRepo.findByEmail(principal.getName()).orElse(null);
        if (farmer == null) {
            return "redirect:/login?error=invalidSession";
        }

        // 3. Fetch Verification Status
        Optional<FarmerVerification> verificationOpt = verificationRepo.findByFarmerId(farmer.getId());
        FarmerVerification verification = verificationOpt.orElse(null);
        model.addAttribute("verification", verification);

        // 4. Fetch Admin Messages
        List<Message> messages = messageRepo.findByFarmerAndSenderTypeOrderByCreatedAtDesc(farmer, "ADMIN");
        long unreadCount = messages.stream().filter(m -> !m.isRead()).count();
        model.addAttribute("messages", messages);
        model.addAttribute("unreadCount", unreadCount);

        // 5. Add States
        model.addAttribute("states", stateRepo.findAll());

        // 6. Handle Alert Messages
        if ("true".equals(loginSuccess)) model.addAttribute("successMsg", "Welcome back, " + farmer.getName() + "!");
        if ("true".equals(updateSuccess)) model.addAttribute("successMsg", "Profile synchronized.");
        if ("noAccess".equals(error)) model.addAttribute("errorMsg", "Verification required.");

        // --- 7. NEW: DYNAMIC SIDEBAR CATEGORIES ---
        // This fetches the categories like 'Fruits', 'Vegitables' that have no parent
        List<Category> allCategories = categoryRepo.findAll();
        model.addAttribute("categories", allCategories);
        // ------------------------------------------

        // 8. Populate Global Model Attributes
        model.addAttribute("farmer", farmer);
        model.addAttribute("title", "Farmer Dashboard | KrushiVikash");

        return "farmer/index";
    }
    @PostMapping("/send-reply")
    @ResponseBody
    public ResponseEntity<String> sendReply(@RequestBody Map<String, String> payload, Principal principal) {
        Farmer farmer = farmerRepo.findByEmail(principal.getName()).orElse(null);
        if (farmer != null) {
            Message reply = new Message();
            reply.setContent(payload.get("message"));
            reply.setFarmer(farmer);
            reply.setSenderType("FARMER"); // Correctly tag this as a farmer reply
            reply.setCreatedAt(LocalDateTime.now());
            reply.setRead(false); // Admin hasn't read it yet

            messageRepo.save(reply);
            return ResponseEntity.ok("Success");
        }
        return ResponseEntity.badRequest().build();
    }

    @PostMapping("/messages/mark-as-read")
    @ResponseBody
    public ResponseEntity<String> markAsRead(Principal principal) {
        Farmer farmer = farmerRepo.findByEmail(principal.getName()).orElse(null);
        if (farmer != null) {
            // Only mark messages from ADMIN as read for the farmer
            List<Message> unreadMessages = messageRepo.findByFarmerAndSenderTypeAndIsReadFalse(farmer, "ADMIN");
            for (Message m : unreadMessages) {
                m.setRead(true);
            }
            messageRepo.saveAll(unreadMessages);
            return ResponseEntity.ok("Success");
        }
        return ResponseEntity.badRequest().build();
    }

    /**
     * PROFILE FRAGMENT
     */
    @GetMapping("/profile-fragment")
    public String getProfileFragment(Model model, Principal principal) {
        Farmer farmer = farmerRepo.findByEmail(principal.getName()).orElse(null);
        model.addAttribute("farmer", farmer);
        return "farmer/fragments :: profile-table";
    }

    /**
     * EDIT PROFILE FRAGMENT
     */
    @GetMapping("/edit-profile-fragment")
    public String getEditFragment(Model model, Principal principal) {
        Farmer farmer = farmerRepo.findByEmail(principal.getName()).orElse(null);
        model.addAttribute("farmer", farmer);
        return "farmer/fragments :: profile-form";
    }

    /**
     * VERIFICATION FRAGMENT (Logic for Form vs Status View)
     */
    @GetMapping("/verify-fragment")
    public String getVerifyFragment(@RequestParam(value = "edit", required = false) Boolean edit,
                                    Model model, Principal principal) {

        Farmer farmer = farmerRepo.findByEmail(principal.getName()).orElse(null);
        if (farmer == null) return "redirect:/login";
        model.addAttribute("farmer", farmer);

        // Fetch existing verification
        Optional<FarmerVerification> existing = verificationRepo.findByFarmerId(farmer.getId());

        if (existing.isPresent()) {
            FarmerVerification v = existing.get();
            model.addAttribute("verification", v);

            // MODE 1: Edit Mode
            if (Boolean.TRUE.equals(edit)) {
                model.addAttribute("states", stateRepo.findAll());
                if (v.getState() != null) model.addAttribute("districts", districtRepo.findByStateId(v.getState().getId()));
                if (v.getDistrict() != null) model.addAttribute("talukas", talukaRepo.findByDistrictId(v.getDistrict().getId()));

                return "farmer/farmer_update :: update-form";
            }

            // MODE 2: View Mode (Status Table)
            return "farmer/verification_details :: status-table";

        } else {
            // MODE 3: Create Mode (Blank Form)
            model.addAttribute("states", stateRepo.findAll());
            return "farmer/farmer_verify :: verify-form";
        }
    }

    /**
     * POST: UPDATE PROFILE
     */
    @PostMapping("/update-profile")
    public String updateProfile(@RequestParam("name") String name,
                                @RequestParam("phone") String phone,
                                @RequestParam("gender") String gender, // ADDED THIS
                                @RequestParam(value = "newPassword", required = false) String newPassword,
                                @RequestParam("profileImage") MultipartFile file,
                                Principal principal) throws IOException {

        Farmer farmer = farmerRepo.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Update basic details
        farmer.setName(name);
        farmer.setContact(phone);
        farmer.setGender(gender); // SAVE GENDER TO DATABASE

        // Handle Password Update
        if (newPassword != null && !newPassword.isEmpty()) {
            farmer.setPassword(passwordEncoder.encode(newPassword));
        }

        // Handle Image Upload
        if (!file.isEmpty()) {
            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            String userDir = System.getProperty("user.dir");
            Path uploadPath = Paths.get(userDir, "uploads");

            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, uploadPath.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
                farmer.setImage(fileName);
            }
        }

        farmerRepo.save(farmer);
        return "redirect:/farmer/index?updateSuccess=true";
    } /**
     * POST: SUBMIT VERIFICATION
     */
    /**
     * POST: SUBMIT VERIFICATION
     */
    @PostMapping("/submit-verification")
    @org.springframework.transaction.annotation.Transactional
    @ResponseBody
    public String submitVerification(
            @RequestParam(value = "data", required = false) String dataJson,
            @RequestParam(value = "satBaraFile", required = false) MultipartFile satBara,
            @RequestParam(value = "aadhaarFile", required = false) MultipartFile aadhaar,
            Principal principal) {

        if (dataJson == null) {
            return "Error: Request parameter 'data' is missing!";
        }

        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            java.util.Map<String, Object> rawData = mapper.readValue(dataJson, java.util.Map.class);

            Farmer farmer = farmerRepo.findByEmail(principal.getName())
                    .orElseThrow(() -> new RuntimeException("Farmer not found"));

            // --- ADDED: DUPLICATE CHECK LOGIC ---
            String regNumber = (String) rawData.get("regNumber");

            // Check if this regNumber is already used by another farmer
            Optional<FarmerVerification> existingWithReg = verificationRepo.findByRegNumber(regNumber);
            if (existingWithReg.isPresent() && !existingWithReg.get().getFarmer().getId().equals(farmer.getId())) {
                return "This Survey Number Is Already Registered";
            }
            // ------------------------------------

            FarmerVerification verification;

            // --- LOGIC FOR EDIT VS NEW ---
            if (rawData.get("id") != null && !rawData.get("id").toString().isEmpty()) {
                Long id = Long.valueOf(rawData.get("id").toString());
                verification = verificationRepo.findById(id)
                        .orElse(new FarmerVerification());
            } else {
                verification = new FarmerVerification();
            }

            // Set text fields
            verification.setFullName((String) rawData.get("fullName"));
            verification.setVillage((String) rawData.get("village"));
            verification.setPincode((String) rawData.get("pincode"));
            verification.setRegNumber(regNumber); // Using the variable from check above

            if (rawData.get("farmArea") != null) {
                verification.setFarmArea(Double.parseDouble(rawData.get("farmArea").toString()));
            }

            // Location Mapping
            Long sId = rawData.get("state") != null ? Long.valueOf(rawData.get("state").toString()) : null;
            Long dId = rawData.get("district") != null ? Long.valueOf(rawData.get("district").toString()) : null;
            Long tId = rawData.get("taluka") != null ? Long.valueOf(rawData.get("taluka").toString()) : null;

            if (sId != null) verification.setState(stateRepo.findById(sId).orElse(null));
            if (dId != null) verification.setDistrict(districtRepo.findById(dId).orElse(null));
            if (tId != null) verification.setTaluka(talukaRepo.findById(tId).orElse(null));

            verification.setFarmer(farmer);

            // 1. Always update the date to 'Now'
            verification.setSubmissionDate(java.time.LocalDateTime.now());

            if ("REJECTED".equals(verification.getStatus())) {
                verification.setStatus("PENDING");
                verification.setAdminRemark(null);
            }

            // File Handling
            String uploadDir = System.getProperty("user.dir") + File.separator + "uploads" + File.separator + "documents" + File.separator;
            File dir = new File(uploadDir);
            if (!dir.exists()) dir.mkdirs();

            if (satBara != null && !satBara.isEmpty()) {
                String fileName = "712_" + System.currentTimeMillis() + "_" + satBara.getOriginalFilename().replace(" ", "_");
                Files.copy(satBara.getInputStream(), Paths.get(uploadDir + fileName), StandardCopyOption.REPLACE_EXISTING);
                verification.setSatBaraFile(fileName);
            }

            if (aadhaar != null && !aadhaar.isEmpty()) {
                String fileName = "aadhaar_" + System.currentTimeMillis() + "_" + aadhaar.getOriginalFilename().replace(" ", "_");
                Files.copy(aadhaar.getInputStream(), Paths.get(uploadDir + fileName), StandardCopyOption.REPLACE_EXISTING);
                verification.setAadhaarFile(fileName);
            }

            verificationRepo.save(verification);
            return "success";

        } catch (Exception e) {
            e.printStackTrace();
            return "Error: " + e.getMessage();
        }
    }

    @GetMapping("/verify-payment/{id}")
    public String verifyPayment(@PathVariable("id") Long orderId, RedirectAttributes redirectAttributes) {
        // 1. Find the order from your Repository
        Order order = orderRepository.findById(orderId).get();

        // 2. Update the status
        order.setStatus("Paid & Confirmed");

        // 3. Save the changes
        orderRepository.save(order);

        // 4. Send a success message to the Farmer's dashboard
        redirectAttributes.addFlashAttribute("message", "Payment for Order #" + orderId + " has been verified successfully!");

        return "redirect:/farmer/farmer_orders";
    }

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.setDisallowedFields("satBaraFile", "aadhaarFile");
    }


}