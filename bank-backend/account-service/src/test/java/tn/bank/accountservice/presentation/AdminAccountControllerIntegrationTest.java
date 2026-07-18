package tn.bank.accountservice.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tn.bank.accountservice.application.AdminCreateAccountRequest;
import tn.bank.accountservice.domain.AccountType;
import tn.bank.accountservice.domain.Client;
import tn.bank.accountservice.infrastructure.ClientRepository;

import javax.crypto.SecretKey;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test d'integration bout-en-bout sur account-service, avec une base H2 en memoire.
 * Comme ce service ne genere jamais lui-meme de JWT (c'est toujours auth-service qui le fait),
 * le token est construit ici directement avec la meme cle partagee, pour simuler fidelement
 * ce que produirait auth-service.
 *
 * Objectif : verifier que la permission ACCOUNTS_MANAGE est bien exigee independamment du
 * service, preuve que le systeme de permissions fonctionne de bout en bout entre microservices.
 *
 * @Transactional : chaque methode de test repart d'une base propre (rollback automatique
 * a la fin de chaque test), pour eviter les conflits entre le client cree dans @BeforeEach
 * d'un test et celui du suivant.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminAccountControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ClientRepository clientRepository;

    @Value("${jwt.secret}")
    private String jwtSecret;

    private Long clientId;

    @BeforeEach
    void setUp() {
        Client client = Client.builder()
                .email("client.integration@bank.tn")
                .firstName("Eya")
                .lastName("Tlig")
                .build();
        clientId = clientRepository.save(client).getId();
    }

    private String tokenWithPermissions(String... permissions) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        return Jwts.builder()
                .subject("admin.integration@bank.tn")
                .claim("role", "ADMIN")
                .claim("permissions", String.join(",", permissions))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3_600_000))
                .signWith(key)
                .compact();
    }

    @Test
    void createAccount_shouldReturn201_whenAdminHasAccountsManagePermission() throws Exception {
        AdminCreateAccountRequest request = new AdminCreateAccountRequest();
        request.setClientId(clientId);
        request.setType(AccountType.COURANT);
        request.setInitialBalance(new BigDecimal("100.000"));

        mockMvc.perform(post("/api/admin/accounts")
                        .header("Authorization", "Bearer " + tokenWithPermissions("ACCOUNTS_MANAGE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("COURANT"))
                .andExpect(jsonPath("$.balance").value(100.000));
    }

    @Test
    void createAccount_shouldReturn403_whenAdminOnlyHasViewPermission() throws Exception {
        AdminCreateAccountRequest request = new AdminCreateAccountRequest();
        request.setClientId(clientId);
        request.setType(AccountType.EPARGNE);
        request.setInitialBalance(BigDecimal.ZERO);

        mockMvc.perform(post("/api/admin/accounts")
                        .header("Authorization", "Bearer " + tokenWithPermissions("ACCOUNTS_VIEW"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createAccount_shouldReturn400_whenClientDoesNotExist() throws Exception {
        AdminCreateAccountRequest request = new AdminCreateAccountRequest();
        request.setClientId(999_999L);
        request.setType(AccountType.COURANT);

        mockMvc.perform(post("/api/admin/accounts")
                        .header("Authorization", "Bearer " + tokenWithPermissions("ACCOUNTS_MANAGE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAllAccounts_shouldReturn200_withAccountsViewPermission() throws Exception {
        mockMvc.perform(get("/api/admin/accounts")
                        .header("Authorization", "Bearer " + tokenWithPermissions("ACCOUNTS_VIEW")))
                .andExpect(status().isOk());
    }

    @Test
    void getStats_shouldBeAccessible_toAnyAdmin_regardlessOfGranularPermissions() throws Exception {
        // Le dashboard general reste accessible a tout admin, meme sans aucune permission fine
        mockMvc.perform(get("/api/admin/stats")
                        .header("Authorization", "Bearer " + tokenWithPermissions()))
                .andExpect(status().isOk());
    }
}