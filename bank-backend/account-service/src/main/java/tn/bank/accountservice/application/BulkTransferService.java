package tn.bank.accountservice.application;

import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tn.bank.accountservice.domain.Account;
import tn.bank.accountservice.domain.BulkTransfer;
import tn.bank.accountservice.domain.BulkTransferItem;
import tn.bank.accountservice.domain.BulkTransferItemStatus;
import tn.bank.accountservice.domain.Client;
import tn.bank.accountservice.domain.Transaction;
import tn.bank.accountservice.domain.TransactionType;
import tn.bank.accountservice.domain.TransferStatus;
import tn.bank.accountservice.infrastructure.AccountRepository;
import tn.bank.accountservice.infrastructure.BulkTransferRepository;
import tn.bank.accountservice.infrastructure.ClientRepository;
import tn.bank.accountservice.infrastructure.TransactionRepository;
import tn.bank.accountservice.infrastructure.TransferOtpService;

import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BulkTransferService {

    private final ClientRepository clientRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final BulkTransferRepository bulkTransferRepository;
    private final TransferOtpService transferOtpService;
    private final TransactionAnomalyService transactionAnomalyService;

    public InitiateBulkTransferResponse initiateBulkTransfer(String email, Long sourceAccountId, MultipartFile csvFile) {

        Client client = clientRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Client introuvable"));

        Account sourceAccount = accountRepository.findById(sourceAccountId)
                .orElseThrow(() -> new IllegalArgumentException("Compte source introuvable"));

        if (!sourceAccount.getClient().getId().equals(client.getId())) {
            throw new IllegalArgumentException("Ce compte ne vous appartient pas");
        }

        List<BulkTransferItem> items = parseCsv(csvFile);

        if (items.isEmpty()) {
            throw new IllegalArgumentException("Le fichier CSV ne contient aucune ligne valide");
        }

        return initiateFromItems(client, sourceAccount, items);
    }

    public InitiateBulkTransferResponse initiateManualBulkTransfer(String email, InitiateManualBulkTransferRequest request) {

        Client client = clientRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Client introuvable"));

        Account sourceAccount = accountRepository.findById(request.getSourceAccountId())
                .orElseThrow(() -> new IllegalArgumentException("Compte source introuvable"));

        if (!sourceAccount.getClient().getId().equals(client.getId())) {
            throw new IllegalArgumentException("Ce compte ne vous appartient pas");
        }

        List<BulkTransferItem> items = request.getItems().stream()
                .map(itemRequest -> BulkTransferItem.builder()
                        .destinationAccountNumber(itemRequest.getDestinationAccountNumber().trim())
                        .amount(itemRequest.getAmount())
                        .label(itemRequest.getLabel())
                        .status(BulkTransferItemStatus.PENDING)
                        .build())
                .toList();

        return initiateFromItems(client, sourceAccount, items);
    }

    private InitiateBulkTransferResponse initiateFromItems(Client client, Account sourceAccount, List<BulkTransferItem> items) {

        BigDecimal totalAmount = items.stream()
                .map(BulkTransferItem::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (sourceAccount.getBalance().compareTo(totalAmount) < 0) {
            throw new IllegalArgumentException("Solde insuffisant pour le total des virements (" + totalAmount + " DT)");
        }

        String otpCode = transferOtpService.generateOtp();

        BulkTransfer bulkTransfer = BulkTransfer.builder()
                .sourceAccount(sourceAccount)
                .status(TransferStatus.PENDING)
                .otpCode(otpCode)
                .otpExpiration(LocalDateTime.now().plusMinutes(transferOtpService.getExpirationMinutes()))
                .createdAt(LocalDateTime.now())
                .build();

        for (BulkTransferItem item : items) {
            item.setBulkTransfer(bulkTransfer);
            bulkTransfer.getItems().add(item);
        }

        BulkTransfer savedBulkTransfer = bulkTransferRepository.save(bulkTransfer);

        transferOtpService.sendTransferOtpEmail(
                client.getEmail(),
                otpCode,
                totalAmount,
                items.size() + " bénéficiaires"
        );

        return new InitiateBulkTransferResponse(
                savedBulkTransfer.getId(),
                items.size(),
                "Un code de confirmation a été envoyé par email pour valider le lot de " + items.size() + " virements"
        );
    }

    @Transactional
    public ConfirmBulkTransferResponse confirmBulkTransfer(String email, ConfirmBulkTransferRequest request) {

        Client client = clientRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Client introuvable"));

        BulkTransfer bulkTransfer = bulkTransferRepository.findById(request.getBulkTransferId())
                .orElseThrow(() -> new IllegalArgumentException("Virement groupé introuvable"));

        if (!bulkTransfer.getSourceAccount().getClient().getId().equals(client.getId())) {
            throw new IllegalArgumentException("Ce virement groupé ne vous appartient pas");
        }

        if (bulkTransfer.getStatus() != TransferStatus.PENDING) {
            throw new IllegalArgumentException("Ce virement groupé a déjà été traité");
        }

        if (LocalDateTime.now().isAfter(bulkTransfer.getOtpExpiration())) {
            bulkTransfer.setStatus(TransferStatus.EXPIRED);
            bulkTransferRepository.save(bulkTransfer);
            throw new IllegalArgumentException("Le code a expiré, veuillez réinitier le virement groupé");
        }

        if (!bulkTransfer.getOtpCode().equals(request.getOtpCode())) {
            throw new IllegalArgumentException("Code incorrect");
        }

        Account sourceAccount = accountRepository.findById(bulkTransfer.getSourceAccount().getId())
                .orElseThrow(() -> new IllegalArgumentException("Compte source introuvable"));

        List<BulkTransferItemResult> results = new ArrayList<>();
        int successCount = 0;
        int failedCount = 0;

        for (BulkTransferItem item : bulkTransfer.getItems()) {

            try {
                Account destinationAccount = accountRepository.findByAccountNumber(item.getDestinationAccountNumber())
                        .orElseThrow(() -> new IllegalArgumentException("Compte destinataire introuvable"));

                if (sourceAccount.getBalance().compareTo(item.getAmount()) < 0) {
                    throw new IllegalArgumentException("Solde insuffisant");
                }

                BigDecimal newSourceBalance = sourceAccount.getBalance().subtract(item.getAmount());
                BigDecimal newDestinationBalance = destinationAccount.getBalance().add(item.getAmount());

                sourceAccount.setBalance(newSourceBalance);
                destinationAccount.setBalance(newDestinationBalance);
                accountRepository.save(sourceAccount);
                accountRepository.save(destinationAccount);

                Transaction debitTransaction = Transaction.builder()
                        .type(TransactionType.VIREMENT_SORTANT)
                        .amount(item.getAmount())
                        .balanceAfter(newSourceBalance)
                        .date(LocalDateTime.now())
                        .account(sourceAccount)
                        .build();

                Transaction creditTransaction = Transaction.builder()
                        .type(TransactionType.VIREMENT_ENTRANT)
                        .amount(item.getAmount())
                        .balanceAfter(newDestinationBalance)
                        .date(LocalDateTime.now())
                        .account(destinationAccount)
                        .build();

                transactionRepository.save(debitTransaction);
                transactionRepository.save(creditTransaction);

                transactionAnomalyService.checkOutgoingTransfer(sourceAccount, item.getAmount());

                item.setStatus(BulkTransferItemStatus.SUCCESS);
                successCount++;

                results.add(new BulkTransferItemResult(
                        item.getDestinationAccountNumber(),
                        item.getAmount(),
                        BulkTransferItemStatus.SUCCESS,
                        null
                ));

            } catch (IllegalArgumentException e) {
                item.setStatus(BulkTransferItemStatus.FAILED);
                item.setErrorMessage(e.getMessage());
                failedCount++;

                results.add(new BulkTransferItemResult(
                        item.getDestinationAccountNumber(),
                        item.getAmount(),
                        BulkTransferItemStatus.FAILED,
                        e.getMessage()
                ));
            }
        }

        bulkTransfer.setStatus(TransferStatus.CONFIRMED);
        bulkTransfer.setConfirmedAt(LocalDateTime.now());
        bulkTransfer.setOtpCode(null);
        bulkTransferRepository.save(bulkTransfer);

        return new ConfirmBulkTransferResponse(
                bulkTransfer.getId(),
                TransferStatus.CONFIRMED,
                successCount,
                failedCount,
                results
        );
    }

    private List<BulkTransferItem> parseCsv(MultipartFile csvFile) {

        List<BulkTransferItem> items = new ArrayList<>();

        try (InputStreamReader reader = new InputStreamReader(csvFile.getInputStream(), StandardCharsets.UTF_8)) {

            CSVFormat format = CSVFormat.DEFAULT.builder()
                    .setHeader("destinationAccountNumber", "amount", "label")
                    .setSkipHeaderRecord(true)
                    .build();

            CSVParser parser = new CSVParser(reader, format);

            for (CSVRecord record : parser) {
                String destinationAccountNumber = record.get("destinationAccountNumber").trim();
                BigDecimal amount = new BigDecimal(record.get("amount").trim());
                String label = record.isMapped("label") ? record.get("label").trim() : null;

                if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new IllegalArgumentException("Montant invalide sur la ligne pour " + destinationAccountNumber);
                }

                items.add(BulkTransferItem.builder()
                        .destinationAccountNumber(destinationAccountNumber)
                        .amount(amount)
                        .label(label)
                        .status(BulkTransferItemStatus.PENDING)
                        .build());
            }

        } catch (IOException e) {
            throw new IllegalArgumentException("Impossible de lire le fichier CSV : " + e.getMessage());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Un montant dans le CSV n'est pas un nombre valide");
        }

        return items;
    }
}