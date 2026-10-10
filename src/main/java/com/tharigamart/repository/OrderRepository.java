package com.tharigamart.repository;

import com.tharigamart.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByBuyerIdOrderByPlacedAtDesc(Long buyerId);

    Optional<Order> findByBuyerIdAndId(Long buyerId, Long id);

    Optional<Order> findByOrderNumber(String orderNumber);

    long countByBuyerId(Long buyerId);

    List<Order> findByOrderByPlacedAtDesc();
}
