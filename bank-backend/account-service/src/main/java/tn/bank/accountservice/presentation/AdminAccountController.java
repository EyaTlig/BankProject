package tn.bank.accountservice.presentation;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.bank.accountservice.application.AdminAccountService;
import tn.bank.accountservice.application.AdminStatsResponse;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminAccountController {

    private final AdminAccountService adminAccountService;

    @GetMapping("/stats")
    public ResponseEntity<AdminStatsResponse> getStats() {
        return ResponseEntity.ok(adminAccountService.getStats());
    }
}
