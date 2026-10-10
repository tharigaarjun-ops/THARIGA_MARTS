package com.tharigamart.repository;

import com.tharigamart.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    /** Public reviews: approved only, newest first. */
    List<Review> findByProductIdAndStatusOrderByIdDesc(Long productId,
                                                        com.tharigamart.entity.ReviewStatus status);

    List<Review> findByProductId(Long productId);

    long countByProductId(Long productId);

    boolean existsByProductIdAndUserId(Long productId, Long userId);
}
