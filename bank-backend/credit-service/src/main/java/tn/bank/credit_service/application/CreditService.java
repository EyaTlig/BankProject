package tn.bank.creditservice.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.bank.creditservice.domain.CreditRequest;
import tn.bank.creditservice.domain.CreditStatus;
import tn.bank.creditservice.infrastructure.CreditRequestRepository;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CreditService {

    private final CreditRequestRepository creditRequestRepository;

    public SimulationResponse simulate(SimulationRequest request) {

        BigDecimal principal = request.getAmount();
        int n = request.getDurationMonths();

        // Taux mensuel = taux annuel / 12 / 100
        BigDecimal monthlyRate = request.getInterestRate()
                .divide(BigDecimal.valueOf(1200), 10, RoundingMode.HALF_UP);

        BigDecimal monthlyPayment = calculateMonthlyPayment(principal, monthlyRate, n);
        BigDecimal totalCost = monthlyPayment.multiply(BigDecimal.valueOf(n)).setScale(3, RoundingMode.HALF_UP);
        BigDecimal totalInterest = totalCost.subtract(principal).setScale(3, RoundingMode.HALF_UP);

        List<ScheduleRow> schedule = generateSchedule(principal, monthlyRate, monthlyPayment, n);

        return new SimulationResponse(
                monthlyPayment.setScale(3, RoundingMode.HALF_UP),
                totalCost,
                totalInterest,
                schedule
        );
    }

    public CreditRequestResponse createCreditRequest(String email, CreateCreditRequest request) {

        BigDecimal monthlyRate = request.getInterestRate()
                .divide(BigDecimal.valueOf(1200), 10, RoundingMode.HALF_UP);

        BigDecimal monthlyPayment = calculateMonthlyPayment(
                request.getAmount(), monthlyRate, request.getDurationMonths()
        );

        CreditRequest creditRequest = CreditRequest.builder()
                .clientEmail(email)
                .type(request.getType())
                .amount(request.getAmount())
                .durationMonths(request.getDurationMonths())
                .interestRate(request.getInterestRate())
                .monthlyPayment(monthlyPayment.setScale(3, RoundingMode.HALF_UP))
                .purpose(request.getPurpose())
                .status(CreditStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        return toResponse(creditRequestRepository.save(creditRequest));
    }

    public List<CreditRequestResponse> getMyCreditRequests(String email) {
        return creditRequestRepository.findByClientEmailOrderByCreatedAtDesc(email)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public CreditRequestResponse updateStatus(Long id, UpdateCreditStatusRequest request) {
        CreditRequest creditRequest = creditRequestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Demande introuvable"));

        creditRequest.setStatus(request.getStatus());
        creditRequest.setAdminComment(request.getAdminComment());
        creditRequest.setUpdatedAt(LocalDateTime.now());

        return toResponse(creditRequestRepository.save(creditRequest));
    }

    private BigDecimal calculateMonthlyPayment(BigDecimal principal, BigDecimal monthlyRate, int n) {
        if (monthlyRate.compareTo(BigDecimal.ZERO) == 0) {
            return principal.divide(BigDecimal.valueOf(n), 10, RoundingMode.HALF_UP);
        }

        // M = P * r * (1+r)^n / ((1+r)^n - 1)
        BigDecimal onePlusR = BigDecimal.ONE.add(monthlyRate);
        BigDecimal pow = onePlusR.pow(n, new MathContext(10, RoundingMode.HALF_UP));
        BigDecimal numerator = principal.multiply(monthlyRate).multiply(pow);
        BigDecimal denominator = pow.subtract(BigDecimal.ONE);

        return numerator.divide(denominator, 10, RoundingMode.HALF_UP);
    }

    private List<ScheduleRow> generateSchedule(BigDecimal principal, BigDecimal monthlyRate,
                                               BigDecimal monthlyPayment, int n) {
        List<ScheduleRow> schedule = new ArrayList<>();
        BigDecimal balance = principal;

        for (int i = 1; i <= n; i++) {
            BigDecimal interest = balance.multiply(monthlyRate).setScale(3, RoundingMode.HALF_UP);
            BigDecimal principalPart = monthlyPayment.subtract(interest).setScale(3, RoundingMode.HALF_UP);
            balance = balance.subtract(principalPart).setScale(3, RoundingMode.HALF_UP);

            if (i == n) balance = BigDecimal.ZERO;

            schedule.add(new ScheduleRow(
                    i,
                    monthlyPayment.setScale(3, RoundingMode.HALF_UP),
                    principalPart,
                    interest,
                    balance
            ));
        }

        return schedule;
    }

    private CreditRequestResponse toResponse(CreditRequest cr) {
        return new CreditRequestResponse(
                cr.getId(),
                cr.getType(),
                cr.getAmount(),
                cr.getDurationMonths(),
                cr.getInterestRate(),
                cr.getMonthlyPayment(),
                cr.getPurpose(),
                cr.getStatus(),
                cr.getCreatedAt(),
                cr.getUpdatedAt(),
                cr.getAdminComment()
        );
    }
}