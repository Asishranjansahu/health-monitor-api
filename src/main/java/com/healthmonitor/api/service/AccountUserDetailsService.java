package com.healthmonitor.api.service;

import com.healthmonitor.api.model.Role;
import com.healthmonitor.api.model.UserAccount;
import com.healthmonitor.api.repository.UserAccountRepository;
import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/** Bridges the user_account table to Spring Security's login machinery. */
@Service
public class AccountUserDetailsService implements UserDetailsService {

    private final UserAccountRepository repository;

    public AccountUserDetailsService(UserAccountRepository repository) {
        this.repository = repository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserAccount account = repository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("Unknown user: " + username));
        return User.withUsername(account.getUsername())
            .password(account.getPasswordHash())
            .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + account.getRole().name())))
            .build();
    }

    /** Resolve the account behind a role authority, used by /api/auth/me. */
    public static String roleName(Iterable<? extends org.springframework.security.core.GrantedAuthority> authorities) {
        for (org.springframework.security.core.GrantedAuthority authority : authorities) {
            String name = authority.getAuthority();
            if (name != null && name.startsWith("ROLE_")) {
                return name.substring("ROLE_".length());
            }
        }
        return "UNKNOWN";
    }

    public static Role parseRole(String roleName) {
        return Role.valueOf(roleName);
    }
}
