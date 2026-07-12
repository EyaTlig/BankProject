package tn.bank.accountservice.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.bank.accountservice.domain.Account;
import tn.bank.accountservice.domain.Client;
import tn.bank.accountservice.domain.RecurringFrequency;
import tn.bank.accountservice.domain.RecurringTransfer;
import tn.bank.accountservice.domain.RecurringTransferStatus;
import tn.bank.accountservice.domain.Transaction;
import tn.bank.accountservice.domain.TransactionType;
import tn.bank.accountservice.infrastructure.AccountRepository;
import tn.bank.accountservice.infrastructure.ClientRepository;
import tn.bank.accountservice.infrastructure.RecurringTransferRepository;
import tn.bank.accountservice.infrastructure.TransactionRepository;
import tn.bank.accountservice.infrastructure.TransferOtpService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecurringTransferService {

    private final ClientRepository clientRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final RecurringTransferRepository recurringTransferRepository;
    private final TransferOtpService transferOtpService;

    public RecurringTransferResponse createRecurringTransfer(String email, CreateRecurringTransferRequest request) {

        Client client = clientRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Client introuvable"));

        Account sourceAccount = accountRepository.findById(request.getSourceAccountId())
                .orElseThrow(() -> new IllegalArgumentException("Compte source introuvable"));

        if (!sourceAccount.getClient().getId().equals(client.getId())) {
            throw new IllegalArgumentException("Ce compte ne vous appartient pas");
        }

        accountRepository.findByAccountNumber(request.getDestinationAccountNumber())
                .orElseThrow(() -> new IllegalArgumentException("Compte destinataire introuvable"));

        if (request.getEndDate() != null && request.getEndDate().isBefore(request.getStartDate())) {
            throw new IllegalArgumentException("La date de fin doit être après la date de début");
        }

        RecurringTransfer recurringTransfer = RecurringTransfer.builder()
                .sourceAccount(sourceAccount)
                .destinationAccountNumber(request.getDestinationAccountNumber())
                .amount(request.getAmount())
                .label(request.getLabel())
                .frequency(request.getFrequency())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .nextExecutionDate(request.getStartDate())
                .status(RecurringTransferStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();

        RecurringTransfer saved = recurringTransferRepository.save(recurringTransfer);

        return toResponse(saved);
    }

    public List<RecurringTransferResponse> getMyRecurringTransfers(String email) {

        Client client = clientRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Client introuvable"));

        return recurringTransferRepository.findBySourceAccount_Client(client).stream()
                .map(this::toResponse)
                .toList();
    }

    public RecurringTransferResponse cancelRecurringTransfer(String email, Long recurringTransferId) {

        Client client = clientRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Client introuvable"));

        RecurringTransfer recurringTransfer = recurringTransferRepository.findById(recurringTransferId)
                .orElseThrow(() -> new IllegalArgumentException("Virement permanent introuvable"));

        if (!recurringTransfer.getSourceAccount().getClient().getId().equals(client.getId())) {
            throw new IllegalArgumentException("Ce virement permanent ne vous appartient pas");
        }

        recurringTransfer.setStatus(RecurringTransferStatus.CANCELLED);
        RecurringTransfer saved = recurringTransferRepository.save(recurringTransfer);

        return toResponse(saved);
    }

    public RecurringTransferResponse pauseRecurringTransfer(String email, Long recurringTransferId) {

        RecurringTransfer recurringTransfer = findOwnedRecurringTransfer(email, recurringTransferId);

        if (recurringTransfer.getStatus() != RecurringTransferStatus.ACTIVE) {
            throw new IllegalArgumentException("Seul un virement permanent actif peut être mis en pause");
        }

        recurringTransfer.setStatus(RecurringTransferStatus.PAUSED);
        RecurringTransfer saved = recurringTransferRepository.save(recurringTransfer);

        return toResponse(saved);
    }

    public RecurringTransferResponse resumeRecurringTransfer(String email, Long recurringTransferId) {

        RecurringTransfer recurringTransfer = findOwnedRecurringTransfer(email, recurringTransferId);

        if (recurringTransfer.getStatus() != RecurringTransferStatus.PAUSED) {
            throw new IllegalArgumentException("Seul un virement permanent en pause peut être repris");
        }

        // Si la date de la prochaine echeance est passee pendant la pause, on avance
        // jusqu'a la prochaine echeance future (sans rejouer les occurrences manquees)
        LocalDate today = LocalDate.now();
        LocalDate nextDate = recurringTransfer.getNextExecutionDate();
        while (nextDate.isBefore(today)) {
            nextDate = computeNextExecutionDate(nextDate, recurringTransfer.getFrequency());
        }

        if (recurringTransfer.getEndDate() != null && nextDate.isAfter(recurringTransfer.getEndDate())) {
            recurringTransfer.setStatus(RecurringTransferStatus.COMPLETED);
        } else {
            recurringTransfer.setNextExecutionDate(nextDate);
            recurringTransfer.setStatus(RecurringTransferStatus.ACTIVE);
        }

        RecurringTransfer saved = recurringTransferRepository.save(recurringTransfer);

        return toResponse(saved);
    }

    private RecurringTransfer findOwnedRecurringTransfer(String email, Long recurringTransferId) {

        Client client = clientRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Client introuvable"));

        RecurringTransfer recurringTransfer = recurringTransferRepository.findById(recurringTransferId)
                .orElseThrow(() -> new IllegalArgumentException("Virement permanent introuvable"));

        if (!recurringTransfer.getSourceAccount().getClient().getId().equals(client.getId())) {
            throw new IllegalArgumentException("Ce virement permanent ne vous appartient pas");
        }

        return recurringTransfer;
    }

    @Transactional
    public void executeRecurringTransfer(RecurringTransfer recurringTransfer) {

        try {
            Account sourceAccount = accountRepository.findById(recurringTransfer.getSourceAccount().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Compte source introuvable"));

            Account destinationAccount = accountRepository.findByAccountNumber(recurringTransfer.getDestinationAccountNumber())
                    .orElseThrow(() -> new IllegalArgumentException("Compte destinataire introuvable"));

            if (sourceAccount.getBalance().compareTo(recurringTransfer.getAmount()) < 0) {
                log.warn("Solde insuffisant pour le virement permanent {} - exécution annulée",
                        recurringTransfer.getId());
                notifyExecutionFailed(sourceAccount.getClient().getEmail(), recurringTransfer, "Solde insuffisant");
                advanceToNextExecution(recurringTransfer);
                return;
            }

            BigDecimal newSourceBalance = sourceAccount.getBalance().subtract(recurringTransfer.getAmount());
            BigDecimal newDestinationBalance = destinationAccount.getBalance().add(recurringTransfer.getAmount());

            sourceAccount.setBalance(newSourceBalance);
            destinationAccount.setBalance(newDestinationBalance);
            accountRepository.save(sourceAccount);
            accountRepository.save(destinationAccount);

            transactionRepository.save(Transaction.builder()
                    .type(TransactionType.VIREMENT_SORTANT)
                    .amount(recurringTransfer.getAmount())
                    .balanceAfter(newSourceBalance)
                    .date(LocalDateTime.now())
                    .account(sourceAccount)
                    .build());

            transactionRepository.save(Transaction.builder()
                    .type(TransactionType.VIREMENT_ENTRANT)
                    .amount(recurringTransfer.getAmount())
                    .balanceAfter(newDestinationBalance)
                    .date(LocalDateTime.now())
                    .account(destinationAccount)
                    .build());

            notifyExecutionSuccess(sourceAccount.getClient().getEmail(), recurringTransfer, newSourceBalance);

            log.info("Virement permanent {} exécuté avec succès ({} DT)",
                    recurringTransfer.getId(), recurringTransfer.getAmount());

            advanceToNextExecution(recurringTransfer);

        } catch (Exception e) {
            log.error("Erreur lors de l'exécution du virement permanent {} : {}",
                    recurringTransfer.getId(), e.getMessage());
        }
    }

    private void advanceToNextExecution(RecurringTransfer recurringTransfer) {

        LocalDate nextDate = computeNextExecutionDate(
                recurringTransfer.getNextExecutionDate(),
                recurringTransfer.getFrequency()
        );

        if (recurringTransfer.getEndDate() != null && nextDate.isAfter(recurringTransfer.getEndDate())) {
            recurringTransfer.setStatus(RecurringTransferStatus.COMPLETED);
        } else {
            recurringTransfer.setNextExecutionDate(nextDate);
        }

        recurringTransferRepository.save(recurringTransfer);
    }

    private LocalDate computeNextExecutionDate(LocalDate current, RecurringFrequency frequency) {
        return switch (frequency) {
            case WEEKLY -> current.plusWeeks(1);
            case MONTHLY -> current.plusMonths(1);
            case QUARTERLY -> current.plusMonths(3);
            case YEARLY -> current.plusYears(1);
        };
    }

    private void notifyExecutionSuccess(String email, RecurringTransfer recurringTransfer, BigDecimal newBalance) {
        transferOtpService.sendExecutionConfirmation(
                email,
                recurringTransfer.getAmount(),
                recurringTransfer.getDestinationAccountNumber(),
                newBalance
        );
    }

    private void notifyExecutionFailed(String email, RecurringTransfer recurringTransfer, String reason) {
        transferOtpService.sendExecutionFailedAlert(
                email,
                recurringTransfer.getAmount(),
                recurringTransfer.getDestinationAccountNumber(),
                reason
        );
    }

    private RecurringTransferResponse toResponse(RecurringTransfer recurringTransfer) {
        return new RecurringTransferResponse(
                recurringTransfer.getId(),
                recurringTransfer.getDestinationAccountNumber(),
                recurringTransfer.getAmount(),
                recurringTransfer.getLabel(),
                recurringTransfer.getFrequency(),
                recurringTransfer.getStartDate(),
                recurringTransfer.getEndDate(),
                recurringTransfer.getNextExecutionDate(),
                recurringTransfer.getStatus()
        );
    }
}