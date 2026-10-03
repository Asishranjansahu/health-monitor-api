package com.healthmonitor.api.config;

import com.healthmonitor.api.security.DeviceKeyAuthFilter;
import com.healthmonitor.api.security.JwtService;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Stateless JWT security. Static pages, login, Swagger, the health endpoint
 * and the WebSocket upgrade are public; everything under /api requires a
 * Bearer token (or a device key on POST /api/vitals). Fine-grained rules live
 * on the controllers as @PreAuthorize annotations.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        DeviceKeyAuthFilter deviceKeyAuthFilter
    ) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/",
                    "/*.html",
                    "/css/*",
                    "/js/*",
                    "/favicon.ico",
                    "/api/auth/login",
                    "/swagger-ui.html",
                    "/swagger-ui/*",
                    "/v3/api-docs",
                    "/v3/api-docs/**",
                    "/actuator/health",
                    "/ws/**",
                    "/h2-console/**"
                )
                .permitAll()
                .anyRequest()
                .authenticated()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((req, res, ex2) -> writeJson(res, HttpStatus.UNAUTHORIZED))
                .accessDeniedHandler((req, res, ex2) -> writeJson(res, HttpStatus.FORBIDDEN))
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
            )
            .addFilterBefore(deviceKeyAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /** Map the "roles" claim (e.g. ["ROLE_DOCTOR"]) to granted authorities. */
    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            List<String> roles = jwt.getClaimAsStringList("roles");
            List<GrantedAuthority> authorities = new java.util.ArrayList<>();
            if (roles != null) {
                for (String role : roles) {
                    authorities.add(new SimpleGrantedAuthority(role));
                }
            }
            return authorities;
        });
        return converter;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private static void writeJson(HttpServletResponse res, HttpStatus status) throws IOException {
        res.setStatus(status.value());
        res.setContentType("application/json");
        res.getWriter().write("{\"error\":\"" + status.getReasonPhrase().toLowerCase() + "\"}");
    }
}
