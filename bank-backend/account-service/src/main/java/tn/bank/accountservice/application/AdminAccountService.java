package tn.bank.accountservice.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.bank.accountservice.domain.TransactionType;
import tn.bank.accountservice.infrastructure.AccountRepository;
import tn.bank.accountservice.infrastructure.TransactionRepository;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class AdminAccountService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

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
}
