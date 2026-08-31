package com.farm.Services;

import com.farm.Entity.Admin;
import com.farm.Repository.AdminRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AdminService {

    @Autowired
    private AdminRepository adminRepo;
    @Autowired
    private PasswordEncoder passwordEncoder;

    public Admin saveAdmin(Admin admin)
    {
        admin.setPassword(passwordEncoder.encode(admin.getPassword()));
        return adminRepo.save(admin);
    }
}
