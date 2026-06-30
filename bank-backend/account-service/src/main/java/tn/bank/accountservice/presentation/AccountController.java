package tn.bank.accountservice.presentation;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import tn.bank.accountservice.application.AccountResponse;
import tn.bank.accountservice.application.AccountService;
import tn.bank.accountservice.application.TransactionResponse;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @GetMapping
    public ResponseEntity<List<AccountResponse>> getMyAccounts(Authentication authentication) {
        String email = authentication.getName();
        List<AccountResponse> accounts = accountService.getMyAccounts(email);
        return ResponseEntity.ok(accounts);
    }

    @GetMapping("/{accountId}/transactions")
    public ResponseEntity<List<TransactionResponse>> getAccountHistory(
            Authentication authentication,
            @PathVariable Long accountId
    ) {
        String email = authentication.getName();
        List<TransactionResponse> transactions = accountService.getAccountHistory(email, accountId);
        return ResponseEntity.ok(transactions);
    }
}