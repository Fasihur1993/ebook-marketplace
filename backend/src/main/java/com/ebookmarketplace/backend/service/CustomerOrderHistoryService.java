package com.ebookmarketplace.backend.service;

import com.ebookmarketplace.backend.dto.CustomerOrderHistoryResponse;
import com.ebookmarketplace.backend.dto.CustomerPaymentAttemptResponse;
import com.ebookmarketplace.backend.dto.OrderItemResponse;
import com.ebookmarketplace.backend.entity.CustomerOrder;
import com.ebookmarketplace.backend.entity.OrderItem;
import com.ebookmarketplace.backend.entity.Payment;
import com.ebookmarketplace.backend.entity.User;
import com.ebookmarketplace.backend.repository.CustomerOrderRepository;
import com.ebookmarketplace.backend.repository.PaymentRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CustomerOrderHistoryService {

    private final CustomerOrderRepository customerOrderRepository;
    private final PaymentRepository paymentRepository;
    private final CurrentUserService currentUserService;

    public CustomerOrderHistoryService(
            CustomerOrderRepository customerOrderRepository,
            PaymentRepository paymentRepository,
            CurrentUserService currentUserService) {
        this.customerOrderRepository = customerOrderRepository;
        this.paymentRepository = paymentRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public Page<CustomerOrderHistoryResponse> getOrderHistory(
            Jwt jwt,
            int page,
            int size) {
        if (page < 0) {
            throw new IllegalArgumentException(
                    "Page must be greater than or equal to zero");
        }

        if (size < 1 || size > 100) {
            throw new IllegalArgumentException(
                    "Page size must be between 1 and 100");
        }

        User user = currentUserService.getRequiredUser(jwt);

        Pageable pageable = PageRequest.of(page, size);

        Page<CustomerOrder> orders = customerOrderRepository.findAllByUserIdOrderByCreatedAtDesc(
                user.getId(),
                pageable);

        return orders.map(this::mapToResponse);
    }

    private CustomerOrderHistoryResponse mapToResponse(
            CustomerOrder order) {
        List<OrderItemResponse> items = order.getItems()
                .stream()
                .map(this::mapOrderItem)
                .toList();

        CustomerPaymentAttemptResponse latestPayment = paymentRepository
                .findTopByOrderIdOrderByCreatedAtDesc(order.getId())
                .map(this::mapPayment)
                .orElse(null);

        return new CustomerOrderHistoryResponse(
                order.getId(),
                order.getTotalAmount(),
                order.getCurrency(),
                order.getStatus().name(),
                items,
                latestPayment,
                order.getCreatedAt(),
                order.getPaidAt());
    }

    private OrderItemResponse mapOrderItem(OrderItem item) {
        Long authorId = item.getAuthor() == null
                ? null
                : item.getAuthor().getId();

        return new OrderItemResponse(
                item.getEbook().getId(),
                item.getEbookTitle(),
                authorId,
                item.getUnitPrice(),
                item.getCurrency(),
                item.getQuantity());
    }

    private CustomerPaymentAttemptResponse mapPayment(Payment payment) {
        return new CustomerPaymentAttemptResponse(
                payment.getId(),
                payment.getPaymentMethod().name(),
                payment.getStatus().name(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getTransactionReference(),
                payment.getRejectionReason(),
                payment.getCreatedAt(),
                payment.getSubmittedAt(),
                payment.getVerifiedAt());
    }
}