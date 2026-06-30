package tn.bank.accountservice.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.bank.accountservice.domain.Client;
import tn.bank.accountservice.domain.RecurringTransfer;
import tn.bank.accountservice.domain.RecurringTransferStatus;

import java.time.LocalDate;
import java.util.List;

public interface RecurringTransferRepository extends JpaRepository<RecurringTransfer, Long> {

    List<RecurringTransfer> findByStatusAndNextExecutionDate(RecurringTransferStatus status, LocalDate date);

    List<RecurringTransfer> findBySourceAccount_Client(Client client);
}