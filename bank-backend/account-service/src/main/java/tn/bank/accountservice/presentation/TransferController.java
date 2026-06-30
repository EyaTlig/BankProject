package tn.bank.accountservice.presentation;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.bank.accountservice.application.ConfirmTransferRequest;
import tn.bank.accountservice.application.ConfirmTransferResponse;
import tn.bank.accountservice.application.InitiateTransferRequest;
import tn.bank.accountservice.application.InitiateTransferResponse;
import tn.bank.accountservice.application.TransferService;

@RestController
@RequestMapping("/api/transfers")
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;

    @PostMapping("/initiate")
    public ResponseEntity<InitiateTransferResponse> initiateTransfer(
            Authentication authentication,
            @Valid @RequestBody InitiateTransferRequest request
    ) {
        String email = authentication.getName();
        InitiateTransferResponse response = transferService.initiateTransfer(email, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/confirm")
    public ResponseEntity<ConfirmTransferResponse> confirmTransfer(
            Authentication authentication,
            @Valid @RequestBody ConfirmTransferRequest request
    ) {
        String email = authentication.getName();
        ConfirmTransferResponse response = transferService.confirmTransfer(email, request);
        return ResponseEntity.ok(response);
    }
}