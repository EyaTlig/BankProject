package tn.bank.creditservice.application;

import lombok.AllArgsConstructor;
import lombok.Data;
import tn.bank.creditservice.domain.CreditStatus;
import tn.bank.creditservice.domain.CreditType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class CreditRequestResponse {
    private Long id;
    private CreditType type;
    private BigDecimal amount;
    private Integer durationMonths;
    private BigDecimal interestRate;
    private BigDecimal monthlyPayment;
    private String purpose;
    private CreditStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String adminComment;
}