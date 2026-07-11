package com.chargesquare.session.service;

import com.chargesquare.session.domain.AppUser;
import com.chargesquare.session.error.UnauthorizedException;
import com.chargesquare.session.repo.AppUserRepository;
import com.chargesquare.session.security.JwtService;
import com.chargesquare.session.web.dto.LoginResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/** Validates credentials against the BCrypt hash and issues a JWT carrying the role. */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(AppUserRepository users, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public LoginResponse login(String username, String password) {
        AppUser user = users.findByUsername(username).orElse(null);
        if (user == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            log.warn("failed login attempt for username '{}'", username);
            throw new UnauthorizedException("Invalid username or password");
        }
        String token = jwtService.issue(user.getUsername(), user.getRole().name());
        log.info("login success for '{}' ({})", user.getUsername(), user.getRole());
        return new LoginResponse(token, "Bearer", user.getUsername(),
                user.getRole().name(), jwtService.getExpirySeconds());
    }
}
