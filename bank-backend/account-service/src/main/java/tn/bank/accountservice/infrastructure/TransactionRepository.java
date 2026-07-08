package tn.bank.accountservice.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.bank.accountservice.domain.Account;
import tn.bank.accountservice.domain.Transaction;

import java.time.LocalDateTime;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByAccountOrderByDateDesc(Account account);

    List<Transaction> findTop10ByAccountOrderByDateDesc(Account account);

    long countByAccountAndDateAfter(Account account, LocalDateTime after);
}