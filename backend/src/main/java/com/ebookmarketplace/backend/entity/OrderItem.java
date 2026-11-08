
package com.ebookmarketplace.backend.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "order_items", uniqueConstraints = @UniqueConstraint(name = "uk_order_items_order_ebook", columnNames = {
        "order_id", "ebook_id" }))
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private CustomerOrder order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ebook_id", nullable = false)
    private Ebook ebook;

    @Column(name = "ebook_title", nullable = false, length = 200)
    private String ebookTitle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private User author;

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false, length = 3)
    private String currency = "INR";

    @Column(nullable = false)
    private Integer quantity = 1;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected OrderItem() {
    }

    public OrderItem(
            Ebook ebook,
            BigDecimal unitPrice,
            String currency) {
        this.ebook = ebook;
        this.ebookTitle = ebook.getTitle();
        this.author = ebook.getAuthor();
        this.unitPrice = unitPrice;
        this.currency = currency;
        this.quantity = 1;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

    void setOrder(CustomerOrder order) {
        this.order = order;
    }

    public Long getId() {
        return id;
    }

    public CustomerOrder getOrder() {
        return order;
    }

    public Ebook getEbook() {
        return ebook;
    }

    public String getEbookTitle() {
        return ebookTitle;
    }

    public User getAuthor() {
        return author;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public String getCurrency() {
        return currency;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
