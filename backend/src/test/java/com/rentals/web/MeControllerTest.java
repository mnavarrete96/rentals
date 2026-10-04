package com.rentals.web;

import com.rentals.domain.TenantStatus;
import com.rentals.repository.AppUserRepository;
import com.rentals.security.RefreshTokenService;
import com.rentals.service.TenantService;
import com.rentals.web.dto.OneTimeChargeDto;
import com.rentals.web.dto.TenantDetailDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @WebMvcTest for MeController.
 *
 * Validates role matrix: only ARRENDATARIO can access /api/me/tenant (ARCHITECTURE §15.4).
 * JWT tenantId claim is passed via the jwt() post-processor.
 *
 * Security design notes: same as TenantControllerTest — SecurityConfig is excluded by
 * WebMvcTypeExcludeFilter, so @TestConfiguration @EnableWebSecurity provides a minimal
 * SecurityFilterChain with the relevant rules for /api/me/**.
 */
@WebMvcTest(MeController.class)
class MeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TenantService tenantService;

    @MockitoBean
    private AppUserRepository userRepository;

    @MockitoBean
    private RefreshTokenService refreshTokenService;

    /**
     * Stub JwtDecoder for oauth2ResourceServer.jwt() config — never actually called
     * because jwt() post-processor injects a pre-authenticated SecurityContext.
     */
    @MockitoBean
    private JwtDecoder jwtDecoder;

    /**
     * Minimal security configuration for this test slice.
     * @EnableWebSecurity is required because SecurityConfig is excluded by WebMvcTypeExcludeFilter.
     * SecurityFilterChain here activates security enforcement for /api/me/** (ARRENDATARIO only).
     */
    @TestConfiguration
    @EnableWebSecurity
    static class TestSecurityConfig {

        @Bean
        SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
                .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/api/me/**").hasRole("ARRENDATARIO")
                    .anyRequest().denyAll()
                );
            return http.build();
        }
    }

    private TenantDetailDto myAccount;

    @BeforeEach
    void setUp() {
        myAccount = new TenantDetailDto(
                1L, "Ana García", "Depto 301", "ana@mail.com",
                "ana.garcia",
                5, LocalDate.of(2026, 7, 1), new BigDecimal("450.00"),
                List.of("2026-07"),
                List.of(new OneTimeChargeDto(10L, "Garantía", new BigDecimal("450.00"), true)),
                List.of("2026-08"),
                new BigDecimal("450.00"),
                TenantStatus.ATRASADO
        );
    }

    // -----------------------------------------------------------------------
    // GET /api/me/tenant
    // -----------------------------------------------------------------------

    @Test
    void getMyTenant_asArrendatario_returns200() throws Exception {
        when(tenantService.getMyTenant(1L)).thenReturn(myAccount);

        mockMvc.perform(get("/api/me/tenant")
                        .with(jwt()
                                .authorities(new SimpleGrantedAuthority("ROLE_ARRENDATARIO"))
                                .jwt(j -> j.tokenValue("test-token").claim("tenantId", 1L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("ana.garcia"))
                .andExpect(jsonPath("$.status").value("ATRASADO"));
    }

    @Test
    void getMyTenant_asAdmin_returns403() throws Exception {
        mockMvc.perform(get("/api/me/tenant")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void getMyTenant_anonymous_returns401() throws Exception {
        mockMvc.perform(get("/api/me/tenant"))
                .andExpect(status().isUnauthorized());
    }
}
