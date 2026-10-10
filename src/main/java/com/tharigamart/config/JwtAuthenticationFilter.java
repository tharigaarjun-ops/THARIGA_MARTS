package com.tharigamart.config;

import com.tharigamart.entity.Role;
import com.tharigamart.entity.User;
import com.tharigamart.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Reads "Authorization: Bearer &lt;token&gt;" on every request.
 * The signature and expiry are validated from the JWT; the ROLE authority is
 * resolved against the database so that role changes and account deletions take
 * effect immediately (a deleted user's token no longer authenticates).
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, UserRepository userRepository) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7).trim();
            if (!token.isEmpty() && jwtUtil.isValid(token)
                    && SecurityContextHolder.getContext().getAuthentication() == null) {
                String email = jwtUtil.extractEmail(token);
                if (email != null) {
                    User user = userRepository.findByEmail(email).orElse(null);
                    if (user != null && user.isEnabled()) {
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                        email, null, List.of(new SimpleGrantedAuthority(roleAuthority(user.getRole()))));
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                    // unknown/deleted/disabled account -> stays unauthenticated (401)
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Roles are stored with the ROLE_ prefix in the database
     * (ROLE_BUYER, ROLE_SELLER, ROLE_ADMIN). Spring Security expects
     * authorities in exactly that form, so only add the prefix when missing.
     */
    static String roleAuthority(Role role) {
        if (role == null) {
            return "ROLE_BUYER";
        }
        String name = role.name();
        return name.startsWith("ROLE_") ? name : "ROLE_" + name;
    }
}
