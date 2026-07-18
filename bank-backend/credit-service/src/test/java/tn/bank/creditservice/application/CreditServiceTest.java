package tn.bank.creditservice.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.bank.creditservice.domain.CreditRequest;
import tn.bank.creditservice.domain.CreditStatus;
import tn.bank.creditservice.domain.CreditType;
import tn.bank.creditservice.infrastructure.CreditRequestRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Tests unitaires de CreditService : la simulation est une pure fonction mathematique
 * (echeancier d'amortissement), ideale pour un test unitaire sans mock. La mise a jour
 * de statut est testee avec le repository mocke.
 */
@ExtendWith(MockitoExtension.class)
class CreditServiceTest {

    @Mock
    private CreditRequestRepository creditRequestRepository;

    @InjectMocks
    private CreditService creditService;

    @Test
    void simulate_withZeroInterestRate_shouldSplitPrincipalEquallyOverDuration() {
        SimulationRequest request = new SimulationRequest();
        request.setAmount(new BigDecimal("12000"));
        request.setDurationMonths(12);
        request.setInterestRate(new BigDecimal("0.01")); // borne minimale autorisee, quasi nulle

        SimulationResponse response = creditService.simulate(request);

        // Avec un taux quasi nul, la mensualite doit etre tres proche de 12000/12 = 1000
        assertThat(response.getMonthlyPayment()).isCloseTo(new BigDecimal("1000.000"), within(new BigDecimal("1.0")));
        assertThat(response.getSchedule()).hasSize(12);
    }

    @Test
    void simulate_shouldProduceScheduleThatFullyAmortizesToZero() {
        SimulationRequest request = new SimulationRequest();
        request.setAmount(new BigDecimal("12000"));
        request.setDurationMonths(12);
        request.setInterestRate(new BigDecimal("12")); // 12% annuel

        SimulationResponse response = creditService.simulate(request);

        assertThat(response.getSchedule()).hasSize(12);
        // La derniere ligne de l'echeancier doit toujours ramener le solde restant a zero,
        // meme apres les arrondis successifs sur chaque ligne
        assertThat(response.getSchedule().get(11).getRemainingBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        // Le cout total doit forcement etre superieur au capital emprunte (interets positifs)
        assertThat(response.getTotalCost()).isGreaterThan(request.getAmount());
        assertThat(response.getTotalInterest()).isGreaterThan(BigDecimal.ZERO);
        // Coherence interne : mensualite (arrondie pour affichage) * duree ~= cout total
        // (le service calcule totalCost a partir de la mensualite NON arrondie en interne,
        // d'ou un ecart d'arrondi de quelques millimes tout a fait normal)
        BigDecimal expectedTotal = response.getMonthlyPayment().multiply(BigDecimal.valueOf(12));
        assertThat(response.getTotalCost()).isCloseTo(expectedTotal, within(new BigDecimal("0.05")));
    }

    @Test
    void updateStatus_shouldThrow_whenRequestDoesNotExist() {
        when(creditRequestRepository.findById(404L)).thenReturn(Optional.empty());

        UpdateCreditStatusRequest request = new UpdateCreditStatusRequest();
        request.setStatus(CreditStatus.APPROVED);

        assertThatThrownBy(() -> creditService.updateStatus(404L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("introuvable");
    }

    @Test
    void updateStatus_shouldApplyNewStatusAndComment() {
        CreditRequest existing = CreditRequest.builder()
                .id(1L)
                .clientEmail("client@bank.tn")
                .type(CreditType.PERSONNEL)
                .amount(new BigDecimal("5000"))
                .durationMonths(24)
                .interestRate(new BigDecimal("7"))
                .monthlyPayment(new BigDecimal("225.500"))
                .status(CreditStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        when(creditRequestRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(creditRequestRepository.save(any(CreditRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateCreditStatusRequest request = new UpdateCreditStatusRequest();
        request.setStatus(CreditStatus.REJECTED);
        request.setAdminComment("Revenus insuffisants");

        CreditRequestResponse response = creditService.updateStatus(1L, request);

        assertThat(response.getStatus()).isEqualTo(CreditStatus.REJECTED);
        assertThat(response.getAdminComment()).isEqualTo("Revenus insuffisants");
    }
}