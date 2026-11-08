package com.ebookmarketplace.backend.repository;

import com.ebookmarketplace.backend.entity.Payment;
import com.ebookmarketplace.backend.entity.PaymentMethod;
import com.ebookmarketplace.backend.entity.PaymentStatus;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PaymentRepository
                extends JpaRepository<Payment, Long> {

        boolean existsByTransactionReferenceIgnoreCase(
                        String transactionReference);

        Optional<Payment> findByIdAndOrderUserId(
                        Long id,
                        Long userId);

        Page<Payment> findAllByStatusOrderBySubmittedAtAsc(
                        PaymentStatus status,
                        Pageable pageable);

        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("""
                        SELECT p
                        FROM Payment p
                        WHERE p.id = :paymentId
                        """)
        Optional<Payment> findByIdForUpdate(
                        @Param("paymentId") Long paymentId);

        Optional<Payment> findTopByOrderIdOrderByCreatedAtDesc(
                        Long orderId);

        Page<Payment> findAllByPaymentMethodAndStatusOrderBySubmittedAtAsc(
                        PaymentMethod paymentMethod,
                        PaymentStatus status,
                        Pageable pageable);
}