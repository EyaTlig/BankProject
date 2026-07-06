package tn.bank.creditservice.application;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import tn.bank.creditservice.domain.CreditType;
import java.math.BigDecimal;

@Data
public class CreateCreditRequest {

    @NotNull
    private CreditType type;

    @NotNull
    @DecimalMin("1000.00")
    private BigDecimal amount;

    @NotNull
    @Min(6)
    private Integer durationMonths;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal interestRate;

    private String purpose;
}