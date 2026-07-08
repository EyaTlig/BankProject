package tn.bank.authservice.presentation;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.bank.authservice.application.SecurityAlertResponse;
import tn.bank.authservice.application.SecurityMonitoringService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/security-alerts")
@RequiredArgsConstructor
public class SecurityAlertController {

    private final SecurityMonitoringService securityMonitoringService;

    @GetMapping
    public ResponseEntity<List<SecurityAlertResponse>> getAllAlerts() {
        return ResponseEntity.ok(securityMonitoringService.getAllAlerts());
    }

    @GetMapping("/unresolved-count")
    public ResponseEntity<Map<String, Long>> getUnresolvedCount() {
        return ResponseEntity.ok(Map.of("count", securityMonitoringService.getUnresolvedCount()));
    }

    @PatchMapping("/{id}/resolve")
    public ResponseEntity<SecurityAlertResponse> resolveAlert(@PathVariable Long id) {
        return ResponseEntity.ok(securityMonitoringService.resolveAlert(id));
    }
}
