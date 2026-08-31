package com.farm.Controller;

import com.farm.Entity.Client;
import com.farm.Entity.ClientReply;
import com.farm.Entity.ContactMessage;
import com.farm.Repository.ClientReplyRepository;
import com.farm.Repository.ClientRepository;
import com.farm.Repository.ContactRepository;
import jakarta.persistence.EntityManager; // Add this import
import jakarta.persistence.PersistenceContext; // Add this import
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional; // Add this import
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;


@Controller
@RequestMapping("/admin")
public class ClientChatController {

    @Autowired
    private ContactRepository contactRepo;

    @Autowired
    private ClientRepository clientRepo;

    @Autowired
    private ClientReplyRepository replyRepo;

    @Autowired

    @PersistenceContext
    private EntityManager entityManager; // Inject the EntityManager

    @GetMapping("/client/messages/{id}")
    @Transactional // Ensure we are in a transaction
    public String getClientContactMessages(@PathVariable("id") Long id, Model model) {

        // 1. FORCE THE CACHE TO CLEAR
        // This stops Hibernate from showing the old "null" reply
        entityManager.clear();

        // 2. Fetch the actual LIST of messages (Now it will see the reply!)
        List<ContactMessage> messages = contactRepo.findByClientIdOrderBySubmittedAtAsc(id);

        // 3. Mark as seen
        messages.forEach(m -> m.setSeen(true));
        contactRepo.saveAll(messages);

        Client client = clientRepo.findById(id).orElseThrow();
        model.addAttribute("clientName", client.getName());
        model.addAttribute("contactMessages", messages);

        return "admin/fragments/contact-message-panel :: contactPanel";
    }

    @PostMapping("/client/reply-message/{id}")
    @ResponseBody
    @Transactional
    public ResponseEntity<?> replyToMessage(@PathVariable Long id, @RequestParam("replyText") String replyText) {
        try {
            ContactMessage msg = contactRepo.findById(id).orElse(null);
            if (msg != null) {
                ClientReply newReply = new ClientReply();
                newReply.setMessage(replyText);
                newReply.setSentAt(LocalDateTime.now());
                newReply.setContactMessage(msg);

                // 1. Save and Flush the reply immediately
                replyRepo.saveAndFlush(newReply);

                // 2. Explicitly add to the parent's list so Hibernate sees it immediately
                msg.getReplies().add(newReply);
                msg.setSeen(true);
                contactRepo.saveAndFlush(msg);

                System.out.println("ADMIN SUCCESS: Saved new reply bubble for message ID " + id);
                return ResponseEntity.ok().build();
            }
            return ResponseEntity.status(404).build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }
}