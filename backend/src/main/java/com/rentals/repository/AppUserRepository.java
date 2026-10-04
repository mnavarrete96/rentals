package com.rentals.repository;

import com.rentals.domain.AppUser;
import com.rentals.domain.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByUsername(String username);

    Optional<AppUser> findByTenantId(Long tenantId);

    boolean existsByRole(Role role);
}
