package tn.bank.accountservice.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.bank.accountservice.domain.Account;
import tn.bank.accountservice.domain.Client;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    List<Account> findByClient(Client client);

    Optional<Account> findByAccountNumber(String accountNumber);
}