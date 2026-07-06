package tn.bank.accountservice.application;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreditAccountRequest {
    private BigDecimal amount;
    private String label;
}