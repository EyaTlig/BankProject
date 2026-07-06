package tn.bank.accountservice.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.bank.accountservice.domain.Account;
import tn.bank.accountservice.domain.Client;
import tn.bank.accountservice.domain.Transaction;
import tn.bank.accountservice.domain.TransactionType;
import tn.bank.accountservice.infrastructure.AccountRepository;
import tn.bank.accountservice.infrastructure.ClientRepository;
import tn.bank.accountservice.infrastructure.TransactionRepository;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminAccountService {

    private static final String CODE_BANQUE_AMEN = "07";
    private static final String CODE_AGENCE = "807";
    private static final int MAX_ACCOUNT_NUMBER_ATTEMPTS = 10;

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final ClientRepository clientRepository;

    public AdminStatsResponse getStats() {
        long totalAccounts = accountRepository.count();
        long totalTransactions = transactionRepository.count();

        BigDecimal totalBalance = accountRepository.findAll().stream()
                .map(a -> a.getBalance())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long sortants = transactionRepository.findAll().stream()
                .filter(t -> t.getType() == TransactionType.VIREMENT_SORTANT)
                .count();

        long entrants = transactionRepository.findAll().stream()
                .filter(t -> t.getType() == TransactionType.VIREMENT_ENTRANT)
                .count();

        return new AdminStatsResponse(totalAccounts, totalTransactions, totalBalance, sortants, entrants);
    }

    public List<AdminClientResponse> getAllClients() {
        return clientRepository.findAll().stream()
                .map(client -> new AdminClientResponse(
                        client.getId(),
                        client.getEmail(),
                        client.getFirstName(),
                        client.getLastName(),
                        client.getAccounts().size(),
                        client.getAccounts().stream()
                                .map(Account::getBalance)
                                .reduce(BigDecimal.ZERO, BigDecimal::add)
                ))
                .toList();
    }

    public List<AdminAccountResponse> getAllAccounts() {
        return accountRepository.findAll().stream()
                .map(this::toAccountResponse)
                .toList();
    }

    @Transactional
    public AdminAccountResponse createAccount(AdminCreateAccountRequest request) {

        if (request.getClientId() == null) {
            throw new IllegalArgumentException("Le client est obligatoire");
        }
        if (request.getType() == null) {
            throw new IllegalArgumentException("Le type de compte est obligatoire");
        }

        Client client = clientRepository.findById(request.getClientId())
                .orElseThrow(() -> new IllegalArgumentException("Client introuvable"));

        BigDecimal initialBalance = request.getInitialBalance() != null
                ? request.getInitialBalance()
                : BigDecimal.ZERO;

        if (initialBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Le solde initial ne peut pas être négatif");
        }

        Account account = Account.builder()
                .accountNumber(generateUniqueAccountNumber())
                .type(request.getType())
                .balance(initialBalance)
                .createdAt(LocalDateTime.now())
                .client(client)
                .build();

        Account savedAccount = accountRepository.save(account);

        return toAccountResponse(savedAccount);
    }
    @Transactional
    public AdminAccountResponse creditAccount(Long accountId, CreditAccountRequest request) {

        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Le montant doit être supérieur à 0");
        }

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Compte introuvable"));

        BigDecimal newBalance = account.getBalance().add(request.getAmount());
        account.setBalance(newBalance);
        Account savedAccount = accountRepository.save(account);

        Transaction transaction = Transaction.builder()
                .type(TransactionType.DEPOT)
                .amount(request.getAmount())
                .balanceAfter(newBalance)
                .date(LocalDateTime.now())
                .account(savedAccount)
                .build();

        transactionRepository.save(transaction);

        return toAccountResponse(savedAccount);
    }
    public List<AdminTransactionResponse> getTransactions(
            LocalDate startDate,
            LocalDate endDate,
            TransactionType type,
            String accountNumber
    ) {
        return accountRepository.findAll().stream()
                .flatMap(account -> transactionRepository.findByAccountOrderByDateDesc(account).stream())
                .filter(t -> startDate == null || !t.getDate().toLocalDate().isBefore(startDate))
                .filter(t -> endDate == null || !t.getDate().toLocalDate().isAfter(endDate))
                .filter(t -> type == null || t.getType() == type)
                .filter(t -> accountNumber == null || accountNumber.isBlank()
                        || t.getAccount().getAccountNumber().equalsIgnoreCase(accountNumber))
                .sorted(Comparator.comparing(Transaction::getDate).reversed())
                .map(this::toTransactionResponse)
                .toList();
    }

    private AdminAccountResponse toAccountResponse(Account account) {
        Client client = account.getClient();
        return new AdminAccountResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getType(),
                account.getBalance(),
                account.getCreatedAt(),
                client.getId(),
                client.getEmail(),
                client.getFirstName() + " " + client.getLastName()
        );
    }

    private AdminTransactionResponse toTransactionResponse(Transaction transaction) {
        Client client = transaction.getAccount().getClient();
        return new AdminTransactionResponse(
                transaction.getId(),
                transaction.getType(),
                transaction.getAmount(),
                transaction.getBalanceAfter(),
                transaction.getDate(),
                transaction.getAccount().getAccountNumber(),
                client.getEmail(),
                client.getFirstName() + " " + client.getLastName()
        );
    }

    private String generateUniqueAccountNumber() {
        String accountNumber;
        int attempts = 0;

        do {
            accountNumber = generateAccountNumber();
            attempts++;
            if (attempts >= MAX_ACCOUNT_NUMBER_ATTEMPTS) {
                throw new IllegalStateException(
                        "Impossible de générer un numéro de compte unique après " + MAX_ACCOUNT_NUMBER_ATTEMPTS + " tentatives"
                );
            }
        } while (accountRepository.findByAccountNumber(accountNumber).isPresent());

        return accountNumber;
    }

    private String generateAccountNumber() {
        String numeroCompte = String.format("%013d", Math.abs(UUID.randomUUID().getMostSignificantBits()) % 10_000_000_000_000L);
        String ribSansCle = CODE_BANQUE_AMEN + CODE_AGENCE + numeroCompte;
        String cleRib = calculerCleRib(ribSansCle);
        return ribSansCle + cleRib;
    }

    private String calculerCleRib(String ribSansCle) {
        long modulo = new BigInteger(ribSansCle).mod(BigInteger.valueOf(97)).longValue();
        long cle = 97 - modulo;
        return String.format("%02d", cle);
    }
}

