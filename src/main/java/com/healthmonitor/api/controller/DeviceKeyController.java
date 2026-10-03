package com.healthmonitor.api.controller;

import com.healthmonitor.api.model.DeviceKey;
import com.healthmonitor.api.repository.DeviceKeyRepository;
import com.healthmonitor.api.security.DeviceKeyAuthFilter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Manages sensor keys. The plaintext key exists only in the creation
 * response; the database stores its SHA-256 hash.
 */
@RestController
@RequestMapping("/api/device-keys")
@PreAuthorize("hasRole('DOCTOR')")
public class DeviceKeyController {

    public record CreateRequest(@NotBlank String name) {
    }

    private final DeviceKeyRepository repository;
    private final SecureRandom random = new SecureRandom();

    public DeviceKeyController(DeviceKeyRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<Map<String, Object>> list() {
        return repository.findAll().stream()
            .map(key -> Map.<String, Object>of(
                "id", key.getId(),
                "name", key.getName(),
                "keyHashPreview", key.getKeyHash().substring(0, 12),
                "createdAt", key.getCreatedAt()
            ))
            .toList();
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@Valid @RequestBody CreateRequest request) {
        byte[] secret = new byte[20];
        random.nextBytes(secret);
        String plaintext = "dk_" + HexFormat.of().formatHex(secret);

        DeviceKey key = new DeviceKey();
        key.setName(request.name().trim());
        key.setKeyHash(DeviceKeyAuthFilter.sha256(plaintext));
        key.setCreatedAt(Instant.now());
        DeviceKey saved = repository.save(key);

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
            "id", saved.getId(),
            "name", saved.getName(),
            "key", plaintext,
            "hint", "Copy it now: only the SHA-256 hash is stored. Send it as X-Device-Key."
        ));
    }
}
