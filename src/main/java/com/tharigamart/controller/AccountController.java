package com.tharigamart.controller;

import com.tharigamart.dto.AddressDTO;
import com.tharigamart.dto.AddressRequest;
import com.tharigamart.dto.ApiResponse;
import com.tharigamart.dto.ChangePasswordRequest;
import com.tharigamart.dto.UpdateProfileRequest;
import com.tharigamart.dto.UserDTO;
import com.tharigamart.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** My Account endpoints (login required): profile, password, address book. */
@RestController
@RequestMapping("/api/account")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/profile")
    public ResponseEntity<UserDTO> profile(Authentication authentication) {
        return ResponseEntity.ok(accountService.getProfile(authentication.getName()));
    }

    @PutMapping("/profile")
    public ResponseEntity<UserDTO> updateProfile(Authentication authentication,
                                                   @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(accountService.updateProfile(authentication.getName(), request));
    }

    @PutMapping("/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(Authentication authentication,
                                                              @Valid @RequestBody ChangePasswordRequest request) {
        return ResponseEntity.ok(accountService.changePassword(authentication.getName(), request));
    }

    @GetMapping("/addresses")
    public ResponseEntity<List<AddressDTO>> listAddresses(Authentication authentication) {
        return ResponseEntity.ok(accountService.listAddresses(authentication.getName()));
    }

    @PostMapping("/addresses")
    public ResponseEntity<AddressDTO> addAddress(Authentication authentication,
                                                   @Valid @RequestBody AddressRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(accountService.addAddress(authentication.getName(), request));
    }

    @PutMapping("/addresses/{id}")
    public ResponseEntity<AddressDTO> updateAddress(Authentication authentication,
                                                      @PathVariable Long id,
                                                      @Valid @RequestBody AddressRequest request) {
        return ResponseEntity.ok(accountService.updateAddress(authentication.getName(), id, request));
    }

    @DeleteMapping("/addresses/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAddress(Authentication authentication,
                                                             @PathVariable Long id) {
        return ResponseEntity.ok(accountService.deleteAddress(authentication.getName(), id));
    }
}
