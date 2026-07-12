package tn.bank.accountservice.presentation;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tn.bank.accountservice.application.*;
import tn.bank.accountservice.domain.TransactionType;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminAccountController {

    private final AdminAccountService adminAccountService;

    @GetMapping("/stats")
    public ResponseEntity<AdminStatsResponse> getStats() {
        return ResponseEntity.ok(adminAccountService.getStats());
    }

    @PreAuthorize("hasAuthority('PERM_ACCOUNTS_VIEW')")
    @GetMapping("/clients")
    public ResponseEntity<List<AdminClientResponse>> getAllClients() {
        return ResponseEntity.ok(adminAccountService.getAllClients());
    }

    @PreAuthorize("hasAuthority('PERM_ACCOUNTS_VIEW')")
    @GetMapping("/accounts")
    public ResponseEntity<List<AdminAccountResponse>> getAllAccounts() {
        return ResponseEntity.ok(adminAccountService.getAllAccounts());
    }

    @PreAuthorize("hasAuthority('PERM_ACCOUNTS_MANAGE')")
    @PostMapping("/accounts")
    public ResponseEntity<AdminAccountResponse> createAccount(@RequestBody AdminCreateAccountRequest request) {
        AdminAccountResponse response = adminAccountService.createAccount(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("hasAuthority('PERM_ACCOUNTS_MANAGE')")
    @PostMapping("/accounts/{accountId}/credit")
    public ResponseEntity<AdminAccountResponse> creditAccount(
            @PathVariable Long accountId,
            @RequestBody CreditAccountRequest request
    ) {
        return ResponseEntity.ok(adminAccountService.creditAccount(accountId, request));
    }

    @PreAuthorize("hasAuthority('PERM_ACCOUNTS_VIEW')")
    @GetMapping("/transactions")
    public ResponseEntity<List<AdminTransactionResponse>> getTransactions(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) String accountNumber
    ) {
        return ResponseEntity.ok(adminAccountService.getTransactions(startDate, endDate, type, accountNumber));
    }
}