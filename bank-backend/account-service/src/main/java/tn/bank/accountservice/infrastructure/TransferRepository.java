package tn.bank.accountservice.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.bank.accountservice.domain.Transfer;

public interface TransferRepository extends JpaRepository<Transfer, Long> {
}