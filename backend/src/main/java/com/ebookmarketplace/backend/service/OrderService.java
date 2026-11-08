package com.ebookmarketplace.backend.service;

import com.ebookmarketplace.backend.dto.CreateOrderRequest;
import com.ebookmarketplace.backend.dto.OrderItemResponse;
import com.ebookmarketplace.backend.dto.OrderResponse;
import com.ebookmarketplace.backend.dto.PaymentResponse;
import com.ebookmarketplace.backend.entity.CustomerOrder;
import com.ebookmarketplace.backend.entity.Ebook;
import com.ebookmarketplace.backend.entity.EbookStatus;
import com.ebookmarketplace.backend.entity.OrderItem;
import com.ebookmarketplace.backend.entity.Payment;
import com.ebookmarketplace.backend.entity.PaymentMethod;
import com.ebookmarketplace.backend.entity.User;
import com.ebookmarketplace.backend.repository.CustomerOrderRepository;
import com.ebookmarketplace.backend.repository.EbookRepository;
import com.ebookmarketplace.backend.repository.PaymentRepository;
import com.ebookmarketplace.backend.repository.PurchaseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.oauth2.jwt.Jwt;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final CurrentUserService currentUserService;
    private final EbookRepository ebookRepository;
    private final CustomerOrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final PurchaseRepository purchaseRepository;

    public OrderService(
            CurrentUserService currentUserService,
            EbookRepository ebookRepository,
            CustomerOrderRepository orderRepository,
            PaymentRepository paymentRepository,
            PurchaseRepository purchaseRepository) {
        this.currentUserService = currentUserService;
        this.ebookRepository = ebookRepository;
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.purchaseRepository = purchaseRepository;
    }

    @Transactional
    public OrderResponse createOrder(Jwt jwt, CreateOrderRequest request) {

        User user = currentUserService.getRequiredUser(jwt);

        List<Long> ebookIds = request.ebookIds();

        if (ebookIds == null || ebookIds.isEmpty()) {
            throw new IllegalArgumentException("At least one ebook is required");
        }

        if (ebookIds.size() > 20) {
            throw new IllegalArgumentException("You can purchase at most 20 ebooks at once");
        }

        Set<Long> uniqueEbookIds = new HashSet<>(ebookIds);

        if (uniqueEbookIds.size() != ebookIds.size()) {
            throw new IllegalArgumentException("Duplicate ebooks are not allowed in an order");
        }

        List<Ebook> ebooks = ebookRepository.findAllById(ebookIds);

        if (ebooks.size() != ebookIds.size()) {
            throw new IllegalArgumentException("One or more ebooks could not be found");
        }

        Map<Long, Ebook> ebooksById = ebooks.stream()
                .collect(Collectors.toMap(Ebook::getId, Function.identity()));

        List<Ebook> orderedEbooks = new ArrayList<>();

        for (Long ebookId : ebookIds) {

            Ebook ebook = ebooksById.get(ebookId);

            if (ebook.getStatus() != EbookStatus.PUBLISHED) {
                throw new IllegalArgumentException(
                        "Ebook is not available for purchase: " + ebook.getTitle());
            }

            if (ebook.getPdfStoragePath() == null
                    || ebook.getPdfStoragePath().isBlank()) {
                throw new IllegalArgumentException(
                        "Ebook does not have an available PDF: " + ebook.getTitle());
            }

            if (purchaseRepository.existsByUserIdAndEbookId(user.getId(), ebookId)) {
                throw new IllegalArgumentException(
                        "You have already purchased: " + ebook.getTitle());
            }

            orderedEbooks.add(ebook);
        }

        String currency = orderedEbooks.get(0).getCurrency();

        BigDecimal totalAmount = BigDecimal.ZERO;

        for (Ebook ebook : orderedEbooks) {

            if (!currency.equals(ebook.getCurrency())) {
                throw new IllegalArgumentException(
                        "All ebooks in an order must use the same currency");
            }

            if (ebook.getPrice() == null || ebook.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException(
                        "Invalid ebook price: " + ebook.getTitle());
            }

            totalAmount = totalAmount.add(ebook.getPrice());
        }

        CustomerOrder order = new CustomerOrder(
                user,
                totalAmount,
                currency);

        for (Ebook ebook : orderedEbooks) {
            OrderItem item = new OrderItem(
                    ebook,
                    ebook.getPrice(),
                    currency);

            order.addItem(item);
        }

        CustomerOrder savedOrder = orderRepository.save(order);

        Payment payment = new Payment(
                savedOrder,
                PaymentMethod.UPI_MANUAL,
                totalAmount,
                currency);

        Payment savedPayment = paymentRepository.save(payment);

        List<OrderItemResponse> itemResponses = savedOrder.getItems()
                .stream()
                .map(item -> new OrderItemResponse(
                        item.getEbook().getId(),
                        item.getEbookTitle(),
                        item.getAuthor() != null ? item.getAuthor().getId() : null,
                        item.getUnitPrice(),
                        item.getCurrency(),
                        item.getQuantity()))
                .toList();

        PaymentResponse paymentResponse = new PaymentResponse(
                savedPayment.getId(),
                savedPayment.getPaymentMethod().name(),
                savedPayment.getStatus().name(),
                savedPayment.getAmount(),
                savedPayment.getCurrency(),
                savedPayment.getTransactionReference(),
                savedPayment.getSubmittedAt());

        return new OrderResponse(
                savedOrder.getId(),
                savedOrder.getTotalAmount(),
                savedOrder.getCurrency(),
                savedOrder.getStatus().name(),
                itemResponses,
                paymentResponse,
                savedOrder.getCreatedAt());
    }
}