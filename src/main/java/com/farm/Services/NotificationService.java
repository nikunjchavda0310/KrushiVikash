package com.farm.Services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    @Autowired
    private JavaMailSender mailSender;

    // This pulls your email from application.properties
    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendOtpEmail(String toEmail, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();

        // ADD THIS LINE - It fixes the "can't determine local email address" error
        message.setFrom(fromEmail);

        message.setTo(toEmail);
        message.setSubject("Your KrushiVikash Verification Code");
        message.setText("Welcome to KrushiVikash! Your OTP for registration is: " + otp);

        mailSender.send(message);
    }

    public void sendNotificationEmail(String farmerEmail, String status, String adminMessage) {
        SimpleMailMessage message = new SimpleMailMessage();

        // Use the variable from properties instead of hardcoding
        message.setFrom(fromEmail);

        message.setTo(farmerEmail);
        message.setSubject("KrushiVikash: Account Verification Update");

        String emailContent = "Hello Farmer,\n\n" +
                "Your account verification status has been updated to: " + status + ".\n\n" +
                "Admin Remarks: " + (adminMessage == null || adminMessage.isEmpty() ? "No additional remarks." : adminMessage) + "\n\n" +
                "Please login to your dashboard to see the changes.\n\n" +
                "Regards,\nKrushiVikash Team";

        message.setText(emailContent);
        mailSender.send(message);
    }
}