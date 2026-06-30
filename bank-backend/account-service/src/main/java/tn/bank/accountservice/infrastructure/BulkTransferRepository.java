package tn.bank.accountservice.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.bank.accountservice.domain.BulkTransfer;

public interface BulkTransferRepository extends JpaRepository<BulkTransfer, Long> {
}