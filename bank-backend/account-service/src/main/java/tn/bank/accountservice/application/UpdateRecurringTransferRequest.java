package tn.bank.accountservice.application;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.bank.accountservice.domain.RecurringFrequency;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateRecurringTransferRequest {

    @NotBlank(message = "Le numéro de compte destinataire est obligatoire")
    private String destinationAccountNumber;

    @NotNull(message = "Le montant est obligatoire")
    @DecimalMin(value = "0.01", message = "Le montant doit être supérieur à 0")
    private BigDecimal amount;

    private String label;

    @NotNull(message = "La fréquence est obligatoire")
    private RecurringFrequency frequency;

    private LocalDate endDate;
}
