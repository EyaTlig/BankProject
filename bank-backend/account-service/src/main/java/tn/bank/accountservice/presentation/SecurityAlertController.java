package tn.bank.accountservice.presentation;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.bank.accountservice.application.SecurityAlertResponse;
import tn.bank.accountservice.application.TransactionAnomalyService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/security-alerts")
@RequiredArgsConstructor
public class SecurityAlertController {

    private final TransactionAnomalyService transactionAnomalyService;

    @GetMapping
    public ResponseEntity<List<SecurityAlertResponse>> getAllAlerts() {
        return ResponseEntity.ok(transactionAnomalyService.getAllAlerts());
    }

    @GetMapping("/unresolved-count")
    public ResponseEntity<Map<String, Long>> getUnresolvedCount() {
        return ResponseEntity.ok(Map.of("count", transactionAnomalyService.getUnresolvedCount()));
    }

    @PatchMapping("/{id}/resolve")
    public ResponseEntity<SecurityAlertResponse> resolveAlert(@PathVariable Long id) {
        return ResponseEntity.ok(transactionAnomalyService.resolveAlert(id));
    }
}
