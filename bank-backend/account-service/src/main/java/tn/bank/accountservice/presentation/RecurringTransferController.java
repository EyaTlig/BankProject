package tn.bank.accountservice.presentation;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.bank.accountservice.application.CreateRecurringTransferRequest;
import tn.bank.accountservice.application.RecurringTransferResponse;
import tn.bank.accountservice.application.RecurringTransferService;
import tn.bank.accountservice.application.UpdateRecurringTransferRequest;

import java.util.List;

@RestController
@RequestMapping("/api/recurring-transfers")
@RequiredArgsConstructor
public class RecurringTransferController {

    private final RecurringTransferService recurringTransferService;

    @PostMapping
    public ResponseEntity<RecurringTransferResponse> createRecurringTransfer(
            Authentication authentication,
            @Valid @RequestBody CreateRecurringTransferRequest request
    ) {
        String email = authentication.getName();
        RecurringTransferResponse response = recurringTransferService.createRecurringTransfer(email, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<RecurringTransferResponse>> getMyRecurringTransfers(Authentication authentication) {
        String email = authentication.getName();
        List<RecurringTransferResponse> response = recurringTransferService.getMyRecurringTransfers(email);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<RecurringTransferResponse> updateRecurringTransfer(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody UpdateRecurringTransferRequest request
    ) {
        String email = authentication.getName();
        RecurringTransferResponse response = recurringTransferService.updateRecurringTransfer(email, id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<RecurringTransferResponse> cancelRecurringTransfer(
            Authentication authentication,
            @PathVariable Long id
    ) {
        String email = authentication.getName();
        RecurringTransferResponse response = recurringTransferService.cancelRecurringTransfer(email, id);
        return ResponseEntity.ok(response);
    }
}