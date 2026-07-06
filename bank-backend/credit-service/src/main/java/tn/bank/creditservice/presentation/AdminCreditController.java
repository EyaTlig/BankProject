package tn.bank.creditservice.presentation;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.bank.creditservice.application.CreditRequestResponse;
import tn.bank.creditservice.application.CreditService;
import tn.bank.creditservice.application.UpdateCreditStatusRequest;

import java.util.List;

@RestController
@RequestMapping("/api/admin/credits")
@RequiredArgsConstructor
public class AdminCreditController {

    private final CreditService creditService;

    @GetMapping("/requests")
    public ResponseEntity<List<CreditRequestResponse>> getAllRequests() {
        return ResponseEntity.ok(creditService.getAllCreditRequests());
    }

    @PatchMapping("/requests/{id}/status")
    public ResponseEntity<CreditRequestResponse> updateStatus(
            @PathVariable Long id,
            @RequestBody UpdateCreditStatusRequest request
    ) {
        return ResponseEntity.ok(creditService.updateStatus(id, request));
    }
}
