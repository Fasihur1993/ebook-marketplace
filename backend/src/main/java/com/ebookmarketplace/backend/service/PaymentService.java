package com.ebookmarketplace.backend.service;

import com.ebookmarketplace.backend.dto.AdminPaymentActionResponse;
import com.ebookmarketplace.backend.dto.AdminPaymentItemResponse;
import com.ebookmarketplace.backend.dto.AdminPaymentResponse;
import com.ebookmarketplace.backend.dto.PaymentResponse;
import com.ebookmarketplace.backend.entity.CustomerOrder;
import com.ebookmarketplace.backend.entity.OrderItem;
import com.ebookmarketplace.backend.entity.OrderStatus;
import com.ebookmarketplace.backend.entity.Payment;
import com.ebookmarketplace.backend.entity.PaymentMethod;
import com.ebookmarketplace.backend.entity.PaymentStatus;
import com.ebookmarketplace.backend.entity.Purchase;
import com.ebookmarketplace.backend.repository.CustomerOrderRepository;
import com.ebookmarketplace.backend.repository.PaymentRepository;
import com.ebookmarketplace.backend.repository.PurchaseRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;

@Service
public class PaymentService {

    private final CurrentUserService currentUserService;
    private final CustomerOrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final PurchaseRepository purchaseRepository;
    private final PaymentScreenshotStorageService screenshotStorageService;

    public PaymentService(
            CurrentUserService currentUserService,
            CustomerOrderRepository orderRepository,
            PaymentRepository paymentRepository,
            PurchaseRepository purchaseRepository,
            PaymentScreenshotStorageService screenshotStorageService) {

        this.currentUserService = currentUserService;
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.purchaseRepository = purchaseRepository;
        this.screenshotStorageService = screenshotStorageService;
    }

    // ---------------------------------------------------------
    // Customer: Submit UPI payment
    // ---------------------------------------------------------

    @Transactional
    public PaymentResponse submitUpiPayment(
            Jwt jwt,
            Long orderId,
            Long paymentId,
            String transactionReference,
            MultipartFile screenshot) {

        var user = currentUserService.getRequiredUser(jwt);

        if (transactionReference == null || transactionReference.isBlank()) {
            throw new IllegalArgumentException(
                    "UPI transaction ID / UTR is required");
        }

        transactionReference = transactionReference.trim();

        if (transactionReference.length() > 150) {
            throw new IllegalArgumentException(
                    "UPI transaction ID is too long");
        }

        CustomerOrder order = orderRepository
                .findByIdAndUserId(orderId, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        Payment payment = paymentRepository
                .findByIdAndOrderUserId(paymentId, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Payment not found"));

        if (!payment.getOrder().getId().equals(order.getId())) {
            throw new IllegalArgumentException(
                    "Payment does not belong to this order");
        }

        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new IllegalStateException(
                    "This order is not awaiting payment");
        }

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new IllegalStateException(
                    "This payment attempt has already been processed");
        }

        if (paymentRepository.existsByTransactionReferenceIgnoreCase(
                transactionReference)) {
            throw new IllegalArgumentException(
                    "This UPI transaction ID has already been submitted");
        }

        String screenshotPath = screenshotStorageService.store(screenshot);

        registerRollbackCleanup(screenshotPath);

        try {
            payment.submitUpiPayment(
                    transactionReference,
                    screenshotPath);

            order.markPaymentSubmitted();

            Payment savedPayment = paymentRepository.saveAndFlush(payment);

            return new PaymentResponse(
                    savedPayment.getId(),
                    savedPayment.getPaymentMethod().name(),
                    savedPayment.getStatus().name(),
                    savedPayment.getAmount(),
                    savedPayment.getCurrency(),
                    savedPayment.getTransactionReference(),
                    savedPayment.getSubmittedAt());

        } catch (DataIntegrityViolationException exception) {
            throw new IllegalArgumentException(
                    "This UPI transaction ID has already been submitted",
                    exception);
        }
    }

    // ---------------------------------------------------------
    // Admin: List submitted payments
    // ---------------------------------------------------------

    @Transactional(readOnly = true)
    public Page<AdminPaymentResponse> getSubmittedPayments(
            int page,
            int size) {

        if (page < 0) {
            throw new IllegalArgumentException(
                    "Page number cannot be negative");
        }

        if (size < 1 || size > 100) {
            throw new IllegalArgumentException(
                    "Page size must be between 1 and 100");
        }

        return paymentRepository
                .findAllByStatusOrderBySubmittedAtAsc(
                        PaymentStatus.SUBMITTED,
                        PageRequest.of(page, size))
                .map(this::toAdminPaymentResponse);
    }

