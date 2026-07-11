package com.chargesquare.session.config;

import com.chargesquare.session.domain.AppUser;
import com.chargesquare.session.domain.Role;
import com.chargesquare.session.repo.AppUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seeds the two demo panel users at startup with BCrypt-hashed passwords, so no
 * credentials ever live in a committed SQL file. Credentials come from config/env.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final String adminUsername;
    private final String adminPassword;
    private final String viewerUsername;
    private final String viewerPassword;

    public DataSeeder(AppUserRepository users, PasswordEncoder passwordEncoder,
                      @Value("${demo.users.admin.username}") String adminUsername,
                      @Value("${demo.users.admin.password}") String adminPassword,
                      @Value("${demo.users.viewer.username}") String viewerUsername,
                      @Value("${demo.users.viewer.password}") String viewerPassword) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
        this.viewerUsername = viewerUsername;
        this.viewerPassword = viewerPassword;
    }

    @Override
    public void run(String... args) {
        seed(adminUsername, adminPassword, Role.ADMIN);
        seed(viewerUsername, viewerPassword, Role.VIEWER);
    }

    private void seed(String username, String rawPassword, Role role) {
        if (users.existsByUsername(username)) {
            return;
        }
        users.save(new AppUser(username, passwordEncoder.encode(rawPassword), role));
        log.info("seeded demo user '{}' with role {}", username, role);
    }
}
