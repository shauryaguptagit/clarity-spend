package com.example.clarityspend;

import com.fasterxml.jackson.annotation.JsonIgnore; // <-- NEW IMPORT
import jakarta.persistence.*; // <-- IMPORT ALL FROM jakarta.persistence

@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String description;
    private Double amount;
    private String category;

    // --- NEW FIELD ---
    // This links the transaction to a User
    // Many transactions can belong to One user
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonIgnore // Prevents this from being sent in JSON and causing loops
    private User user;
    
    // --- Getters and Setters (for all fields) ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    // Getter/Setter for the new User field
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
}