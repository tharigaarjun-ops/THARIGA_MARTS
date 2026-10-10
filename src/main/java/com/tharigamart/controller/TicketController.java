package com.tharigamart.controller;

import com.tharigamart.dto.ApiResponse;
import com.tharigamart.dto.TicketDTO;
import com.tharigamart.dto.TicketRequest;
import com.tharigamart.entity.User;
import com.tharigamart.repository.UserRepository;
import com.tharigamart.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Support tickets (login required to submit). */
@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;
    private final UserRepository userRepository;

    public TicketController(TicketService ticketService, UserRepository userRepository) {
        this.ticketService = ticketService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<List<TicketDTO>> myTickets(Authentication authentication) {
        return ResponseEntity.ok(ticketService.myTickets(currentUser(authentication)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TicketDTO>> create(Authentication authentication,
                                                         @Valid @RequestBody TicketRequest request) {
        TicketDTO created = ticketService.create(currentUser(authentication), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Support ticket submitted", created));
    }

    private User currentUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new com.tharigamart.exception.ResourceNotFoundException("Account not found"));
    }
}
