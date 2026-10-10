package com.tharigamart.service;

import com.tharigamart.dto.ReviewDTO;
import com.tharigamart.dto.ReviewRequest;
import com.tharigamart.entity.Product;
import com.tharigamart.entity.Review;
import com.tharigamart.entity.ReviewStatus;
import com.tharigamart.entity.User;
import com.tharigamart.exception.BadRequestException;
import com.tharigamart.exception.ResourceNotFoundException;
import com.tharigamart.repository.OrderItemRepository;
import com.tharigamart.repository.ProductRepository;
import com.tharigamart.repository.ReviewRepository;
import com.tharigamart.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Product reviews: public listing + post a review. */
@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OrderItemRepository orderItemRepository;

    public ReviewService(ReviewRepository reviewRepository,
                           ProductRepository productRepository,
                           UserRepository userRepository,
                           OrderItemRepository orderItemRepository) {
        this.reviewRepository = reviewRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.orderItemRepository = orderItemRepository;
    }

    /** Approved reviews for a product, newest first. */
    @Transactional(readOnly = true)
    public List<ReviewDTO> listApproved(Long productId) {
        return reviewRepository
                .findByProductIdAndStatusOrderByIdDesc(productId, ReviewStatus.APPROVED)
                .stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * Posts a review. Verified-purchase badge is set when the
     * signed-in user has actually bought the product.
     */
    @Transactional
    public ReviewDTO create(User currentUser, Long productId, ReviewRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> ResourceNotFoundException.of("Product", productId));

        if (reviewRepository.existsByProductIdAndUserId(productId, currentUser.getId())) {
            throw new BadRequestException("You have already reviewed this product");
        }

        boolean purchased = orderItemRepository.findByProductId(productId).stream()
                .anyMatch(line -> line.getOrder() != null
                        && line.getOrder().getBuyer() != null
                        && line.getOrder().getBuyer().getId().equals(currentUser.getId())
                        && line.getOrder().getStatus() != null
                        && line.getOrder().getStatus().name().equals("DELIVERED"));

        Review review = new Review(product, currentUser,
                request.getRating() == null ? 5 : request.getRating(),
                request.getTitle() == null || request.getTitle().isBlank()
                        ? product.getTitle() : request.getTitle().trim(),
                request.getComment().trim());
        review.setVerifiedPurchase(purchased);
        review.setStatus(ReviewStatus.APPROVED);

        // Refresh the product's cached rating / review count
        List<Review> all = reviewRepository.findByProductId(productId);
        double average = all.stream().mapToInt(Review::getRating).average().orElse(0);
        product.setRating(Math.round(average * 10.0) / 10.0);
        product.setReviewCount(all.size());
        productRepository.save(product);

        return toDto(reviewRepository.save(review));
    }

    public ReviewDTO toDto(Review review) {
        ReviewDTO dto = new ReviewDTO();
        dto.setId(review.getId());
        dto.setProductId(review.getProduct() == null ? null : review.getProduct().getId());
        dto.setUserId(review.getUser() == null ? null : review.getUser().getId());
        dto.setUserName(review.getUser() == null ? null : review.getUser().getDisplayName());
        dto.setRating(review.getRating());
        dto.setTitle(review.getTitle());
        dto.setComment(review.getComment());
        dto.setHelpfulVotes(review.getHelpfulVotes());
        dto.setVerifiedPurchase(review.isVerifiedPurchase());
        dto.setStatus(review.getStatus() == null ? null : review.getStatus().name());
        dto.setCreatedAt(review.getCreatedAt());
        return dto;
    }
}
