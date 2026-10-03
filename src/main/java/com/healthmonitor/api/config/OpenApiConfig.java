package com.healthmonitor.api.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.security.SecuritySchemes;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

/** Swagger metadata: Bearer JWT for clinicians, X-Device-Key for sensors. */
@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "Health Monitor API",
        version = "1.0",
        description = "Patient vitals, threshold-based alerts and real-time WebSocket delivery."
    ),
    servers = @Server(url = "/", description = "This server")
)
@SecuritySchemes({
    @SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "JWT from POST /api/auth/login, sent as Authorization: Bearer <token>"
    ),
    @SecurityScheme(
        name = "deviceKey",
        type = SecuritySchemeType.APIKEY,
        in = SecuritySchemeIn.HEADER,
        paramName = "X-Device-Key",
        description = "Sensor key issued by POST /api/device-keys"
    )
})
public class OpenApiConfig {
}
