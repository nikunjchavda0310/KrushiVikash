package com.farm.Controller;

import com.farm.Entity.ContactMessage;
import com.farm.Repository.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

@ControllerAdvice
public class GlobalControllerAdvice {

    @Autowired private ClientRepository clientRepository;
    @Autowired private ContactRepository contactRepo;
    @Autowired private CartRepository cartRepository;
    @Autowired private WishlistRepository wishlistRepository;

    @ModelAttribute
    public void addGlobalAttributes(Model model, Principal principal, HttpServletRequest request, HttpSession session) {
        String uri = request.getRequestURI();
        if (uri.contains("/error") || uri.contains(".")) return;

        // Set Defaults to prevent Thymeleaf null errors
        model.addAttribute("notifCount", 0);
        model.addAttribute("clientSentMessages", new ArrayList<>());
        model.addAttribute("cartCount", 0);
        model.addAttribute("wishlistCount", 0);

        if (principal != null) {
            String email = principal.getName();
            clientRepository.findByEmail(email).ifPresent(user -> {
                // --- THE CRITICAL FIX ---
                // This line provides the user object needed for the profile image
                model.addAttribute("currentUser", user);

                // Fetch Notifications and sorting logic
                List<ContactMessage> messages = contactRepo.findByClientIdOrderByIdDesc(user.getId());
                messages.forEach(msg -> {
                    if (msg.getReplies() != null) {
                        msg.getReplies().sort((r1, r2) -> r2.getId().compareTo(r1.getId()));
                    }
                });
                model.addAttribute("clientSentMessages", messages);

                // Badge/Notification Logic
                int totalReplies = (int) messages.stream()
                        .filter(m -> m.getReplies() != null)
                        .mapToLong(m -> m.getReplies().size()).sum();

                Integer seenCount = (Integer) session.getAttribute("seenReplyCount_" + user.getId());
                if (seenCount == null) seenCount = 0;

                model.addAttribute("notifCount", Math.max(0, totalReplies - seenCount));
                session.setAttribute("lastKnownTotal_" + user.getId(), totalReplies);

                // Add Cart/Wishlist counts using the user object
                model.addAttribute("cartCount", cartRepository.findByClientEmail(email).size());
                model.addAttribute("wishlistCount", wishlistRepository.countByClientEmail(email));
            });
        }
    }
}