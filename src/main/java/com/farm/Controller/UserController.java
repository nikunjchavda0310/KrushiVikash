package com.farm.Controller;

import com.farm.Entity.Client;
import com.farm.Entity.Farmer;
import com.farm.Repository.ClientRepository;
import com.farm.Repository.FarmerRepository;
import com.farm.Services.ClientService;
import com.farm.Services.FarmerService;
import com.farm.Services.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.Random;

@Controller
public class UserController {

    @Autowired
    private FarmerService farmerService;

    @Autowired
    private ClientService clientService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private FarmerRepository farmerRepo;

    @Autowired
    private ClientRepository clientRepo;

    private static final String UPLOAD_DIR = System.getProperty("user.dir") + File.separator + "uploads" + File.separator;
    // 1. SHOW VERIFICATION PAGE
    // This is where the user lands after clicking 'Register'
    @GetMapping("/verify")
    public String showVerifyPage(@RequestParam("email") String email,
                                 @RequestParam("type") String type,
                                 Model model) {
        model.addAttribute("email", email);
        model.addAttribute("type", type);
        return "verify";
    }

    // 2. REGISTRATION LOGIC
    @PostMapping("/register")
    public String registerUser(@RequestParam("name") String name,
                               @RequestParam("email") String email,
                               @RequestParam("pass") String pass,
                               @RequestParam("gender") String gender,
                               @RequestParam(required = false) String address,
                               @RequestParam("contact") String contact,
                               @RequestParam("user_type") String user_type,
                               @RequestParam("image") MultipartFile imageFile,
                               Model model) throws IOException {

        // 1. Check for Duplicate Email FIRST
        boolean emailExists = false;
        if ("farmer".equalsIgnoreCase(user_type)) {
            emailExists = farmerRepo.existsByEmail(email); // Ensure this method exists in your service
        } else {
            emailExists = clientRepo.existsByEmail(email); // Ensure this method exists in your service
        }

        if (emailExists) {
            model.addAttribute("error", "This email is already registered. Please use a different one or login.");
            return "register"; // Send them back to the form
        }

        // 2. Proceed with registration if email is unique
        String otp = String.valueOf(new Random().nextInt(900000) + 100000);
        String fileName = saveImage(imageFile);

        try {
            if ("farmer".equalsIgnoreCase(user_type)) {
                Farmer f = new Farmer();
                f.setName(name);
                f.setEmail(email);
                f.setPassword(pass);
                f.setContact(contact);
                f.setGender(gender);
                f.setImage(fileName);
                f.setOtp(otp);
                f.setVerified(false);
                farmerService.saveFarmer(f);
            } else {
                Client c = new Client();
                c.setName(name);
                c.setEmail(email);
                c.setPassword(pass);
                c.setAddress(address);
                c.setContact(contact);
                c.setGender(gender);
                c.setImage(fileName);
                c.setOtp(otp);
                c.setVerified(false);
                clientService.saveClient(c);
            }

            notificationService.sendOtpEmail(email, otp);
            return "redirect:/verify?email=" + email + "&type=" + user_type;

        } catch (Exception e) {
            model.addAttribute("error", "Registration failed: " + e.getMessage());
            return "register";
        }
    }
    // 3. OTP VERIFICATION LOGIC
    @PostMapping("/verify-otp")
    public String verify(@RequestParam String email, @RequestParam String type, @RequestParam String otp) {
        System.out.println("DEBUG: Verifying Email: " + email + " | Type: " + type + " | OTP: " + otp);

        boolean success = false;

        if ("farmer".equalsIgnoreCase(type)) {
            Farmer f = farmerRepo.findByEmail(email).orElse(null);
            // Check if farmer exists AND the OTP matches
            if (f != null && otp.trim().equals(f.getOtp())) {
                f.setVerified(true);
                f.setOtp(null);
                farmerRepo.saveAndFlush(f);
                System.out.println("DEBUG: Farmer table updated in DB!");
                success = true;
            }
        } else {
            Client c = clientRepo.findByEmail(email).orElse(null);
            // Check if client exists AND the OTP matches
            if (c != null && otp.trim().equals(c.getOtp())) {
                c.setVerified(true);
                c.setOtp(null);
                clientRepo.saveAndFlush(c);
                System.out.println("DEBUG: Client table updated in DB!");
                success = true;
            }
        }

        if (success) {
            // If OTP matched, go to login
            return "redirect:/login?success=verified";
        } else {
            // If OTP was WRONG, go back to verify page with error parameter
            System.out.println("DEBUG: Wrong OTP entered for " + email);
            return "redirect:/verify?email=" + email + "&type=" + type + "&error=true";
        }
    }
    // --- DASHBOARD & CRUD METHODS REMAIN THE SAME ---
    @GetMapping("/admin/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("farmers", farmerService.getAllFarmers());
        model.addAttribute("clients", clientService.getAllClients());
        return "dashboard";
    }

    @PostMapping("/update/client/{id}")
    public String updateClient(
            @PathVariable Long id,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
            @RequestParam(value = "verified", defaultValue = "false") boolean verified,
            @ModelAttribute Client clientData
    ) throws IOException {

        // 1. Ensure the ID is set for the update
        clientData.setId(id);
        clientData.setVerified(verified);

        // 2. Handle Image Logic
        if (imageFile != null && !imageFile.isEmpty()) {
            // Save new image using your existing saveImage method
            clientData.setImage(saveImage(imageFile));
        } else {
            // Retain existing image if no new file is selected
            Client existingClient = clientService.getClientById(id);
            clientData.setImage(existingClient.getImage());
        }

        // 3. Update in Database
        clientService.updateClient(id, clientData);

        return "redirect:/admin/index?updateSuccess=true";
    }

    @GetMapping("/delete/{userType}/{id}")
    public String delete(@PathVariable String userType, @PathVariable Long id) {
        if ("farmer".equalsIgnoreCase(userType)) farmerService.deleteFarmer(id);
        else clientService.deleteClient(id);
        return "redirect:/admin/dashboard";
    }

    private String saveImage(MultipartFile file) throws IOException {
        // Return the name of your default image if no file is uploaded
        if (file == null || file.isEmpty()) {
            return "pic-1.png";
        }

        // 2. Create a unique file name
        String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();

        // 3. Use Path.resolve for cleaner path handling
        Path uploadPath = Paths.get(UPLOAD_DIR);

        // 4. Create directory if it doesn't exist
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // 5. Save the file to the 'uploads' folder
        Path filePath = uploadPath.resolve(fileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        return fileName;
    }
}