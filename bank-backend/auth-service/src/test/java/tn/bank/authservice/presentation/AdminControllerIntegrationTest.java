package tn.bank.authservice.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tn.bank.authservice.application.AdminCreateClientRequest;
import tn.bank.authservice.infrastructure.JwtService;
import tn.bank.authservice.infrastructure.UserEventPublisher;

import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test d'integration bout-en-bout : vraies requetes HTTP (MockMvc) traversant le filtre JWT
 * et Spring Security, sur une base H2 en memoire. Les dependances externes (RabbitMQ, SMTP)
 * sont mockees pour ne pas necessiter d'infrastructure reelle pendant les tests.
 *
 * Objectif principal : verifier que le systeme de permissions granulaires (PERM_*) est bien
 * applique de bout en bout, pas seulement en theorie dans le code du controleur.
 *
 * @Transactional : chaque test repart d'une base propre (rollback automatique).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    // On evite tout appel reseau reel vers RabbitMQ/SMTP pendant les tests
    @MockBean
    private UserEventPublisher userEventPublisher;

    @MockBean
    private JavaMailSender javaMailSender;

    private String tokenWithPermissions(String... permissions) {
        return jwtService.generateToken("admin.integration@bank.tn", "ADMIN", Set.of(permissions));
    }

    @Test
    void createClient_shouldReturn201_whenAdminHasUsersManagePermission() throws Exception {
        AdminCreateClientRequest request = new AdminCreateClientRequest("Test", "Client", "new.client@bank.tn");

        mockMvc.perform(post("/api/admin/users")
                        .header("Authorization", "Bearer " + tokenWithPermissions("USERS_MANAGE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("new.client@bank.tn"))
                .andExpect(jsonPath("$.role").value("CLIENT"));
    }

    @Test
    void createClient_shouldReturn403_whenAdminLacksUsersManagePermission() throws Exception {
        // Cet admin a seulement le droit de consultation, pas de gestion :
        // la route doit rester bloquee malgre un role ADMIN valide
        AdminCreateClientRequest request = new AdminCreateClientRequest("Test", "Client2", "another.client@bank.tn");

        mockMvc.perform(post("/api/admin/users")
                        .header("Authorization", "Bearer " + tokenWithPermissions("USERS_VIEW"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createClient_shouldReturn403_whenNoTokenProvided() throws Exception {
        AdminCreateClientRequest request = new AdminCreateClientRequest("Test", "Client3", "third.client@bank.tn");

        mockMvc.perform(post("/api/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createClient_shouldReturn400_whenEmailAlreadyUsed() throws Exception {
        AdminCreateClientRequest request = new AdminCreateClientRequest("Doublon", "Test", "doublon@bank.tn");
        String token = tokenWithPermissions("USERS_MANAGE");

        mockMvc.perform(post("/api/admin/users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Meme email une seconde fois : doit etre rejete proprement, pas planter en 500
        mockMvc.perform(post("/api/admin/users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAllUsers_shouldReturn200_whenAdminHasUsersViewPermission() throws Exception {
        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + tokenWithPermissions("USERS_VIEW")))
                .andExpect(status().isOk());
    }

    @Test
    void getAllUsers_shouldReturn403_whenAdminHasNoPermissionAtAll() throws Exception {
        // Un admin "limite" sans aucune permission : connecte, mais aveugle sur ce module
        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + tokenWithPermissions()))
                .andExpect(status().isForbidden());
    }
}