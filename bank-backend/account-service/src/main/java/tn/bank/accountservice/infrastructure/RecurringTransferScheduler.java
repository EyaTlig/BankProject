package tn.bank.accountservice.infrastructure;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tn.bank.accountservice.application.RecurringTransferService;
import tn.bank.accountservice.domain.RecurringTransfer;
import tn.bank.accountservice.domain.RecurringTransferStatus;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RecurringTransferScheduler {

    private final RecurringTransferRepository recurringTransferRepository;
    private final RecurringTransferService recurringTransferService;
    private final TransferOtpService transferOtpService;

    // MODE TEST : tourne toutes les 30 secondes (remettre "0 0 2 * * *" avant la démo finale)
    @Scheduled(cron = "*/30 * * * * *")
    public void executeRecurringTransfers() {

        LocalDate today = LocalDate.now();
        log.info("=== Scheduler virements permanents : vérification des échéances du {} ===", today);

        List<RecurringTransfer> dueTransfers = recurringTransferRepository
                .findByStatusAndNextExecutionDate(RecurringTransferStatus.ACTIVE, today);

        log.info("=== {} virement(s) permanent(s) à exécuter aujourd'hui ===", dueTransfers.size());

        for (RecurringTransfer transfer : dueTransfers) {
            recurringTransferService.executeRecurringTransfer(transfer);
        }
    }

    // Tourne chaque jour à 18h00, envoie un rappel pour les échéances de demain
    @Scheduled(cron = "0 0 18 * * *")
    public void sendExecutionReminders() {

        LocalDate tomorrow = LocalDate.now().plusDays(1);
        log.info("=== Scheduler rappels : vérification des échéances de demain ({}) ===", tomorrow);

        List<RecurringTransfer> upcomingTransfers = recurringTransferRepository
                .findByStatusAndNextExecutionDate(RecurringTransferStatus.ACTIVE, tomorrow);

        for (RecurringTransfer transfer : upcomingTransfers) {
            transferOtpService.sendExecutionReminder(
                    transfer.getSourceAccount().getClient().getEmail(),
                    transfer.getAmount(),
                    transfer.getDestinationAccountNumber(),
                    tomorrow
            );
        }

        log.info("=== {} rappel(s) envoyé(s) ===", upcomingTransfers.size());
    }
}