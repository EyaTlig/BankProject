package tn.bank.accountservice.application;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.bank.accountservice.domain.RecurringFrequency;
import tn.bank.accountservice.domain.RecurringTransferStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecurringTransferResponse {

    private Long id;
    private String destinationAccountNumber;
    private BigDecimal amount;
    private String label;
    private RecurringFrequency frequency;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate nextExecutionDate;
    private RecurringTransferStatus status;
}