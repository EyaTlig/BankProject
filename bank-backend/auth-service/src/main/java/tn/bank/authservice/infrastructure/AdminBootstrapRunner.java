package tn.bank.authservice.infrastructure;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import tn.bank.authservice.domain.Role;
import tn.bank.authservice.domain.User;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminBootstrapRunner implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.bootstrap.email}")
    private String bootstrapEmail;

    @Value("${admin.bootstrap.password}")
    private String bootstrapPassword;

    @Override
    public void run(String... args) {
        if (userRepository.existsByRole(Role.ADMIN)) {
            return;
        }

        User admin = User.builder()
                .email(bootstrapEmail)
                .password(passwordEncoder.encode(bootstrapPassword))
                .firstName("Super")
                .lastName("Admin")
                .role(Role.ADMIN)
                .enabled(true)
                .build();

        userRepository.save(admin);

        log.warn("=== Compte administrateur créé automatiquement : {} / {} (à changer en production) ===",
                bootstrapEmail, bootstrapPassword);
    }
}
