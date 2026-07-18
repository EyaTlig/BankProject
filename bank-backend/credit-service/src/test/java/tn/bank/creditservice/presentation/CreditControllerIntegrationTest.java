package tn.bank.creditservice.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tn.bank.creditservice.application.SimulationRequest;
import tn.bank.creditservice.application.UpdateCreditStatusRequest;
import tn.bank.creditservice.domain.CreditStatus;

import javax.crypto.SecretKey;
import java.math.BigDecimal;
import java.util.Date;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test d'integration bout-en-bout sur credit-service (le plus simple des 3 services :
 * pas de RabbitMQ ni de SMTP a gerer). Couvre a la fois la route publique de simulation
 * et la gestion par permission des routes d'administration des demandes de credit.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CreditControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${jwt.secret}")
    private String jwtSecret;

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
    void simulate_shouldBeAccessible_withoutAnyAuthentication() throws Exception {
        SimulationRequest request = new SimulationRequest();
        request.setAmount(new BigDecimal("10000"));
        request.setDurationMonths(24);
        request.setInterestRate(new BigDecimal("8"));

        mockMvc.perform(post("/api/credits/simulate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.schedule").isArray())
                .andExpect(jsonPath("$.schedule.length()").value(24));
    }

    @Test
    void simulate_shouldReturn400_whenAmountBelowMinimum() throws Exception {
        SimulationRequest request = new SimulationRequest();
        request.setAmount(new BigDecimal("100")); // sous le minimum de 1000
        request.setDurationMonths(12);
        request.setInterestRate(new BigDecimal("5"));

        mockMvc.perform(post("/api/credits/simulate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAllRequests_shouldReturn403_withoutCreditsViewPermission() throws Exception {
        mockMvc.perform(get("/api/admin/credits/requests")
                        .header("Authorization", "Bearer " + tokenWithPermissions("USERS_VIEW")))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAllRequests_shouldReturn200_withCreditsViewPermission() throws Exception {
        mockMvc.perform(get("/api/admin/credits/requests")
                        .header("Authorization", "Bearer " + tokenWithPermissions("CREDITS_VIEW")))
                .andExpect(status().isOk());
    }

    @Test
    void updateStatus_shouldReturn403_whenAdminCanOnlyViewButNotValidate() throws Exception {
        UpdateCreditStatusRequest request = new UpdateCreditStatusRequest();
        request.setStatus(CreditStatus.APPROVED);

        // Cet admin peut consulter les demandes mais pas les valider :
        // la distinction CREDITS_VIEW / CREDITS_VALIDATE doit etre respectee
        mockMvc.perform(patch("/api/admin/credits/requests/1/status")
                        .header("Authorization", "Bearer " + tokenWithPermissions("CREDITS_VIEW"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}