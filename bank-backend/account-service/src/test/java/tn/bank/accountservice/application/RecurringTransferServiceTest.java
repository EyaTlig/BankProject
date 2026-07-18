package tn.bank.accountservice.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.bank.accountservice.domain.Account;
import tn.bank.accountservice.domain.Client;
import tn.bank.accountservice.domain.RecurringFrequency;
import tn.bank.accountservice.domain.RecurringTransfer;
import tn.bank.accountservice.domain.RecurringTransferStatus;
import tn.bank.accountservice.infrastructure.AccountRepository;
import tn.bank.accountservice.infrastructure.ClientRepository;
import tn.bank.accountservice.infrastructure.RecurringTransferRepository;
import tn.bank.accountservice.infrastructure.TransactionRepository;
import tn.bank.accountservice.infrastructure.TransferOtpService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Tests unitaires cibles sur la logique de pause/reprise des virements permanents,
 * en particulier le recalcul de la prochaine echeance lors d'une reprise apres une
 * longue pause (cas limite le plus delicat de cette fonctionnalite).
 */
@ExtendWith(MockitoExtension.class)
class RecurringTransferServiceTest {

    @Mock private ClientRepository clientRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private RecurringTransferRepository recurringTransferRepository;
    @Mock private TransferOtpService transferOtpService;

    @InjectMocks
    private RecurringTransferService recurringTransferService;

    private Client client;
    private Account sourceAccount;

    private void setUpOwner() {
        client = Client.builder().id(10L).email("client@bank.tn").firstName("Eya").lastName("Tlig").build();
        sourceAccount = Account.builder().id(20L).accountNumber("TN591000123456").balance(new BigDecimal("500.000")).client(client).build();
        when(clientRepository.findByEmail("client@bank.tn")).thenReturn(Optional.of(client));
    }

    private RecurringTransfer recurringTransferWith(RecurringTransferStatus status, LocalDate nextExecutionDate, LocalDate endDate) {
        return RecurringTransfer.builder()
                .id(1L)
                .sourceAccount(sourceAccount)
                .destinationAccountNumber("TN591000999999")
                .amount(new BigDecimal("50.000"))
                .frequency(RecurringFrequency.MONTHLY)
                .startDate(LocalDate.now().minusMonths(6))
                .endDate(endDate)
                .nextExecutionDate(nextExecutionDate)
                .status(status)
                .createdAt(LocalDateTime.now().minusMonths(6))
                .build();
    }

    @Test
    void pause_shouldSetStatusToPaused_whenTransferIsActive() {
        setUpOwner();
        RecurringTransfer transfer = recurringTransferWith(RecurringTransferStatus.ACTIVE, LocalDate.now().plusDays(10), null);
        when(recurringTransferRepository.findById(1L)).thenReturn(Optional.of(transfer));
        when(recurringTransferRepository.save(any(RecurringTransfer.class))).thenAnswer(inv -> inv.getArgument(0));

        RecurringTransferResponse response = recurringTransferService.pauseRecurringTransfer("client@bank.tn", 1L);

        assertThat(response.getStatus()).isEqualTo(RecurringTransferStatus.PAUSED);
    }

