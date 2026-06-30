package tn.bank.accountservice.application;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InitiateTransferResponse {

    private Long transferId;
    private String message;
}