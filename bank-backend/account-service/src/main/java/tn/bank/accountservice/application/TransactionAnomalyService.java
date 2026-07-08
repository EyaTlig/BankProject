package tn.bank.accountservice.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.bank.accountservice.domain.Account;
import tn.bank.accountservice.domain.SecurityAlert;
import tn.bank.accountservice.domain.SecurityAlertSeverity;
import tn.bank.accountservice.domain.SecurityAlertType;
import tn.bank.accountservice.domain.Transaction;
import tn.bank.accountservice.infrastructure.SecurityAlertRepository;
import tn.bank.accountservice.infrastructure.TransactionRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionAnomalyService {

    // Seuils volontairement simples et explicables :
    // - un virement sortant jugé "inhabituel" s'il dépasse 5x la moyenne des
    //   10 dernières transactions du compte (avec un plancher pour éviter le
    //   bruit sur les petits comptes qui viennent de démarrer).
    // - une "fréquence anormale" si 5 virements sortants ou plus partent du
    //   même compte en moins de 10 minutes.
    private static final BigDecimal UNUSUAL_AMOUNT_MULTIPLIER = BigDecimal.valueOf(5);
    private static final BigDecimal MINIMUM_FLOOR = BigDecimal.valueOf(500);
    private static final int FREQUENCY_WINDOW_MINUTES = 10;
    private static final int FREQUENCY_THRESHOLD = 5;

    private final TransactionRepository transactionRepository;
    private final SecurityAlertRepository securityAlertRepository;

    /**
     * À appeler juste après la création d'une transaction de débit (virement
     * sortant confirmé) sur le compte source.
     */
    public void checkOutgoingTransfer(Account sourceAccount, BigDecimal amount) {
        checkUnusualAmount(sourceAccount, amount);
        checkHighFrequency(sourceAccount);
    }

    private void checkUnusualAmount(Account account, BigDecimal amount) {
        List<Transaction> recent = transactionRepository.findTop10ByAccountOrderByDateDesc(account);

        if (recent.size() < 3) {
            // Historique trop court pour établir une moyenne fiable
            return;
        }

        BigDecimal sum = recent.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal average = sum.divide(BigDecimal.valueOf(recent.size()), 2, java.math.RoundingMode.HALF_UP);

        BigDecimal threshold = average.multiply(UNUSUAL_AMOUNT_MULTIPLIER).max(MINIMUM_FLOOR);

        if (amount.compareTo(threshold) <= 0) {
            return;
        }

        LocalDateTime windowStart = LocalDateTime.now().minusMinutes(FREQUENCY_WINDOW_MINUTES);
        boolean alreadyAlerted = securityAlertRepository
                .existsByRelatedAccountNumberAndTypeAndResolvedFalseAndCreatedAtAfter(
                        account.getAccountNumber(), SecurityAlertType.UNUSUAL_AMOUNT, windowStart
                );
        if (alreadyAlerted) {
            return;
        }

        securityAlertRepository.save(
                SecurityAlert.builder()
                        .type(SecurityAlertType.UNUSUAL_AMOUNT)
                        .severity(SecurityAlertSeverity.MEDIUM)
                        .message("Virement de " + amount + " TND depuis le compte "
                                + account.getAccountNumber() + " — largement au-dessus de la moyenne habituelle ("
                                + average + " TND).")
                        .relatedEmail(account.getClient().getEmail())
                        .relatedAccountNumber(account.getAccountNumber())
                        .resolved(false)
                        .createdAt(LocalDateTime.now())
                        .build()
        );
    }

    private void checkHighFrequency(Account account) {
        LocalDateTime windowStart = LocalDateTime.now().minusMinutes(FREQUENCY_WINDOW_MINUTES);
        long recentCount = transactionRepository.countByAccountAndDateAfter(account, windowStart);

        if (recentCount < FREQUENCY_THRESHOLD) {
            return;
        }

        boolean alreadyAlerted = securityAlertRepository
                .existsByRelatedAccountNumberAndTypeAndResolvedFalseAndCreatedAtAfter(
                        account.getAccountNumber(), SecurityAlertType.HIGH_FREQUENCY_TRANSFERS, windowStart
                );
        if (alreadyAlerted) {
            return;
        }

        securityAlertRepository.save(
                SecurityAlert.builder()
                        .type(SecurityAlertType.HIGH_FREQUENCY_TRANSFERS)
                        .severity(SecurityAlertSeverity.HIGH)
                        .message(recentCount + " opérations sur le compte " + account.getAccountNumber()
                                + " en moins de " + FREQUENCY_WINDOW_MINUTES + " minutes.")
                        .relatedEmail(account.getClient().getEmail())
                        .relatedAccountNumber(account.getAccountNumber())
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
                alert.getRelatedAccountNumber(),
                alert.isResolved(),
                alert.getCreatedAt()
        );
    }
}
