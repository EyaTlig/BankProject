package tn.bank.accountservice.application;

import lombok.AllArgsConstructor;
import lombok.Data;
import tn.bank.accountservice.domain.AccountType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class AdminAccountResponse {
    private Long id;
    private String accountNumber;
    private AccountType type;
    private BigDecimal balance;
    private LocalDateTime createdAt;
    private Long clientId;
    private String clientEmail;
    private String clientFullName;
}
