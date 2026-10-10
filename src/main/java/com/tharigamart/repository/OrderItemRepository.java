package com.tharigamart.repository;

import com.tharigamart.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByProductId(Long productId);

    long countByProductId(Long productId);

    /** All order lines fulfilled by a seller. */
    List<OrderItem> findBySellerId(Long sellerId);

    long countBySellerId(Long sellerId);
}
