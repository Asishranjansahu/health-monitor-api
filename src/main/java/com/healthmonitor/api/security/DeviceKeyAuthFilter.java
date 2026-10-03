package com.healthmonitor.api.security;

import com.healthmonitor.api.model.DeviceKey;
import com.healthmonitor.api.repository.DeviceKeyRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Authenticates sensors that present a raw key in the X-Device-Key header.
 * Requests that already carry a Bearer token are left to the JWT filter.
 * Only the SHA-256 hash of a key is compared, so the database never holds
 * usable secrets.
 */
@Component
public class DeviceKeyAuthFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Device-Key";

    private final DeviceKeyRepository deviceKeyRepository;

    public DeviceKeyAuthFilter(DeviceKeyRepository deviceKeyRepository) {
        this.deviceKeyRepository = deviceKeyRepository;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        String provided = request.getHeader(HEADER);
        boolean hasBearer = request.getHeader("Authorization") != null;

        if (provided == null || provided.isBlank() || hasBearer
            || SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        DeviceKey device = deviceKeyRepository.findByKeyHash(sha256(provided.trim())).orElse(null);
        if (device == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"error\":\"invalid device key\"}");
            return;
        }

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
            "device:" + device.getName(),
            "N/A",
            java.util.List.of(new SimpleGrantedAuthority("ROLE_DEVICE"))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);
    }

    /** Public so the seeder and the key-creation endpoint can hash keys too. */
    public static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
