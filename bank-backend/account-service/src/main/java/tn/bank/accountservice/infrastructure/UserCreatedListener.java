package tn.bank.accountservice.infrastructure;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import tn.bank.accountservice.application.UserCreatedEvent;
import tn.bank.accountservice.domain.Account;
import tn.bank.accountservice.domain.AccountType;
import tn.bank.accountservice.domain.Client;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserCreatedListener {

    private final ClientRepository clientRepository;
    private final AccountRepository accountRepository;

    @RabbitListener(queues = "${rabbitmq.queue.user-created}")
    public void handleUserCreated(UserCreatedEvent event) {

        if (clientRepository.existsByEmail(event.getEmail())) {
            log.warn("Client déjà existant pour l'email {}, événement ignoré", event.getEmail());
            return;
        }

        Client client = Client.builder()
                .email(event.getEmail())
                .firstName(event.getFirstName())
                .lastName(event.getLastName())
                .build();

        Client savedClient = clientRepository.save(client);

        Account defaultAccount = Account.builder()
                .accountNumber(generateUniqueAccountNumber())
                .type(AccountType.COURANT)
                .balance(BigDecimal.ZERO)
                .createdAt(LocalDateTime.now())
                .client(savedClient)
                .build();

        accountRepository.save(defaultAccount);

        log.info("Client créé automatiquement pour {} avec un compte courant {}",
                event.getEmail(), defaultAccount.getAccountNumber());
    }

    private String generateUniqueAccountNumber() {
        String accountNumber;
        int attempts = 0;
        final int maxAttempts = 10;

        do {
            accountNumber = generateAccountNumber();
            attempts++;
            if (attempts >= maxAttempts) {
                throw new IllegalStateException(
                        "Impossible de générer un numéro de compte unique après " + maxAttempts + " tentatives"
                );
            }
        } while (accountRepository.findByAccountNumber(accountNumber).isPresent());

        return accountNumber;
    }

    private static final String CODE_BANQUE_AMEN = "07";
    private static final String CODE_AGENCE = "807";

    private String generateAccountNumber() {
        String numeroCompte = String.format("%013d", Math.abs(UUID.randomUUID().getMostSignificantBits()) % 10_000_000_000_000L);
        String ribSansCle = CODE_BANQUE_AMEN + CODE_AGENCE + numeroCompte;
        String cleRib = calculerCleRib(ribSansCle);
        return ribSansCle + cleRib;
    }

    private String calculerCleRib(String ribSansCle) {
        long modulo = new java.math.BigInteger(ribSansCle).mod(java.math.BigInteger.valueOf(97)).longValue();
        long cle = 97 - modulo;
        return String.format("%02d", cle);
    }
}