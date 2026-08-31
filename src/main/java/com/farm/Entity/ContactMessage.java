package com.farm.Entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
public class ContactMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 1000)
    private String msg;

    private LocalDateTime submittedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private Client client;

    private boolean seen = false;

    // NEW: One inquiry can have many separate reply bubbles
    @OneToMany(mappedBy = "contactMessage", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sentAt ASC")
    private List<ClientReply> replies = new ArrayList<>();

    public ContactMessage() {
        this.submittedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getMsg() { return msg; }
    public void setMsg(String msg) { this.msg = msg; }

    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }

    public Client getClient() { return client; }
    public void setClient(Client client) { this.client = client; }

    public boolean isSeen() { return seen; }
    public void setSeen(boolean seen) { this.seen = seen; }

    public List<ClientReply> getReplies() { return replies; }
    public void setReplies(List<ClientReply> replies) { this.replies = replies; }
}