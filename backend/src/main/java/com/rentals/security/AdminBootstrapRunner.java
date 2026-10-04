package com.rentals.security;

import com.rentals.domain.AppUser;
import com.rentals.domain.Role;
import com.rentals.repository.AppUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates the initial ADMIN user from environment variables on startup,
 * if no ADMIN exists yet. Idempotent: never overwrites an existing admin.
 *
 * ARCHITECTURE §15.6: variables RENTALS_BOOTSTRAP_ADMIN_USERNAME / PASSWORD.
 * Both must be set for the bootstrap to run.
 */
@Component
public class AdminBootstrapRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapRunner.class);

    private final AppUserRepository userRepository;
    private final AuthProperties authProperties;
    private final PasswordEncoder passwordEncoder;

    public AdminBootstrapRunner(AppUserRepository userRepository,
                                AuthProperties authProperties,
                                PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.authProperties = authProperties;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        AuthProperties.BootstrapAdmin bootstrap = authProperties.bootstrapAdmin();

        if (bootstrap == null) return;

        String username = bootstrap.username();
        String password = bootstrap.password();

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            log.info("AdminBootstrapRunner: RENTALS_BOOTSTRAP_ADMIN_USERNAME/PASSWORD not set, skipping");
            return;
        }

        // Already have an admin
        if (userRepository.existsByRole(Role.ADMIN)) {
            log.info("AdminBootstrapRunner: ADMIN user already exists, skipping bootstrap");
            return;
        }

        // Password policy: min 10 chars
        if (password.length() < 10) {
            throw new IllegalStateException(
                "RENTALS_BOOTSTRAP_ADMIN_PASSWORD must be at least 10 characters");
        }

        String normalizedUsername = username.trim().toLowerCase();
        String hash = passwordEncoder.encode(password);

        AppUser admin = new AppUser(normalizedUsername, hash, Role.ADMIN, null);
        userRepository.save(admin);

        log.warn("AdminBootstrapRunner: Initial ADMIN created: {}", normalizedUsername);
    }
}
