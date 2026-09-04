package com.orderflow.orderflow.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "inventory", uniqueConstraints = @UniqueConstraint(columnNames = "product_id"))
public class Inventory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false, unique = true)
    private Product product;
    @Column(nullable = false)
    private int quantity;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Inventory() {}
    public Inventory(Product product, int quantity) { this.product = product; setQuantity(quantity); }
    @PrePersist @PreUpdate void updateTimestamp() { updatedAt = Instant.now(); }
    public void setQuantity(int quantity) {
        if (quantity < 0) throw new IllegalArgumentException("Inventory quantity cannot be negative");
        this.quantity = quantity;
    }
    public Long getId() { return id; }
    public Product getProduct() { return product; }
    public int getQuantity() { return quantity; }
    public Instant getUpdatedAt() { return updatedAt; }
}
