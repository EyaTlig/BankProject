package tn.bank.accountservice.application;

import lombok.Data;
import tn.bank.accountservice.domain.AccountType;

import java.math.BigDecimal;

@Data
public class AdminCreateAccountRequest {
    private Long clientId;
    private AccountType type;
    private BigDecimal initialBalance;
}
