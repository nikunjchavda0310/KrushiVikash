package com.farm.Config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import java.io.File;

@Configuration
public class MvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String userDir = System.getProperty("user.dir");

        // Correct formatting for Windows Absolute Paths
        String path = "file:///" + userDir.replace("\\", "/") + "/uploads/";

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(path)
                .setCachePeriod(0);

        System.out.println("DEBUG: Serving uploads from: " + path);
    }
}