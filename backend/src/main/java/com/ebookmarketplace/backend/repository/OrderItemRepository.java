
package com.ebookmarketplace.backend.repository;

import com.ebookmarketplace.backend.entity.OrderItem;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderItemRepository
        extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findAllByOrderId(Long orderId);

    boolean existsByOrderIdAndEbookId(
            Long orderId,
            Long ebookId);
}
