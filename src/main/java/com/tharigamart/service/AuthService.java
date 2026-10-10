package com.tharigamart.service;

import com.tharigamart.config.JwtUtil;
import com.tharigamart.dto.ApiResponse;
import com.tharigamart.dto.AuthResponse;
import com.tharigamart.dto.LoginRequest;
import com.tharigamart.dto.RegisterRequest;
import com.tharigamart.dto.ResetPasswordRequest;
import com.tharigamart.dto.UserDTO;
import com.tharigamart.entity.PasswordResetToken;
import com.tharigamart.entity.Role;
import com.tharigamart.entity.User;
import com.tharigamart.exception.BadRequestException;
import com.tharigamart.exception.ResourceNotFoundException;
import com.tharigamart.repository.PasswordResetTokenRepository;
import com.tharigamart.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/** Registration, login, logout support and forgot / reset password flows. */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final long resetTtlMinutes;

    public AuthService(UserRepository userRepository,
                       PasswordResetTokenRepository tokenRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil,
                       @Value("${app.reset.token-ttl-minutes:30}") long resetTtlMinutes) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.resetTtlMinutes = resetTtlMinutes;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new BadRequestException("An account with this email already exists. Please log in instead.");
        }

        Role role = parseRole(request.getRole());
        String displayName = request.getFullName().trim();

        User user = new User(displayName, email,
                passwordEncoder.encode(request.getPassword()),
                blankToNull(request.getPhone()), role);
        user.setEnabled(true);
        user = userRepository.save(user);

        log.info("New {} account registered: {}", role, email);
        String token = jwtUtil.generateToken(user);
        return new AuthResponse(token, toDto(user), "Registration successful. Welcome to THARIGA MART!");
    }

    /** Registration may ask for BUYER or SELLER; admins are never created here. */
    private Role parseRole(String requested) {
        if (requested == null) {
            return Role.ROLE_BUYER;
        }
        return switch (requested.trim().toUpperCase()) {
            case "SELLER", "ROLE_SELLER" -> Role.ROLE_SELLER;
            default -> Role.ROLE_BUYER;
        };
    }

    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("Invalid email or password"));

        if (!user.isEnabled()) {
            throw new BadRequestException("This account is disabled. Please contact support.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadRequestException("Invalid email or password");
        }

        String token = jwtUtil.generateToken(user);
        return new AuthResponse(token, toDto(user), "Welcome back, " + user.getDisplayName() + "!");
    }

    /**
     * Creates a one-time reset token. The response always looks the same whether
     * or not the email exists (prevents account enumeration).
     * Email delivery is not configured for this demo, so the reset link is
     * returned to the caller and printed to the server log.
     */
    @Transactional
    public ApiResponse<String> forgotPassword(String email) {
        String genericMessage = "If an account exists for that email, a password reset link has been generated.";
        String normalized = email == null ? "" : email.trim().toLowerCase();

        return userRepository.findByEmail(normalized)
                .<ApiResponse<String>>map(user -> {
                    tokenRepository.deleteByUserId(user.getId());

                    String rawToken = UUID.randomUUID().toString().replace("-", "")
                            + UUID.randomUUID().toString().replace("-", "").substring(0, 16);

                    PasswordResetToken resetToken = new PasswordResetToken(
                            rawToken, user, LocalDateTime.now().plusMinutes(resetTtlMinutes));
                    tokenRepository.save(resetToken);

                    String resetLink = "/reset-password.html?token=" + rawToken;
                    log.info("Password reset link for {}: {}", user.getEmail(), resetLink);
                    return ApiResponse.ok(genericMessage, resetLink);
                })
                .orElseGet(() -> ApiResponse.ok(genericMessage, null));
    }

    @Transactional
    public ApiResponse<Void> resetPassword(ResetPasswordRequest request) {
        PasswordResetToken resetToken = tokenRepository.findByToken(request.getToken().trim())
                .orElseThrow(() -> new BadRequestException("This reset link is invalid or has expired"));

        if (resetToken.isUsed() || resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("This reset link is invalid or has expired");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        resetToken.setUsed(true);
        tokenRepository.save(resetToken);

        return ApiResponse.ok("Password updated successfully. Please log in with your new password.", null);
    }

    public UserDTO toDto(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setFullName(user.getDisplayName());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());
        dto.setRole(user.getRole().name());
        dto.setCreatedAt(user.getCreatedAt());
        return dto;
    }

    public User requireUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
