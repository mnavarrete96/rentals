package com.rentals.web;

import com.rentals.service.TenantService;
import com.rentals.web.dto.TenantDetailDto;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints for the currently authenticated ARRENDATARIO.
 * tenantId is read from the JWT claim (set during login by JwtAccessTokenIssuer).
 * IDOR is prevented by never accepting the tenantId from the URL.
 *
 * #8 GET /api/me/tenant → 200 TenantDetailDto
 *
 * ARCHITECTURE §6.1, §15.2.
 */
@RestController
@RequestMapping("/api/me")
public class MeController {

    private final TenantService tenantService;

    public MeController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    /**
     * Returns the tenant account for the authenticated ARRENDATARIO.
     * The tenantId is extracted from the JWT "tenantId" claim — never from the URL.
     */
    @GetMapping("/tenant")
    public ResponseEntity<TenantDetailDto> getMyTenant(@AuthenticationPrincipal Jwt jwt) {
        Long tenantId = jwt.getClaim("tenantId");
        return ResponseEntity.ok(tenantService.getMyTenant(tenantId));
    }
}
