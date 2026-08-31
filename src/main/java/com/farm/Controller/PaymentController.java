package com.farm.Controller;

import com.farm.Entity.CartItem;
import com.farm.Entity.Client;
import com.farm.Entity.Order;
import com.farm.Entity.OrderItem;
import com.farm.Repository.CartRepository;
import com.farm.Repository.ClientRepository;
import com.farm.Repository.OrderItemRepository;
import com.farm.Repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
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
public class PaymentController {

    @Autowired private OrderRepository orderRepository;
    @Autowired private OrderItemRepository orderItemRepository;
    @Autowired private CartRepository cartItemRepository;
    @Autowired private ClientRepository clientRepository;

    @PostMapping("/checkout/place_order")
    @Transactional
    public String placeOrder(
            @RequestParam String name, @RequestParam String number,
            @RequestParam String email, @RequestParam String address,
            @RequestParam String method,
            @RequestParam(required = false) String transactionId,
            Principal principal, RedirectAttributes redirectAttributes, Model model) {

        // --- 1. SERVER SIDE VALIDATION ---
        if ("upi".equalsIgnoreCase(method)) {
            if (transactionId == null || !transactionId.matches("^\\d{12}$")) {
                return "redirect:/client/checkout?error=invalid_utr";
            }
        }

        String clientEmail = principal.getName();
        Client client = clientRepository.findByEmail(clientEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Update Client Info
        client.setName(name);
        client.setContact(number);
        client.setAddress(address);
        clientRepository.save(client);

        List<CartItem> cartItems = cartItemRepository.findByClient(client);
        if (cartItems.isEmpty()) return "redirect:/client/cart?error=empty";

        // Create Order
        Order order = new Order();
        order.setClient(client);
        order.setOrderDate(LocalDateTime.now());
        order.setShippingName(name);
        order.setShippingPhone(number);
        order.setShippingAddress(address);
        order.setShippingEmail(email);
        order.setPaymentMethod(method);

        // --- 2. STATUS ASSIGNMENT ---
        if ("upi".equalsIgnoreCase(method)) {
            order.setTransactionId(transactionId);
            order.setStatus("Pending Verification");
        } else if ("cash on delivery".equalsIgnoreCase(method)) {
            order.setStatus("PENDING");
        } else {
            order.setStatus("PAID");
        }

        double total = cartItems.stream()
                .mapToDouble(item -> item.getProduct().getPrice() * item.getQuantity()).sum();
        order.setTotalAmount(total);
        Order savedOrder = orderRepository.save(order);

        // Stock Management
        List<OrderItem> orderItemsList = new ArrayList<>();
        for (CartItem cartItem : cartItems) {
            var product = cartItem.getProduct();
            if (product.getStock() < cartItem.getQuantity()) throw new RuntimeException("Out of stock");

            product.setStock(product.getStock() - cartItem.getQuantity());

            OrderItem oi = new OrderItem();
            oi.setOrder(savedOrder);
            oi.setProduct(product);
            oi.setQuantity(cartItem.getQuantity());
            oi.setPriceAtPurchase(product.getPrice());
            orderItemRepository.save(oi);
            orderItemsList.add(oi);
        }

        savedOrder.setOrderItems(orderItemsList);
        orderRepository.save(savedOrder);
        cartItemRepository.deleteByClient(client);

        model.addAttribute("orderId", savedOrder.getId());
        model.addAttribute("total", total);
        return "client/order_success";
    }

    @GetMapping("/client/order/invoice/{id}")
    public String viewInvoice(@PathVariable Long id, Principal principal, Model model) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        String currentUsername = principal.getName();
        if (!order.getClient().getEmail().equals(currentUsername)) {
            return "redirect:/client/orders";
        }

        model.addAttribute("order", order);
        return "client/invoice_print";
    }
}