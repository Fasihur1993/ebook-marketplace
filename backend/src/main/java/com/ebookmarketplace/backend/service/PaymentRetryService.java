
package com.ebookmarketplace.backend.service;

import com.ebookmarketplace.backend.dto.PaymentResponse;
import com.ebookmarketplace.backend.entity.CustomerOrder;
import com.ebookmarketplace.backend.entity.OrderStatus;
import com.ebookmarketplace.backend.entity.Payment;
import com.ebookmarketplace.backend.entity.PaymentMethod;
import com.ebookmarketplace.backend.entity.PaymentStatus;
import com.ebookmarketplace.backend.repository.CustomerOrderRepository;
import com.ebookmarketplace.backend.repository.PaymentRepository;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentRetryService {

    private final CustomerOrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final CurrentUserService currentUserService;

    public PaymentRetryService(
            CustomerOrderRepository orderRepository,
            PaymentRepository paymentRepository,
            CurrentUserService currentUserService) {
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public PaymentResponse retryPayment(Long orderId, Jwt jwt) {

        var user = currentUserService.getRequiredUser(jwt);

        CustomerOrder order = orderRepository
                .findByIdAndUserId(orderId, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new IllegalStateException(
                    "Only orders pending payment can be retried");
        }

        Payment latestPayment = paymentRepository
                .findTopByOrderIdOrderByCreatedAtDesc(orderId)
                .orElseThrow(() -> new IllegalStateException("Payment attempt not found"));

        if (latestPayment.getStatus() != PaymentStatus.REJECTED) {
            throw new IllegalStateException(
                    "Payment can only be retried after the previous attempt is rejected");
        }

        Payment newPayment = new Payment(
                order,
                PaymentMethod.UPI_MANUAL,
                order.getTotalAmount(),
                order.getCurrency());

        Payment savedPayment = paymentRepository.saveAndFlush(newPayment);

        return new PaymentResponse(
                savedPayment.getId(),
                savedPayment.getPaymentMethod().name(),
                savedPayment.getStatus().name(),
                savedPayment.getAmount(),
                savedPayment.getCurrency(),
                savedPayment.getTransactionReference(),
                savedPayment.getSubmittedAt());
    }
}
