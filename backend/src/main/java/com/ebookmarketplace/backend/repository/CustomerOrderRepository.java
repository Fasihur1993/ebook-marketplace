
package com.ebookmarketplace.backend.repository;

import com.ebookmarketplace.backend.entity.CustomerOrder;
import com.ebookmarketplace.backend.entity.OrderStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerOrderRepository
        extends JpaRepository<CustomerOrder, Long> {

    Optional<CustomerOrder> findByIdAndUserId(
            Long id,
            Long userId);

    Page<CustomerOrder> findAllByUserIdOrderByCreatedAtDesc(
            Long userId,
            Pageable pageable);

    Page<CustomerOrder> findAllByStatusOrderByCreatedAtDesc(
            OrderStatus status,
            Pageable pageable);
}
