package com.commandos.identity.application;

import com.commandos.identity.api.dto.AuthUserResponse;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.spec.SecretKeySpec;
import java.time.Clock;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenServiceTest {

    @Test
    void accessTokenCarriesTheAuthorizedOrganizationAndPermissions() {
        byte[] key = "test-signing-key-with-at-least-32-bytes".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        SecretKeySpec secretKey = new SecretKeySpec(key, "HmacSHA256");
        var encoder = new NimbusJwtEncoder(new ImmutableSecret<>(secretKey));
        JwtDecoder decoder = NimbusJwtDecoder.withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        ((NimbusJwtDecoder) decoder).setJwtValidator(JwtValidators.createDefaultWithIssuer("commandos-api"));
        JwtTokenService tokenService = new JwtTokenService(
                encoder,
                Clock.systemUTC(),
                "commandos-api",
                900);
        UUID userId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();

        String encodedToken = tokenService.create(new AuthUserResponse(
                userId,
                "alex@example.com",
                "Alex Morgan",
                organizationId,
                "Northstar Studio",
                "northstar",
                "Owner",
                Set.of("organization:read", "members:manage")));
        var decoded = decoder.decode(encodedToken);

        assertThat(decoded.getSubject()).isEqualTo(userId.toString());
        assertThat(decoded.getClaimAsString("org_id")).isEqualTo(organizationId.toString());
        assertThat(decoded.getClaimAsStringList("permissions"))
                .containsExactlyInAnyOrder("organization:read", "members:manage");
        assertThat(decoded.getExpiresAt()).isEqualTo(decoded.getIssuedAt().plusSeconds(900));
    }
}
