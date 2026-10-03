package com.healthmonitor.api.repository;

import com.healthmonitor.api.model.DeviceKey;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceKeyRepository extends JpaRepository<DeviceKey, Long> {
    Optional<DeviceKey> findByKeyHash(String keyHash);
}
