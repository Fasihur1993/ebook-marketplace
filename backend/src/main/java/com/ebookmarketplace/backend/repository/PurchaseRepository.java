
package com.ebookmarketplace.backend.repository;

import com.ebookmarketplace.backend.entity.Purchase;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PurchaseRepository
        extends JpaRepository<Purchase, Long> {

    boolean existsByUserIdAndEbookId(
            Long userId,
            Long ebookId);

    Optional<Purchase> findByUserIdAndEbookId(
            Long userId,
            Long ebookId);

    Page<Purchase> findAllByUserIdOrderByPurchasedAtDesc(
            Long userId,
            Pageable pageable);
}
