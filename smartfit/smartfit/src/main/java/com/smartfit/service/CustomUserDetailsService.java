package com.smartfit.service;

import com.smartfit.entity.User;
import com.smartfit.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new UsernameNotFoundException("Utilizatorul cu email-ul " + email + " nu a fost găsit.");
        }

        // Normalizare rol: DB stocheaza 'USER', 'ADMIN', 'EMPLOYEE'
        // .roles() adauga automat prefixul ROLE_ (deci nu-l trimitem noi)
        String rawRole = user.getRole();
        if (rawRole == null || rawRole.isBlank()) rawRole = "USER";
        // Daca rolul vine deja cu prefix ROLE_ (legacy), il scoatem
        String role = rawRole.startsWith("ROLE_") ? rawRole.substring(5) : rawRole;

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPasswordHash())
                .roles(role)
                .build();
    }
}