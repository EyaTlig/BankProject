package tn.bank.accountservice.application;

import lombok.AllArgsConstructor;
import lombok.Data;
import tn.bank.accountservice.domain.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class AdminTransactionResponse {
    private Long id;
    private TransactionType type;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private LocalDateTime date;
    private String accountNumber;
    private String clientEmail;
    private String clientFullName;
}
