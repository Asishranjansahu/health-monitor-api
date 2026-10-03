package com.healthmonitor.api.security;

import com.healthmonitor.api.model.UserAccount;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Service;

/**
 * Issues and validates HS256 JWTs. The signing secret comes from
 * healthmonitor.jwt.secret (override with JWT_SECRET in production).
 */
@Service
public class JwtService {

    public static final String ISSUER = "health-monitor-api";

    private final JwtEncoder encoder;
    private final JwtDecoder decoder;
    private final Duration ttl;

    public JwtService(
        @Value("${healthmonitor.jwt.secret}") String secret,
        @Value("${healthmonitor.jwt.ttl-minutes}") long ttlMinutes
    ) {
        SecretKey key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        this.encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));

        NimbusJwtDecoder nimbus = NimbusJwtDecoder.withSecretKey(key)
            .macAlgorithm(MacAlgorithm.HS256)
            .build();
        nimbus.setJwtValidator(JwtValidators.createDefaultWithIssuer(ISSUER));
        this.decoder = nimbus;

        this.ttl = Duration.ofMinutes(ttlMinutes);
    }

    /** Create a signed access token for an account. */
    public String issue(UserAccount user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer(ISSUER)
            .subject(user.getUsername())
            .claim("name", user.getDisplayName())
            .claim("roles", List.of("ROLE_" + user.getRole().name()))
            .issuedAt(now)
            .expiresAt(now.plus(ttl))
            .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        Jwt token = encoder.encode(JwtEncoderParameters.from(header, claims));
        return token.getTokenValue();
    }

    public Duration getTtl() {
        return ttl;
    }

    /** Exposed as a bean so Spring Security's resource server can validate tokens. */
    @Bean
    public JwtDecoder jwtDecoder() {
        return decoder;
    }
}
