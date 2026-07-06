package tn.bank.accountservice.application;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class AdminStatsResponse {
    private long totalAccounts;
    private long totalTransactions;
    private BigDecimal totalBalance;
    private long totalTransfersSortants;
    private long totalTransfersEntrants;
}
