package tn.bank.authservice.infrastructure;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import tn.bank.authservice.domain.AdminPermission;
import tn.bank.authservice.domain.Role;
import tn.bank.authservice.domain.User;

import java.util.HashSet;
import java.util.List;

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
        if (!userRepository.existsByRole(Role.ADMIN)) {
            User admin = User.builder()
                    .email(bootstrapEmail)
                    .password(passwordEncoder.encode(bootstrapPassword))
                    .firstName("Super")
                    .lastName("Admin")
                    .role(Role.ADMIN)
                    .enabled(true)
                    .permissions(new HashSet<>(List.of(AdminPermission.values())))
                    .build();

            userRepository.save(admin);

            log.warn("=== Compte administrateur créé automatiquement : {} / {} (à changer en production) ===",
                    bootstrapEmail, bootstrapPassword);
        }

        // Migration en douceur : tout admin existant sans permissions (créé avant l'introduction
        // du systeme de permissions granulaires) recoit l'ensemble des permissions par defaut,
        // pour ne pas se retrouver bloque partout apres la mise a jour.
        userRepository.findAll().stream()
                .filter(user -> user.getRole() == Role.ADMIN && user.getPermissions().isEmpty())
                .forEach(user -> {
                    user.setPermissions(new HashSet<>(List.of(AdminPermission.values())));
                    userRepository.save(user);
                    log.info("Permissions par defaut attribuees a l'admin existant : {}", user.getEmail());
                });
    }
}