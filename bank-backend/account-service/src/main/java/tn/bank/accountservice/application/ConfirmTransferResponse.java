package tn.bank.accountservice.application;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.bank.accountservice.domain.TransferStatus;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmTransferResponse {

    private Long transferId;
    private TransferStatus status;
    private BigDecimal newBalance;
    private String message;
}