    private AdminPaymentResponse toAdminPaymentResponse(Payment payment) {

        CustomerOrder order = payment.getOrder();

        var customer = order.getUser();

        var items = order.getItems()
                .stream()
                .map(this::toAdminPaymentItemResponse)
                .toList();

        return new AdminPaymentResponse(
                payment.getId(),
                order.getId(),
                customer.getFullName(),
                customer.getEmail(),
                payment.getPaymentMethod().name(),
                payment.getStatus().name(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getTransactionReference(),
                payment.getSubmittedAt(),
                payment.getScreenshotStoragePath() != null,
                items);
    }

    private AdminPaymentItemResponse toAdminPaymentItemResponse(
            OrderItem item) {

        return new AdminPaymentItemResponse(
                item.getEbook().getId(),
                item.getEbookTitle(),
                item.getAuthor() != null
                        ? item.getAuthor().getId()
                        : null,
                item.getUnitPrice(),
                item.getCurrency(),
                item.getQuantity());
    }

    // ---------------------------------------------------------
    // Admin: Verify payment
    // ---------------------------------------------------------

    @Transactional
    public AdminPaymentActionResponse verifyPayment(
            Jwt jwt,
            Long paymentId) {

        var admin = currentUserService.getRequiredUser(jwt);

        Payment payment = paymentRepository
                .findByIdForUpdate(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found"));

        validateAdminUpiPayment(payment);

        CustomerOrder order = payment.getOrder();

        if (order.getStatus() != OrderStatus.PAYMENT_SUBMITTED) {
            throw new IllegalStateException(
                    "Order is not awaiting payment verification");
        }

        // Change payment status.
        payment.verify(admin);

        // Mark the order as paid.
        order.markPaid();

        // Grant library access.
        for (OrderItem item : order.getItems()) {

            boolean alreadyPurchased = purchaseRepository.existsByUserIdAndEbookId(
                    order.getUser().getId(),
                    item.getEbook().getId());

            if (!alreadyPurchased) {
                Purchase purchase = new Purchase(
                        order.getUser(),
                        item.getEbook(),
                        item);

                purchaseRepository.save(purchase);
            }
        }

        return new AdminPaymentActionResponse(
                payment.getId(),
                payment.getStatus().name(),
                order.getId(),
                order.getStatus().name(),
                payment.getVerifiedAt(),
                payment.getRejectionReason());
    }

    // ---------------------------------------------------------
    // Admin: Reject payment
    // ---------------------------------------------------------

    @Transactional
    public AdminPaymentActionResponse rejectPayment(
            Jwt jwt,
            Long paymentId,
            String reason) {

        var admin = currentUserService.getRequiredUser(jwt);

        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException(
                    "Payment rejection reason is required");
        }

        if (reason.trim().length() > 1000) {
            throw new IllegalArgumentException(
                    "Payment rejection reason cannot exceed 1000 characters");
        }

        Payment payment = paymentRepository
                .findByIdForUpdate(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found"));

        validateAdminUpiPayment(payment);

        CustomerOrder order = payment.getOrder();

        if (order.getStatus() != OrderStatus.PAYMENT_SUBMITTED) {
            throw new IllegalStateException(
                    "Order is not awaiting payment verification");
        }

        payment.reject(admin, reason);

        // Allow the customer to retry payment.
        order.markPaymentRejected();

        return new AdminPaymentActionResponse(
                payment.getId(),
                payment.getStatus().name(),
                order.getId(),
                order.getStatus().name(),
                payment.getVerifiedAt(),
                payment.getRejectionReason());
    }

    // ---------------------------------------------------------
    // Admin: Retrieve private screenshot
    // ---------------------------------------------------------

    @Transactional(readOnly = true)
    public Path getPaymentScreenshot(Long paymentId) {

        Payment payment = paymentRepository
                .findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found"));

        validateAdminUpiPayment(payment);

        return screenshotStorageService.resolvePath(
                payment.getScreenshotStoragePath());
    }

    private void validateAdminUpiPayment(Payment payment) {

        if (payment.getPaymentMethod() != PaymentMethod.UPI_MANUAL) {
            throw new IllegalStateException(
                    "This operation is only supported for manual UPI payments");
        }
    }

    // ---------------------------------------------------------
    // Screenshot cleanup if customer submission rolls back
    // ---------------------------------------------------------

    private void registerRollbackCleanup(String screenshotPath) {

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {

                    @Override
                    public void afterCompletion(int status) {

                        if (status == STATUS_ROLLED_BACK) {
                            screenshotStorageService.delete(screenshotPath);
                        }
                    }
                });
    }
}