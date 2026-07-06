package tn.bank.creditservice.presentation;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.bank.creditservice.application.CreateCreditRequest;
import tn.bank.creditservice.application.CreditRequestResponse;
import tn.bank.creditservice.application.CreditService;
import tn.bank.creditservice.application.SimulationRequest;
import tn.bank.creditservice.application.SimulationResponse;

import java.util.List;

@RestController
@RequestMapping("/api/credits")
@RequiredArgsConstructor
public class CreditController {

    private final CreditService creditService;

    @PostMapping("/simulate")
    public ResponseEntity<SimulationResponse> simulate(
            @Valid @RequestBody SimulationRequest request
    ) {
        return ResponseEntity.ok(creditService.simulate(request));
    }

    @PostMapping("/requests")
    public ResponseEntity<CreditRequestResponse> createRequest(
            Authentication authentication,
            @Valid @RequestBody CreateCreditRequest request
    ) {
        return ResponseEntity.ok(creditService.createCreditRequest(authentication.getName(), request));
    }

    @GetMapping("/requests/my")
    public ResponseEntity<List<CreditRequestResponse>> myRequests(Authentication authentication) {
        return ResponseEntity.ok(creditService.getMyCreditRequests(authentication.getName()));
    }
}