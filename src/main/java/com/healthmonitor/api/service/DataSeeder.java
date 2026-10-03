package com.healthmonitor.api.service;

import com.healthmonitor.api.model.DeviceKey;
import com.healthmonitor.api.model.Role;
import com.healthmonitor.api.model.UserAccount;
import com.healthmonitor.api.repository.DeviceKeyRepository;
import com.healthmonitor.api.repository.UserAccountRepository;
import com.healthmonitor.api.security.DeviceKeyAuthFilter;
import java.util.logging.Logger;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seeds development accounts and one demo sensor key on an empty database.
 * Passwords are only ever written as BCrypt hashes.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = Logger.getLogger(DataSeeder.class.getName());

    private final UserAccountRepository users;
    private final DeviceKeyRepository deviceKeys;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserAccountRepository users, DeviceKeyRepository deviceKeys, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.deviceKeys = deviceKeys;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (users.count() == 0) {
            create("doctor", "doctor123", "Doctor Demo", Role.DOCTOR);
            create("nurse", "nurse123", "Nurse Demo", Role.NURSE);
            log.info("Seeded default accounts: doctor (DOCTOR) and nurse (NURSE)");
        }
        if (deviceKeys.count() == 0) {
            DeviceKey key = new DeviceKey();
            key.setName("demo-simulator");
            key.setKeyHash(DeviceKeyAuthFilter.sha256("demo-device-key"));
            deviceKeys.save(key);
            log.info("Seeded demo device key: X-Device-Key: demo-device-key");
        }
    }

    private void create(String username, String rawPassword, String displayName, Role role) {
        UserAccount account = new UserAccount();
        account.setUsername(username);
        account.setPasswordHash(passwordEncoder.encode(rawPassword));
        account.setDisplayName(displayName);
        account.setRole(role);
        users.save(account);
    }
}
