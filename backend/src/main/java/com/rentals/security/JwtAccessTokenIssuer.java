package com.rentals.security;

import com.rentals.domain.AppUser;
import com.rentals.service.AccessTokenIssuer;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;

/**
 * Issues HS256 JWT access tokens.
 * Implements AccessTokenIssuer so services don't import Nimbus.
 */
@Component
public class JwtAccessTokenIssuer implements AccessTokenIssuer {

    private final NimbusJwtEncoder encoder;
    private final AuthProperties authProperties;
    private final Clock clock;

    public JwtAccessTokenIssuer(NimbusJwtEncoder encoder, AuthProperties authProperties, Clock clock) {
        this.encoder = encoder;
        this.authProperties = authProperties;
        this.clock = clock;
    }

    @Override
    public IssuedToken issue(AppUser user) {
        Instant now = Instant.now(clock);
        long ttlSeconds = authProperties.accessTokenTtl().getSeconds();
        Instant exp = now.plusSeconds(ttlSeconds);

        JwtClaimsSet.Builder builder = JwtClaimsSet.builder()
            .issuer(authProperties.issuer())
            .subject(String.valueOf(user.getId()))
            .claim("username", user.getUsername())
            .claim("role", user.getRole().name())
            .issuedAt(now)
            .expiresAt(exp);

        if (user.getTenant() != null) {
            builder.claim("tenantId", user.getTenant().getId());
        }

        JwtClaimsSet claims = builder.build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();

        String tokenValue = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new IssuedToken(tokenValue, ttlSeconds);
    }
}
