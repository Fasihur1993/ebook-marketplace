
package com.ebookmarketplace.backend.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private CustomerOrder order;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 30)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency = "INR";

    @Column(name = "transaction_reference", length = 150)
    private String transactionReference;

    @Column(name = "screenshot_storage_path", length = 500)
    private String screenshotStoragePath;

    @Column(name = "provider_payment_id", length = 200)
    private String providerPaymentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verified_by")
    private User verifiedBy;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "rejection_reason", length = 1000)
    private String rejectionReason;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Payment() {
    }

    public Payment(
            CustomerOrder order,
            PaymentMethod paymentMethod,
            BigDecimal amount,
            String currency) {
        this.order = order;
        this.paymentMethod = paymentMethod;
        this.amount = amount;
        this.currency = currency;
    }

    public void submitUpiPayment(
            String transactionReference,
            String screenshotStoragePath) {
        if (paymentMethod != PaymentMethod.UPI_MANUAL) {
            throw new IllegalStateException(
                    "This payment does not support manual UPI submission");
        }

        if (status != PaymentStatus.PENDING &&
                status != PaymentStatus.REJECTED) {
            throw new IllegalStateException(
                    "Payment cannot be submitted in its current state");
        }

        if (transactionReference == null ||
                transactionReference.isBlank()) {
            throw new IllegalArgumentException(
                    "UPI transaction reference is required");
        }

        if (screenshotStoragePath == null ||
                screenshotStoragePath.isBlank()) {
            throw new IllegalArgumentException(
                    "Payment screenshot is required");
        }

        this.transactionReference = transactionReference.trim();
        this.screenshotStoragePath = screenshotStoragePath;
        this.status = PaymentStatus.SUBMITTED;
        this.submittedAt = Instant.now();
        this.rejectionReason = null;
        this.verifiedBy = null;
        this.verifiedAt = null;
    }

    public void verify(User admin) {
        if (status != PaymentStatus.SUBMITTED) {
            throw new IllegalStateException(
                    "Only submitted payments can be verified");
        }

        this.status = PaymentStatus.VERIFIED;
        this.verifiedBy = admin;
        this.verifiedAt = Instant.now();
        this.rejectionReason = null;
    }

    public void reject(User admin, String reason) {
        if (status != PaymentStatus.SUBMITTED) {
            throw new IllegalStateException(
                    "Only submitted payments can be rejected");
        }

        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException(
                    "Rejection reason is required");
        }

        this.status = PaymentStatus.REJECTED;
        this.verifiedBy = admin;
        this.verifiedAt = Instant.now();
        this.rejectionReason = reason.trim();
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public CustomerOrder getOrder() {
        return order;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getTransactionReference() {
        return transactionReference;
    }

    public String getScreenshotStoragePath() {
        return screenshotStoragePath;
    }

    public String getProviderPaymentId() {
        return providerPaymentId;
    }

    public User getVerifiedBy() {
        return verifiedBy;
    }

    public Instant getVerifiedAt() {
        return verifiedAt;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
