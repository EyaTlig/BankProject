package tn.bank.accountservice.application;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InitiateManualBulkTransferRequest {

    @NotNull(message = "Le compte source est obligatoire")
    private Long sourceAccountId;

    @NotEmpty(message = "Ajoutez au moins un bénéficiaire")
    @Valid
    private List<ManualBulkTransferItemRequest> items;
}