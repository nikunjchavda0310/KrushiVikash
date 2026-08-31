package com.farm.Services;

import com.farm.Entity.Client;
import com.farm.Entity.Order;
import com.farm.Entity.OrderItem;
import com.farm.Repository.CartRepository;
import com.farm.Repository.ClientRepository;
import com.farm.Repository.WishlistRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ClientService {

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private CartRepository cartItemRepository; // 1. Inject the CartItemRepository

    @Autowired
    private WishlistRepository wishlistRepository;

    public void saveClient(Client client) {
        client.setPassword(passwordEncoder.encode(client.getPassword()));
        clientRepository.save(client);
    }

    public List<Client> getAllClients() {
        return clientRepository.findAll();
    }

    public void updateClient(Long id, Client incomingData) {
        Client existingClient = clientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Client not found with id: " + id));

        // Only update the fields allowed from the Admin Profile Edit form
        existingClient.setName(incomingData.getName());
        existingClient.setAddress(incomingData.getAddress());
        existingClient.setContact(incomingData.getContact());
        existingClient.setGender(incomingData.getGender());
        existingClient.setVerified(incomingData.isVerified());

        // Only update image if a new one was actually uploaded
        if (incomingData.getImage() != null && !incomingData.getImage().isEmpty()) {
            existingClient.setImage(incomingData.getImage());
        }

        // Notice: We NEVER call existingClient.setPassword(...)
        // Therefore, the original password remains untouched in the DB.
        clientRepository.save(existingClient);
    }

    @Transactional
    public void deleteClient(Long id) {
        // Find the client
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Client not found with id: " + id));

        // Step 1: Clear the items in their cart so MySQL doesn't throw a Foreign Key error
        cartItemRepository.deleteByClient(client);

        // Step 2: Clear their wishlist as well, otherwise it will block the deletion next!
        wishlistRepository.deleteByClient(client);
        // Note: Ensure findByClientEmail or deleteByClient is handled in WishlistRepository if needed

        // Step 3: Flush the child table changes out to the database
        clientRepository.saveAndFlush(client);

        // Step 4: Safely delete the client
        clientRepository.delete(client);
    }
    public Client getClientByEmail(String email) {
        return clientRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Client not found"));
    }

    public Client getClientById(Long id) {
        return clientRepository.findById(id).orElseThrow();
    }
}
