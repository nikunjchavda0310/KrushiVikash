package com.farm.Services;

import com.farm.Entity.Admin;
import com.farm.Entity.Client;
import com.farm.Entity.Farmer;
import com.farm.Repository.AdminRepository;
import com.farm.Repository.ClientRepository;
import com.farm.Repository.FarmerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private FarmerRepository farmerRepo;

    @Autowired
    private ClientRepository clientRepo;

    @Autowired
    private AdminRepository adminRepo;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        String userType = (attributes != null) ? attributes.getRequest().getParameter("user_type") : null;

        System.out.println("--- LOGIN ATTEMPT ---");
        System.out.println("Email: " + email + " | UserType: " + userType);

        if (userType == null || userType.isEmpty()) {
            throw new UsernameNotFoundException("Please select a user type.");
        }

        // --- FARMER LOGIC ---
        if ("farmer".equalsIgnoreCase(userType)) {
            Farmer farmer = farmerRepo.findByEmail(email)
                    .orElseThrow(() -> new UsernameNotFoundException("Farmer not found with email: " + email));

            if (!farmer.isVerified()) {
                throw new DisabledException("Your account is not verified.");
            }

            System.out.println("Successfully authenticated Farmer: " + email);
            return User.builder()
                    .username(farmer.getEmail())
                    .password(farmer.getPassword())
                    .authorities("ROLE_FARMER") // Explicitly setting authority
                    .build();
        }

        // --- CLIENT LOGIC ---
        else if ("client".equalsIgnoreCase(userType)) {
            Client client = clientRepo.findByEmail(email)
                    .orElseThrow(() -> new UsernameNotFoundException("Client not found with email: " + email));

            if (!client.isVerified()) {
                throw new DisabledException("Your account is not verified.");
            }

            System.out.println("Successfully authenticated Client: " + email);
            return User.builder()
                    .username(client.getEmail())
                    .password(client.getPassword())
                    .authorities("ROLE_CLIENT") // Explicitly setting authority
                    .build();
        }

        // --- ADMIN LOGIC ---
        else if ("admin".equalsIgnoreCase(userType)) {
            Admin admin = adminRepo.findByEmail(email)
                    .orElseThrow(() -> new UsernameNotFoundException("Admin record not found."));

            System.out.println("Successfully authenticated Admin: " + email);
            return User.builder()
                    .username(admin.getEmail())
                    .password(admin.getPassword())
                    .authorities("ROLE_ADMIN") // Explicitly setting authority
                    .build();
        }

        throw new UsernameNotFoundException("User type invalid");
    }
}