package com.tharigamart.repository;

import com.tharigamart.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByUserIdOrderByPlacedAtDesc(Long userId);

    Optional<Order> findByUserIdAndId(Long userId, Long id);

    Optional<Order> findByOrderNumber(String orderNumber);

    long countByUserId(Long userId);

    List<Order> findByOrderByPlacedAtDesc();
}
