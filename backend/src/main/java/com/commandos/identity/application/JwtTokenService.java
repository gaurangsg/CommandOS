package com.commandos.identity.application;

import com.commandos.identity.api.dto.AuthUserResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
public class JwtTokenService {

    private final JwtEncoder encoder;
    private final Clock clock;
    private final String issuer;
    private final long accessTokenSeconds;

    public JwtTokenService(
            JwtEncoder encoder,
            Clock clock,
            @Value("${commandos.security.jwt.issuer}") String issuer,
            @Value("${commandos.security.jwt.access-token-seconds}") long accessTokenSeconds) {
        this.encoder = encoder;
        this.clock = clock;
        this.issuer = issuer;
        this.accessTokenSeconds = accessTokenSeconds;
        if (accessTokenSeconds < 300 || accessTokenSeconds > 3600) {
            throw new IllegalArgumentException("JWT access-token lifetime must be between 300 and 3600 seconds");
        }
    }

    public String create(AuthUserResponse user) {
        Instant issuedAt = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plusSeconds(accessTokenSeconds))
                .subject(user.userId().toString())
                .id(UUID.randomUUID().toString())
                .claim("org_id", user.organizationId().toString())
                .claim("org_name", user.organizationName())
                .claim("org_slug", user.organizationSlug())
                .claim("email", user.email())
                .claim("name", user.displayName())
                .claim("role", user.role())
                .claim("permissions", user.permissions())
                .build();

        return encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).type("JWT").build(),
                claims)).getTokenValue();
    }

    public long getAccessTokenSeconds() {
        return accessTokenSeconds;
    }
}
