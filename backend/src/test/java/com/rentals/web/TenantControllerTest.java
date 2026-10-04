package com.rentals.web;

import com.rentals.domain.TenantStatus;
import com.rentals.repository.AppUserRepository;
import com.rentals.security.RefreshTokenService;
import com.rentals.service.TenantNotFoundException;
import com.rentals.service.TenantService;
import com.rentals.service.UsernameTakenException;
import com.rentals.web.dto.OneTimeChargeDto;
import com.rentals.web.dto.TenantDetailDto;
import com.rentals.web.dto.TenantSummaryDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @WebMvcTest for TenantController.
 *
 * Tests the role matrix (ARCHITECTURE §15.4) and endpoint behavior with a mocked TenantService.
 *
 * Security design notes for @WebMvcTest:
 *  - WebMvcTypeExcludeFilter in Spring Boot 4.x excludes @Configuration classes that are not
 *    @Controller, @ControllerAdvice, etc. This means SecurityConfig is NOT in the slice context.
 *  - Without SecurityConfig there is no @EnableWebSecurity, no HttpSecurity, and no
 *    SecurityFilterChain → all requests pass through without enforcement.
 *  - Fix: provide a standalone @TestConfiguration @EnableWebSecurity with a minimal
 *    SecurityFilterChain that replicates the rules relevant to TenantController (/api/tenants/**
 *    requires ADMIN). This activates the security filter in MockMvc.
 *  - @MockitoBean JwtDecoder satisfies oauth2ResourceServer.jwt()'s decoder requirement;
 *    the jwt() post-processor bypasses the decoder completely.
 */
@WebMvcTest(TenantController.class)
class TenantControllerTest {

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
     * @EnableWebSecurity is required here (SecurityConfig is excluded by WebMvcTypeExcludeFilter).
     * The SecurityFilterChain bean definition must be registered before
     * OAuth2ResourceServerWebSecurityAutoConfiguration evaluates @ConditionalOnDefaultWebSecurity;
     * having it here (as a @TestConfiguration inner class) ensures it is registered early enough
     * so that the autoconfiguration's condition is FALSE and no competing filter chain is created.
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
                    .requestMatchers("/api/tenants/**").hasRole("ADMIN")
                    .anyRequest().denyAll()
                );
            return http.build();
        }
    }

    private TenantSummaryDto summaryDto;
    private TenantDetailDto detailDto;

    @BeforeEach
    void setUp() {
        summaryDto = new TenantSummaryDto(
                1L, "María Pérez", "Depto 301", TenantStatus.AL_DIA,
                BigDecimal.ZERO, 0
        );
        detailDto = new TenantDetailDto(
                1L, "María Pérez", "Depto 301", "maria@mail.com",
                "maria.perez",
                5, LocalDate.of(2026, 7, 1), new BigDecimal("450.00"),
                List.of("2026-07", "2026-08"),
                List.of(new OneTimeChargeDto(10L, "Garantía", new BigDecimal("450.00"), true)),
                List.of(),
                BigDecimal.ZERO,
                TenantStatus.AL_DIA
        );
    }

    // -----------------------------------------------------------------------
    // GET /api/tenants
    // -----------------------------------------------------------------------

    @Test
    void listTenants_asAdmin_returns200() throws Exception {
        when(tenantService.listSummaries()).thenReturn(List.of(summaryDto));

        mockMvc.perform(get("/api/tenants")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].fullName").value("María Pérez"));
    }

    @Test
    void listTenants_anonymous_returns401() throws Exception {
        mockMvc.perform(get("/api/tenants"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listTenants_asArrendatario_returns403() throws Exception {
        mockMvc.perform(get("/api/tenants")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ARRENDATARIO"))))
                .andExpect(status().isForbidden());
    }

    // -----------------------------------------------------------------------
    // GET /api/tenants/{id}
    // -----------------------------------------------------------------------

    @Test
    void getTenantById_asAdmin_returns200() throws Exception {
        when(tenantService.getDetail(1L)).thenReturn(detailDto);

        mockMvc.perform(get("/api/tenants/1")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("maria.perez"));
    }

    @Test
    void getTenantById_notFound_returns404() throws Exception {
        when(tenantService.getDetail(99L)).thenThrow(new TenantNotFoundException(99L));

        mockMvc.perform(get("/api/tenants/99")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isNotFound());
    }

    // -----------------------------------------------------------------------
    // POST /api/tenants
    // -----------------------------------------------------------------------

    @Test
    void createTenant_asAdmin_returns201WithLocation() throws Exception {
        when(tenantService.create(any())).thenReturn(detailDto);

        String body = """
                {
                  "fullName": "María Pérez",
                  "unit": "Depto 301",
                  "contact": "maria@mail.com",
                  "paymentDay": 5,
                  "startDate": "2026-07-01",
                  "monthlyAmount": 450.00,
                  "username": "maria.perez",
                  "initialPassword": "mi-clave-segura"
                }
                """;

        mockMvc.perform(post("/api/tenants")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void createTenant_duplicateUsername_returns409() throws Exception {
        when(tenantService.create(any())).thenThrow(new UsernameTakenException("maria.perez"));

        String body = """
                {
                  "fullName": "María Pérez",
                  "unit": "Depto 301",
                  "contact": "maria@mail.com",
                  "paymentDay": 5,
                  "startDate": "2026-07-01",
                  "monthlyAmount": 450.00,
                  "username": "maria.perez",
                  "initialPassword": "mi-clave-segura"
                }
                """;

        mockMvc.perform(post("/api/tenants")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void createTenant_invalidPayload_returns400() throws Exception {
        // fullName blank + missing required fields → Bean Validation should reject
        String body = """
                {
                  "fullName": "",
                  "unit": "Depto 301"
                }
                """;

        mockMvc.perform(post("/api/tenants")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createTenant_anonymous_returns401() throws Exception {
        mockMvc.perform(post("/api/tenants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    // -----------------------------------------------------------------------
    // PUT /api/tenants/{id}/paid-months/{yearMonth}
    // -----------------------------------------------------------------------

    @Test
    void markMonthPaid_asAdmin_returns200() throws Exception {
        when(tenantService.markMonthPaid(anyLong(), any())).thenReturn(detailDto);

        mockMvc.perform(put("/api/tenants/1/paid-months/2026-09")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void markMonthPaid_asArrendatario_returns403() throws Exception {
        mockMvc.perform(put("/api/tenants/1/paid-months/2026-09")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ARRENDATARIO"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void markMonthPaid_invalidYearMonth_returns400() throws Exception {
        mockMvc.perform(put("/api/tenants/1/paid-months/invalid-month")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isBadRequest());
    }

    // -----------------------------------------------------------------------
    // DELETE /api/tenants/{id}/paid-months/{yearMonth}
    // -----------------------------------------------------------------------

    @Test
    void unmarkMonthPaid_asAdmin_returns200() throws Exception {
        when(tenantService.unmarkMonthPaid(anyLong(), any())).thenReturn(detailDto);

        mockMvc.perform(delete("/api/tenants/1/paid-months/2026-07")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk());
    }

    // -----------------------------------------------------------------------
    // POST /api/tenants/{id}/charges
    // -----------------------------------------------------------------------

    @Test
    void addCharge_asAdmin_returns201() throws Exception {
        when(tenantService.addCharge(anyLong(), any())).thenReturn(detailDto);

        String body = """
                {
                  "description": "Daños en ventana",
                  "amount": 100.00
                }
                """;

        mockMvc.perform(post("/api/tenants/1/charges")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
    }

    // -----------------------------------------------------------------------
    // PUT /api/tenants/{id}/charges/{chargeId}/paid
    // -----------------------------------------------------------------------

    @Test
    void markChargePaid_asAdmin_returns200() throws Exception {
        when(tenantService.markChargePaid(anyLong(), anyLong())).thenReturn(detailDto);

        mockMvc.perform(put("/api/tenants/1/charges/10/paid")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk());
    }

    @Test
    void markChargePaid_chargeNotFound_returns404() throws Exception {
        when(tenantService.markChargePaid(anyLong(), anyLong()))
                .thenThrow(new TenantNotFoundException("Cargo no encontrado: 999"));

        mockMvc.perform(put("/api/tenants/1/charges/999/paid")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isNotFound());
    }
}
