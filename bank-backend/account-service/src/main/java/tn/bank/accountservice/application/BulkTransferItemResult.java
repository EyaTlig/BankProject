package tn.bank.accountservice.application;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.bank.accountservice.domain.BulkTransferItemStatus;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulkTransferItemResult {

    private String destinationAccountNumber;
    private BigDecimal amount;
    private BulkTransferItemStatus status;
    private String errorMessage;
}