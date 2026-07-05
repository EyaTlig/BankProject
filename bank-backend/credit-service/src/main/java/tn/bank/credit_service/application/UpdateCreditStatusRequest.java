package tn.bank.creditservice.application;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import tn.bank.creditservice.domain.CreditStatus;

@Data
public class UpdateCreditStatusRequest {

    @NotNull
    private CreditStatus status;

    private String adminComment;
}