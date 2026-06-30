package tn.bank.accountservice.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.bank.accountservice.domain.Account;
import tn.bank.accountservice.domain.Client;
import tn.bank.accountservice.infrastructure.AccountRepository;
import tn.bank.accountservice.infrastructure.ClientRepository;
import tn.bank.accountservice.infrastructure.TransactionRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final ClientRepository clientRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public List<AccountResponse> getMyAccounts(String email) {

        Client client = clientRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Client introuvable"));

        return accountRepository.findByClient(client).stream()
                .map(this::toAccountResponse)
                .toList();
    }

    public List<TransactionResponse> getAccountHistory(String email, Long accountId) {

        Client client = clientRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Client introuvable"));

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Compte introuvable"));

        if (!account.getClient().getId().equals(client.getId())) {
            throw new IllegalArgumentException("Ce compte ne vous appartient pas");
        }

        return transactionRepository.findByAccountOrderByDateDesc(account).stream()
                .map(this::toTransactionResponse)
                .toList();
    }

    private AccountResponse toAccountResponse(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getType(),
                account.getBalance(),
                account.getCreatedAt()
        );
    }

    private TransactionResponse toTransactionResponse(tn.bank.accountservice.domain.Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getType(),
                transaction.getAmount(),
                transaction.getBalanceAfter(),
                transaction.getDate()
        );
    }
}