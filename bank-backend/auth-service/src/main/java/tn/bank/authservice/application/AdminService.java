package tn.bank.authservice.application;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.bank.authservice.domain.AuditLog;
import tn.bank.authservice.domain.User;
import tn.bank.authservice.infrastructure.AuditLogRepository;
import tn.bank.authservice.infrastructure.OtpService;
import tn.bank.authservice.infrastructure.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogRepository auditLogRepository;
    private final OtpService otpService;

    public List<AdminUserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public AdminUserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable"));
        return toResponse(user);
    }

    @Transactional
    public AdminUserResponse updateUserStatus(Long id, UpdateUserStatusRequest request, String performedBy) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable"));
        user.setEnabled(request.isEnabled());
        User saved = userRepository.save(user);

        logAction(
                request.isEnabled() ? "REACTIVATION_UTILISATEUR" : "DESACTIVATION_UTILISATEUR",
                saved.getEmail(),
                performedBy,
                null
        );

        return toResponse(saved);
    }

    @Transactional
    public AdminUserResponse updateUserRole(Long id, UpdateUserRoleRequest request, String performedBy) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable"));
        String previousRole = user.getRole().name();
        user.setRole(request.getRole());
        User saved = userRepository.save(user);

        logAction(
                "MODIFICATION_ROLE",
                saved.getEmail(),
                performedBy,
                previousRole + " -> " + request.getRole()
        );

        return toResponse(saved);
    }

    @Transactional
    public void deleteUser(Long id, String performedBy) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable"));

        String targetEmail = user.getEmail();
        userRepository.delete(user);

        logAction("SUPPRESSION_UTILISATEUR", targetEmail, performedBy, null);
    }

    @Transactional
    public void resetPassword(Long id, String performedBy) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable"));
        String tempPassword = UUID.randomUUID().toString().substring(0, 8);
        user.setPassword(passwordEncoder.encode(tempPassword));
        userRepository.save(user);

        otpService.sendPasswordResetEmail(user.getEmail(), tempPassword);

        logAction("REINITIALISATION_MOT_DE_PASSE", user.getEmail(), performedBy, null);
    }

    public List<AuditLogResponse> getAuditLogs() {
        return auditLogRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(log -> new AuditLogResponse(
                        log.getId(),
                        log.getAction(),
                        log.getTargetEmail(),
                        log.getPerformedBy(),
                        log.getDetails(),
                        log.getCreatedAt()
                ))
                .toList();
    }

    private void logAction(String action, String targetEmail, String performedBy, String details) {
        AuditLog log = AuditLog.builder()
                .action(action)
                .targetEmail(targetEmail)
                .performedBy(performedBy)
                .details(details)
                .createdAt(LocalDateTime.now())
                .build();
        auditLogRepository.save(log);
    }

    private AdminUserResponse toResponse(User user) {
        return new AdminUserResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole(),
                user.isEnabled()
        );
    }
}