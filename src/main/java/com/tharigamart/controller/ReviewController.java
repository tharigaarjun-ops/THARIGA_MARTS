package com.tharigamart.controller;

import com.tharigamart.dto.ApiResponse;
import com.tharigamart.dto.ReviewDTO;
import com.tharigamart.dto.ReviewRequest;
import com.tharigamart.entity.User;
import com.tharigamart.repository.UserRepository;
import com.tharigamart.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Product reviews (public read, authenticated write). */
@RestController
@RequestMapping("/api/products")
public class ReviewController {

    private final ReviewService reviewService;
    private final UserRepository userRepository;

    public ReviewController(ReviewService reviewService, UserRepository userRepository) {
        this.reviewService = reviewService;
        this.userRepository = userRepository;
    }

    @GetMapping("/{id}/reviews")
    public ResponseEntity<List<ReviewDTO>> list(@PathVariable Long id) {
        return ResponseEntity.ok(reviewService.listApproved(id));
    }

    @PostMapping("/{id}/reviews")
    public ResponseEntity<ApiResponse<ReviewDTO>> create(Authentication authentication,
                                                           @PathVariable Long id,
                                                           @Valid @RequestBody ReviewRequest request) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new com.tharigamart.exception.ResourceNotFoundException("Account not found"));
        ReviewDTO created = reviewService.create(user, id, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Review posted", created));
    }
}
