
package com.ebookmarketplace.backend.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "purchases", uniqueConstraints = {
        @UniqueConstraint(name = "uk_purchases_user_ebook", columnNames = { "user_id", "ebook_id" }),
        @UniqueConstraint(name = "uk_purchases_order_item", columnNames = { "order_item_id" })
})
public class Purchase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ebook_id", nullable = false)
    private Ebook ebook;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItem orderItem;

    @Column(name = "purchased_at", nullable = false, updatable = false)
    private Instant purchasedAt;

    protected Purchase() {
    }

    public Purchase(
            User user,
            Ebook ebook,
            OrderItem orderItem) {
        this.user = user;
        this.ebook = ebook;
        this.orderItem = orderItem;
    }

    @PrePersist
    protected void onCreate() {
        purchasedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public Ebook getEbook() {
        return ebook;
    }

    public OrderItem getOrderItem() {
        return orderItem;
    }

    public Instant getPurchasedAt() {
        return purchasedAt;
    }
}
