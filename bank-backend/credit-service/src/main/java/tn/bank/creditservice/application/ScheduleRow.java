package tn.bank.creditservice.application;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class ScheduleRow {
    private int month;
    private BigDecimal monthlyPayment;
    private BigDecimal principal;
    private BigDecimal interest;
    private BigDecimal remainingBalance;
}