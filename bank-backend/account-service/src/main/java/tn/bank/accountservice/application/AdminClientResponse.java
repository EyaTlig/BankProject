package tn.bank.accountservice.application;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class AdminClientResponse {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private int accountCount;
    private BigDecimal totalBalance;
}
