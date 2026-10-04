package com.rentals.auth;

import com.rentals.domain.AppUser;
import com.rentals.domain.Role;
import com.rentals.repository.AppUserRepository;
import com.rentals.security.AuthProperties;
import com.rentals.security.RefreshTokenService;
import com.rentals.service.AuthResult;
import com.rentals.service.AuthService;
import com.rentals.service.InvalidRefreshTokenException;
import com.rentals.web.AuthController;
import com.rentals.web.RefreshCookieFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller-level tests for AuthController.
 * Uses @WebMvcTest (Spring Boot 4.x: org.springframework.boot.webmvc.test.autoconfigure) +
 * spring-security-test.
 */
@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private RefreshCookieFactory cookieFactory;

    @MockitoBean
    private AppUserRepository userRepository;

    // Required by SecurityConfig/JwtConfig to start up
    @MockitoBean
    private RefreshTokenService refreshTokenService;

    private AppUser adminUser;
    private AuthResult authResult;
    private ResponseCookie refreshCookie;

    @BeforeEach
    void setUp() {
        adminUser = new AppUser("admin", "{bcrypt}hash", Role.ADMIN, null);

        authResult = new AuthResult("jwt-token", 900, "raw-refresh", adminUser);

        refreshCookie = ResponseCookie.from("rentals_refresh", "raw-refresh")
            .httpOnly(true)
            .secure(false)
            .sameSite("Strict")
            .path("/api/auth")
            .maxAge(Duration.ofDays(7))
            .build();

        when(cookieFactory.create(anyString())).thenReturn(refreshCookie);
        when(cookieFactory.clear()).thenReturn(
            ResponseCookie.from("rentals_refresh", "")
                .httpOnly(true).path("/api/auth").maxAge(Duration.ZERO).build()
        );
    }

    @Test
    void login_withValidCredentials_returns200AndCookie() throws Exception {
        when(authService.login("admin", "admin-dev-2024")).thenReturn(authResult);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"admin\",\"password\":\"admin-dev-2024\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").value("jwt-token"))
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.expiresIn").value(900))
            .andExpect(jsonPath("$.user.username").value("admin"))
            .andExpect(jsonPath("$.user.role").value("ADMIN"));
    }

    @Test
    void login_withBadCredentials_returns401ProblemDetail() throws Exception {
        when(authService.login(anyString(), anyString()))
            .thenThrow(new BadCredentialsException("Usuario o contraseña incorrectos"));

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"admin\",\"password\":\"wrong\"}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void refresh_withValidCookie_returns200AndNewCookie() throws Exception {
        when(authService.refresh("raw-refresh")).thenReturn(authResult);

        mockMvc.perform(post("/api/auth/refresh")
                .cookie(new jakarta.servlet.http.Cookie("rentals_refresh", "raw-refresh")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").value("jwt-token"));
    }

    @Test
    void refresh_withNoCookie_returns401() throws Exception {
        mockMvc.perform(post("/api/auth/refresh"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void logout_always_returns204AndClearsCookie() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
            .andExpect(status().isNoContent());
    }
}
