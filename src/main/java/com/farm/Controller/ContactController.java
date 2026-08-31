package com.farm.Controller;

import com.farm.Entity.Client;
import com.farm.Entity.ClientReply;
import com.farm.Entity.ContactMessage;
import com.farm.Repository.ClientReplyRepository;
import com.farm.Repository.ContactRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/client")
public class ContactController {

    @Autowired
    private ContactRepository contactRepo;

    @Autowired
    private ClientReplyRepository replyRepo;


    @Autowired
    private com.farm.Repository.ClientRepository userRepo;



    @GetMapping("/contact")
    public String showContactPage(java.security.Principal principal, org.springframework.ui.Model model) {
        if (principal != null) {
            String email = principal.getName();

            // Use .orElse(null) to pass the actual Client object (or null)
            // instead of the Optional wrapper
            var currentUser = userRepo.findByEmail(email).orElse(null);

            model.addAttribute("currentUser", currentUser);
        }
        return "client/contact";
    }

    @PostMapping("/contact")
    @ResponseBody
    public ResponseEntity<String> processContactForm(
            @RequestParam("msg") String msg,
            java.security.Principal principal) { // Added Principal here

        try {
            ContactMessage message = new ContactMessage();
            message.setMsg(msg);
            message.setSubmittedAt(LocalDateTime.now());

            // --- NEW LOGIC TO FIX THE NULL ID ---
            if (principal != null) {
                String email = principal.getName();
                // Fetch the client object from your userRepo
                var currentUser = userRepo.findByEmail(email).orElse(null);

                // Link the client to the message
                message.setClient(currentUser);
            }
            // ------------------------------------

            contactRepo.save(message);
            return ResponseEntity.ok("success");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/reply-message/{id}")
    @ResponseBody
    @Transactional
    public ResponseEntity<?> replyToMessage(@PathVariable Long id, @RequestParam("replyText") String replyText) {
        try {
            // 1. Find the original inquiry
            ContactMessage msg = contactRepo.findById(id).orElse(null);

            if (msg != null) {
                // 2. Create a NEW Reply Bubble object (The new way)
                ClientReply newReply = new ClientReply();
                newReply.setMessage(replyText);
                newReply.setSentAt(LocalDateTime.now());
                newReply.setContactMessage(msg); // Link it to the inquiry

                // 3. Save the reply bubble to the new table
                replyRepo.saveAndFlush(newReply);

                // 4. Update the inquiry status
                msg.setSeen(true);
                contactRepo.saveAndFlush(msg);

                System.out.println("SUCCESS: New reply bubble saved for Message ID: " + id);
                return ResponseEntity.ok().build();
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Message not found");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }


    @PostMapping("/reply-contact/{id}")
    public String replyToContact(@PathVariable Long id, @RequestParam("reply") String reply, Model model) {
        // 1. Find the original inquiry
        ContactMessage message = contactRepo.findById(id).orElse(null);

        if (message != null) {
            // 2. Create and Save a new Reply Entity (The new way)
            ClientReply newReply = new ClientReply();
            newReply.setMessage(reply);
            newReply.setSentAt(LocalDateTime.now());
            newReply.setContactMessage(message);

            // Save the bubble to the new table
            replyRepo.save(newReply);

            // 3. Mark the inquiry as seen
            message.setSeen(true);
            contactRepo.save(message);

            // 4. Prepare data to reload the fragment
            Client client = message.getClient();
            model.addAttribute("clientName", client.getName());

            // Fetch fresh messages including the new reply bubbles
            model.addAttribute("contactMessages", contactRepo.findByClientIdOrderBySubmittedAtAsc(client.getId()));
        }

        // 5. Return the fragment name
        return "admin/fragments/contact-message-panel :: contactPanel";
    }

    @ModelAttribute
    public void addNotifications(Model model, java.security.Principal principal) {
        if (principal != null) {
            String email = principal.getName();
            var currentUser = userRepo.findByEmail(email).orElse(null);
            if (currentUser != null) {
                // Fetch messages specifically for this client
                List<ContactMessage> messages = contactRepo.findByClientIdOrderBySubmittedAtAsc(currentUser.getId());
                model.addAttribute("clientSentMessages", messages);

                // Calculate total unread (optional, if you have a seen/unread boolean)
                long unreadCount = messages.stream().flatMap(m -> m.getReplies().stream()).count();
                model.addAttribute("notifCount", unreadCount);
            }
        } else {
            model.addAttribute("clientSentMessages", new ArrayList<>());
            model.addAttribute("notifCount", 0);
        }
    }

    @PostMapping("/reset-notif-count")
    @ResponseBody
    public void resetNotifCount(HttpSession session, Principal principal) {
        if (principal != null) {
            userRepo.findByEmail(principal.getName()).ifPresent(user -> {
                Integer currentTotal = (Integer) session.getAttribute("lastKnownTotal_" + user.getId());
                session.setAttribute("seenReplyCount_" + user.getId(), currentTotal != null ? currentTotal : 0);
            });
        }
    }
}