package com.rentals.service;

import com.rentals.domain.AppUser;
import com.rentals.domain.Role;
import com.rentals.domain.Tenant;
import com.rentals.repository.AppUserRepository;
import com.rentals.security.RefreshTokenService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;

/**
 * Creates and manages user accounts.
 * Username is always normalized (trimmed + lowercase) before storing.
 */
@Service
public class UserAccountService {

    private final AppUserRepository userRepository;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public UserAccountService(AppUserRepository userRepository,
                              RefreshTokenService refreshTokenService,
                              PasswordEncoder passwordEncoder,
                              Clock clock) {
        this.userRepository = userRepository;
        this.refreshTokenService = refreshTokenService;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    /**
     * Creates a user account for an ARRENDATARIO tenant.
     * Username is normalized to lowercase and trimmed.
     *
     * @throws UsernameTakenException if the username is already in use
     */
    @Transactional
    public AppUser createTenantUser(Tenant tenant, String rawUsername, String rawPassword) {
        String username = rawUsername.trim().toLowerCase();

        if (userRepository.findByUsername(username).isPresent()) {
            throw new UsernameTakenException(username);
        }

        String hash = passwordEncoder.encode(rawPassword);
        AppUser user = new AppUser(username, hash, Role.ARRENDATARIO, tenant);
        return userRepository.save(user);
    }

    /**
     * Creates an ADMIN user.
     *
     * @throws UsernameTakenException if the username is already in use
     */
    @Transactional
    public AppUser createAdmin(String rawUsername, String rawPassword) {
        String username = rawUsername.trim().toLowerCase();

        if (userRepository.findByUsername(username).isPresent()) {
            throw new UsernameTakenException(username);
        }

        String hash = passwordEncoder.encode(rawPassword);
        AppUser user = new AppUser(username, hash, Role.ADMIN, null);
        return userRepository.save(user);
    }

    /**
     * Admin resets a tenant's password and revokes all their refresh tokens.
     */
    @Transactional
    public void resetPassword(Long userId, String newRawPassword) {
        AppUser user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        user.setPasswordHash(passwordEncoder.encode(newRawPassword));
        user.setPasswordChangedAt(OffsetDateTime.now(clock));
        refreshTokenService.revokeAllForUser(user);
    }
}
