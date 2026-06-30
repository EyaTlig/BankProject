package tn.bank.accountservice.application;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InitiateBulkTransferResponse {

    private Long bulkTransferId;
    private int totalItems;
    private String message;
}