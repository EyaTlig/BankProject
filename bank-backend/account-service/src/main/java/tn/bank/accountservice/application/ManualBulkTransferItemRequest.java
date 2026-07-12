package tn.bank.accountservice.application;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ManualBulkTransferItemRequest {

    @NotBlank(message = "Le compte destinataire est obligatoire")
    private String destinationAccountNumber;

    @NotNull(message = "Le montant est obligatoire")
    @DecimalMin(value = "0.001", message = "Le montant doit être supérieur à 0")
    private BigDecimal amount;

    private String label;
}