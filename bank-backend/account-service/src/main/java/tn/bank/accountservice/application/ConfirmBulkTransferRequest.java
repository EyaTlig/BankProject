package tn.bank.accountservice.application;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmBulkTransferRequest {

    @NotNull(message = "L'identifiant du virement groupé est obligatoire")
    private Long bulkTransferId;

    @NotBlank(message = "Le code est obligatoire")
    private String otpCode;
}