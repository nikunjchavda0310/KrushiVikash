package com.farm.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class HomeController {

    @GetMapping("/index")
    public String goIndex() {
        return "index";
    }

    @GetMapping("/about")
    public String getAbout() {
        return "about"; // refers to about.html in templates folder
    }















    @GetMapping("/login")
    public String showLogin() {
        return "login"; // templates/login.html
    }

    @GetMapping("/register")
    public String showRegister() {
        return "register"; // templates/register.html
    }










}
