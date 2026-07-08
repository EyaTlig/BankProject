package tn.bank.accountservice.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.bank.accountservice.domain.Account;
import tn.bank.accountservice.domain.Client;
import tn.bank.accountservice.domain.Transaction;
import tn.bank.accountservice.domain.TransactionType;
import tn.bank.accountservice.domain.Transfer;
import tn.bank.accountservice.domain.TransferStatus;
import tn.bank.accountservice.infrastructure.AccountRepository;
import tn.bank.accountservice.infrastructure.ClientRepository;
import tn.bank.accountservice.infrastructure.TransactionRepository;
import tn.bank.accountservice.infrastructure.TransferOtpService;
import tn.bank.accountservice.infrastructure.TransferRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TransferService {

    private final ClientRepository clientRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final TransferRepository transferRepository;
    private final TransferOtpService transferOtpService;
    private final TransactionAnomalyService transactionAnomalyService;

    public InitiateTransferResponse initiateTransfer(String email, InitiateTransferRequest request) {

        Client client = clientRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Client introuvable"));

        Account sourceAccount = accountRepository.findById(request.getSourceAccountId())
                .orElseThrow(() -> new IllegalArgumentException("Compte source introuvable"));

        System.out.println("=== DEBUG === client.getId() = " + client.getId() + " (classe: " + client.getId().getClass() + ")");
        System.out.println("=== DEBUG === sourceAccount.getClient().getId() = " + sourceAccount.getClient().getId() + " (classe: " + sourceAccount.getClient().getId().getClass() + ")");
        System.out.println("=== DEBUG === sourceAccount.getClient().getEmail() = " + sourceAccount.getClient().getEmail());
        System.out.println("=== DEBUG === client.getEmail() = " + client.getEmail());

        if (!sourceAccount.getClient().getId().equals(client.getId())) {
            throw new IllegalArgumentException("Ce compte ne vous appartient pas");
        }

        Account destinationAccount = accountRepository.findByAccountNumber(request.getDestinationAccountNumber())
                .orElseThrow(() -> new IllegalArgumentException("Compte destinataire introuvable"));

        if (sourceAccount.getId().equals(destinationAccount.getId())) {
            throw new IllegalArgumentException("Impossible de faire un virement vers le même compte");
        }

        if (sourceAccount.getBalance().compareTo(request.getAmount()) < 0) {
            throw new IllegalArgumentException("Solde insuffisant");
        }

        String otpCode = transferOtpService.generateOtp();

        Transfer transfer = Transfer.builder()
                .sourceAccount(sourceAccount)
                .destinationAccountNumber(request.getDestinationAccountNumber())
                .amount(request.getAmount())
                .label(request.getLabel())
                .status(TransferStatus.PENDING)
                .otpCode(otpCode)
                .otpExpiration(LocalDateTime.now().plusMinutes(transferOtpService.getExpirationMinutes()))
                .createdAt(LocalDateTime.now())
                .build();

        Transfer savedTransfer = transferRepository.save(transfer);

        transferOtpService.sendTransferOtpEmail(
                client.getEmail(),
                otpCode,
                request.getAmount(),
                request.getDestinationAccountNumber()
        );

        return new InitiateTransferResponse(
                savedTransfer.getId(),
                "Un code de confirmation a été envoyé par email"
        );
    }

    @Transactional
    public ConfirmTransferResponse confirmTransfer(String email, ConfirmTransferRequest request) {

        Client client = clientRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Client introuvable"));

        Transfer transfer = transferRepository.findById(request.getTransferId())
                .orElseThrow(() -> new IllegalArgumentException("Virement introuvable"));

        if (!transfer.getSourceAccount().getClient().getId().equals(client.getId())) {
            throw new IllegalArgumentException("Ce virement ne vous appartient pas");
        }

        if (transfer.getStatus() != TransferStatus.PENDING) {
            throw new IllegalArgumentException("Ce virement a déjà été traité");
        }

        if (LocalDateTime.now().isAfter(transfer.getOtpExpiration())) {
            transfer.setStatus(TransferStatus.EXPIRED);
            transferRepository.save(transfer);
            throw new IllegalArgumentException("Le code a expiré, veuillez réinitier le virement");
        }

        if (!transfer.getOtpCode().equals(request.getOtpCode())) {
            throw new IllegalArgumentException("Code incorrect");
        }

        Account sourceAccount = accountRepository.findById(transfer.getSourceAccount().getId())
                .orElseThrow(() -> new IllegalArgumentException("Compte source introuvable"));

        Account destinationAccount = accountRepository.findByAccountNumber(transfer.getDestinationAccountNumber())
                .orElseThrow(() -> new IllegalArgumentException("Compte destinataire introuvable"));

        if (sourceAccount.getBalance().compareTo(transfer.getAmount()) < 0) {
            transfer.setStatus(TransferStatus.FAILED);
            transferRepository.save(transfer);
            throw new IllegalArgumentException("Solde insuffisant au moment de la confirmation");
        }

        BigDecimal newSourceBalance = sourceAccount.getBalance().subtract(transfer.getAmount());
        BigDecimal newDestinationBalance = destinationAccount.getBalance().add(transfer.getAmount());

        sourceAccount.setBalance(newSourceBalance);
        destinationAccount.setBalance(newDestinationBalance);

        accountRepository.save(sourceAccount);
        accountRepository.save(destinationAccount);

        Transaction debitTransaction = Transaction.builder()
                .type(TransactionType.VIREMENT_SORTANT)
                .amount(transfer.getAmount())
                .balanceAfter(newSourceBalance)
                .date(LocalDateTime.now())
                .account(sourceAccount)
                .build();

        Transaction creditTransaction = Transaction.builder()
                .type(TransactionType.VIREMENT_ENTRANT)
                .amount(transfer.getAmount())
                .balanceAfter(newDestinationBalance)
                .date(LocalDateTime.now())
                .account(destinationAccount)
                .build();

        transactionRepository.save(debitTransaction);
        transactionRepository.save(creditTransaction);

        transactionAnomalyService.checkOutgoingTransfer(sourceAccount, transfer.getAmount());

        transfer.setStatus(TransferStatus.CONFIRMED);
        transfer.setConfirmedAt(LocalDateTime.now());
        transfer.setOtpCode(null);
        transferRepository.save(transfer);

        return new ConfirmTransferResponse(
                transfer.getId(),
                TransferStatus.CONFIRMED,
                newSourceBalance,
                "Virement effectué avec succès"
        );
    }
}