    @Test
    void pause_shouldThrow_whenTransferIsAlreadyPaused() {
        setUpOwner();
        RecurringTransfer transfer = recurringTransferWith(RecurringTransferStatus.PAUSED, LocalDate.now().plusDays(10), null);
        when(recurringTransferRepository.findById(1L)).thenReturn(Optional.of(transfer));

        assertThatThrownBy(() -> recurringTransferService.pauseRecurringTransfer("client@bank.tn", 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("actif");
    }

    @Test
    void pause_shouldThrow_whenTransferBelongsToAnotherClient() {
        setUpOwner();
        Client otherClient = Client.builder().id(99L).email("other@bank.tn").firstName("Autre").lastName("Client").build();
        Account otherAccount = Account.builder().id(21L).accountNumber("TN591000000001").balance(BigDecimal.TEN).client(otherClient).build();
        RecurringTransfer transfer = RecurringTransfer.builder()
                .id(2L).sourceAccount(otherAccount).destinationAccountNumber("TN591000999999")
                .amount(new BigDecimal("50.000")).frequency(RecurringFrequency.MONTHLY)
                .startDate(LocalDate.now().minusMonths(1)).nextExecutionDate(LocalDate.now().plusDays(5))
                .status(RecurringTransferStatus.ACTIVE).createdAt(LocalDateTime.now())
                .build();
        when(recurringTransferRepository.findById(2L)).thenReturn(Optional.of(transfer));

        assertThatThrownBy(() -> recurringTransferService.pauseRecurringTransfer("client@bank.tn", 2L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("appartient pas");
    }

    @Test
    void resume_shouldThrow_whenTransferIsNotPaused() {
        setUpOwner();
        RecurringTransfer transfer = recurringTransferWith(RecurringTransferStatus.ACTIVE, LocalDate.now().plusDays(10), null);
        when(recurringTransferRepository.findById(1L)).thenReturn(Optional.of(transfer));

        assertThatThrownBy(() -> recurringTransferService.resumeRecurringTransfer("client@bank.tn", 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("pause");
    }

    @Test
    void resume_shouldKeepNextExecutionDate_whenItIsStillInTheFuture() {
        setUpOwner();
        LocalDate futureDate = LocalDate.now().plusDays(15);
        RecurringTransfer transfer = recurringTransferWith(RecurringTransferStatus.PAUSED, futureDate, null);
        when(recurringTransferRepository.findById(1L)).thenReturn(Optional.of(transfer));
        when(recurringTransferRepository.save(any(RecurringTransfer.class))).thenAnswer(inv -> inv.getArgument(0));

        RecurringTransferResponse response = recurringTransferService.resumeRecurringTransfer("client@bank.tn", 1L);

        assertThat(response.getStatus()).isEqualTo(RecurringTransferStatus.ACTIVE);
        assertThat(response.getNextExecutionDate()).isEqualTo(futureDate);
    }

    @Test
    void resume_shouldAdvanceToNextFutureOccurrence_whenPausedLongEnoughThatDateIsInThePast() {
        setUpOwner();
        // Mensuel, mis en pause il y a 3 mois : la prochaine echeance stockee est perimee
        LocalDate staleDate = LocalDate.now().minusMonths(3);
        RecurringTransfer transfer = recurringTransferWith(RecurringTransferStatus.PAUSED, staleDate, null);
        when(recurringTransferRepository.findById(1L)).thenReturn(Optional.of(transfer));
        when(recurringTransferRepository.save(any(RecurringTransfer.class))).thenAnswer(inv -> inv.getArgument(0));

        RecurringTransferResponse response = recurringTransferService.resumeRecurringTransfer("client@bank.tn", 1L);

        assertThat(response.getStatus()).isEqualTo(RecurringTransferStatus.ACTIVE);
        // La reprise ne doit jamais rejouer une echeance passee : la nouvelle date doit etre >= aujourd'hui
        assertThat(response.getNextExecutionDate()).isAfterOrEqualTo(LocalDate.now());
    }

    @Test
    void resume_shouldMarkAsCompleted_whenAdvancingPastTheEndDate() {
        setUpOwner();
        LocalDate staleDate = LocalDate.now().minusMonths(3);
        LocalDate endDate = LocalDate.now().minusMonths(1); // La periode prevue est deja terminee
        RecurringTransfer transfer = recurringTransferWith(RecurringTransferStatus.PAUSED, staleDate, endDate);
        when(recurringTransferRepository.findById(1L)).thenReturn(Optional.of(transfer));
        when(recurringTransferRepository.save(any(RecurringTransfer.class))).thenAnswer(inv -> inv.getArgument(0));

        RecurringTransferResponse response = recurringTransferService.resumeRecurringTransfer("client@bank.tn", 1L);

        assertThat(response.getStatus()).isEqualTo(RecurringTransferStatus.COMPLETED);
    }
}