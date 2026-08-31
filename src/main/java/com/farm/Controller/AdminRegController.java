package com.farm.Controller;

import com.farm.Entity.Admin;
import com.farm.Services.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/reg")
public class AdminRegController {
    @Autowired
    private AdminService service;

    @PostMapping("/save")
    public Admin saveAdmin(@RequestBody Admin admin)
    {
        return service.saveAdmin(admin);
    }
}
