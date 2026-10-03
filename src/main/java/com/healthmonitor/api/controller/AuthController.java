package com.healthmonitor.api.controller;

import com.healthmonitor.api.model.UserAccount;
import com.healthmonitor.api.repository.UserAccountRepository;
import com.healthmonitor.api.security.JwtService;
import com.healthmonitor.api.service.AccountUserDetailsService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Login endpoint that exchanges username/password for a JWT. The token is
 * stored by the frontend and sent as Authorization: Bearer &lt;token&gt;.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {
    }

    private final UserAccountRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthController(UserAccountRepository users, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        UserAccount account = users.findByUsername(request.username().trim()).orElse(null);
        if (account == null || !passwordEncoder.matches(request.password(), account.getPasswordHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "invalid credentials"));
        }
        return ResponseEntity.ok(Map.of(
            "token", jwtService.issue(account),
            "role", account.getRole().name(),
            "username", account.getUsername(),
            "name", account.getDisplayName(),
            "expiresInMinutes", jwtService.getTtl().toMinutes()
        ));
    }

    @GetMapping("/me")
    public Map<String, Object> me(Authentication authentication) {
        String role = AccountUserDetailsService.roleName(authentication.getAuthorities());
        String name = authentication.getName();
        if (authentication.getPrincipal() instanceof org.springframework.security.oauth2.jwt.Jwt jwt) {
            Object claim = jwt.getClaim("name");
            if (claim instanceof String s && !s.isBlank()) {
                name = s;
            }
        }
        return Map.of("username", authentication.getName(), "name", name, "role", role);
    }
}
