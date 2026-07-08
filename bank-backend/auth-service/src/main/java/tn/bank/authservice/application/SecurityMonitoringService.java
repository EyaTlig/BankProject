package tn.bank.authservice.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.bank.authservice.domain.LoginAttempt;
import tn.bank.authservice.domain.SecurityAlert;
import tn.bank.authservice.domain.SecurityAlertSeverity;
import tn.bank.authservice.domain.SecurityAlertType;
import tn.bank.authservice.infrastructure.LoginAttemptRepository;
import tn.bank.authservice.infrastructure.SecurityAlertRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SecurityMonitoringService {

    // Seuils de détection — volontairement simples et lisibles pour rester
    // faciles à ajuster/justifier en soutenance.
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int WINDOW_MINUTES = 15;

    private final LoginAttemptRepository loginAttemptRepository;
    private final SecurityAlertRepository securityAlertRepository;

    /**
     * Enregistre une tentative de connexion et déclenche une alerte si le
     * nombre d'échecs récents pour cet email dépasse le seuil.
     */
    public void recordLoginAttempt(String email, boolean success, String ipAddress) {
        loginAttemptRepository.save(
                LoginAttempt.builder()
                        .email(email)
                        .success(success)
                        .ipAddress(ipAddress)
                        .createdAt(LocalDateTime.now())
                        .build()
        );

        if (success) {
            return;
        }

        LocalDateTime windowStart = LocalDateTime.now().minusMinutes(WINDOW_MINUTES);
        long recentFailures = loginAttemptRepository
                .countByEmailAndSuccessFalseAndCreatedAtAfter(email, windowStart);

        if (recentFailures < MAX_FAILED_ATTEMPTS) {
            return;
        }

        boolean alreadyAlerted = securityAlertRepository.existsByRelatedEmailAndTypeAndResolvedFalseAndCreatedAtAfter(
                email, SecurityAlertType.BRUTE_FORCE_LOGIN, windowStart
        );

        if (alreadyAlerted) {
            return;
        }

        securityAlertRepository.save(
                SecurityAlert.builder()
                        .type(SecurityAlertType.BRUTE_FORCE_LOGIN)
                        .severity(SecurityAlertSeverity.HIGH)
                        .message(recentFailures + " tentatives de connexion échouées pour "
                                + email + " en moins de " + WINDOW_MINUTES + " minutes.")
                        .relatedEmail(email)
                        .resolved(false)
                        .createdAt(LocalDateTime.now())
                        .build()
        );
    }

    public List<SecurityAlertResponse> getAllAlerts() {
        return securityAlertRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public long getUnresolvedCount() {
        return securityAlertRepository.countByResolvedFalse();
    }

    public SecurityAlertResponse resolveAlert(Long id) {
        SecurityAlert alert = securityAlertRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Alerte introuvable"));

        alert.setResolved(true);
        alert.setResolvedAt(LocalDateTime.now());
        securityAlertRepository.save(alert);

        return toResponse(alert);
    }

    private SecurityAlertResponse toResponse(SecurityAlert alert) {
        return new SecurityAlertResponse(
                alert.getId(),
                alert.getType().name(),
                alert.getSeverity().name(),
                alert.getMessage(),
                alert.getRelatedEmail(),
                alert.isResolved(),
                alert.getCreatedAt()
        );
    }
}
