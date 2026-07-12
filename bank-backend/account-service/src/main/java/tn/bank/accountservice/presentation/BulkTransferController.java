package tn.bank.accountservice.presentation;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import tn.bank.accountservice.application.BulkTransferService;
import tn.bank.accountservice.application.ConfirmBulkTransferRequest;
import tn.bank.accountservice.application.ConfirmBulkTransferResponse;
import tn.bank.accountservice.application.InitiateBulkTransferResponse;
import tn.bank.accountservice.application.InitiateManualBulkTransferRequest;

@RestController
@RequestMapping("/api/transfers/bulk")
@RequiredArgsConstructor
public class BulkTransferController {

    private final BulkTransferService bulkTransferService;

    @PostMapping("/initiate")
    public ResponseEntity<InitiateBulkTransferResponse> initiateBulkTransfer(
            Authentication authentication,
            @RequestParam("sourceAccountId") Long sourceAccountId,
            @RequestParam("file") MultipartFile csvFile
    ) {
        String email = authentication.getName();
        InitiateBulkTransferResponse response = bulkTransferService.initiateBulkTransfer(email, sourceAccountId, csvFile);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/initiate-manual")
    public ResponseEntity<InitiateBulkTransferResponse> initiateManualBulkTransfer(
            Authentication authentication,
            @Valid @RequestBody InitiateManualBulkTransferRequest request
    ) {
        String email = authentication.getName();
        InitiateBulkTransferResponse response = bulkTransferService.initiateManualBulkTransfer(email, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/confirm")
    public ResponseEntity<ConfirmBulkTransferResponse> confirmBulkTransfer(
            Authentication authentication,
            @Valid @RequestBody ConfirmBulkTransferRequest request
    ) {
        String email = authentication.getName();
        ConfirmBulkTransferResponse response = bulkTransferService.confirmBulkTransfer(email, request);
        return ResponseEntity.ok(response);
    }
}