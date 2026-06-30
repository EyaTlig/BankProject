package tn.bank.accountservice.application;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.bank.accountservice.domain.TransferStatus;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmBulkTransferResponse {

    private Long bulkTransferId;
    private TransferStatus status;
    private int successCount;
    private int failedCount;
    private List<BulkTransferItemResult> results;
}