package tn.bank.authservice.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import tn.bank.authservice.domain.AdminPermission;
import tn.bank.authservice.domain.AuditLog;
import tn.bank.authservice.domain.Role;
import tn.bank.authservice.domain.User;
import tn.bank.authservice.infrastructure.AuditLogRepository;
import tn.bank.authservice.infrastructure.OtpService;
import tn.bank.authservice.infrastructure.UserEventPublisher;
import tn.bank.authservice.infrastructure.UserRepository;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitaires de AdminService : dependances (repositories, mail, rabbitmq) mockees,
 * on verifie uniquement la logique metier (creation de client, promotion/retrogradation
 * d'un admin et ses effets sur les permissions granulaires).
 */
@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private OtpService otpService;

    @Mock
    private UserEventPublisher userEventPublisher;

    @InjectMocks
    private AdminService adminService;

    @Test
    void createClient_shouldThrow_whenEmailAlreadyExists() {
        when(userRepository.existsByEmail("existing@bank.tn")).thenReturn(true);

        AdminCreateClientRequest request = new AdminCreateClientRequest("Eya", "Tlig", "existing@bank.tn");

        assertThatThrownBy(() -> adminService.createClient(request, "admin@bank.tn"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("existe déjà");

        verify(userRepository, never()).save(any());
        verify(userEventPublisher, never()).publishUserCreated(any());
    }

    @Test
    void createClient_shouldCreateClient_publishEvent_andSendWelcomeEmail() {
        AdminCreateClientRequest request = new AdminCreateClientRequest("Eya", "Tlig", "eya@bank.tn");

        when(userRepository.existsByEmail("eya@bank.tn")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });

        AdminUserResponse response = adminService.createClient(request, "admin@bank.tn");

        assertThat(response.getEmail()).isEqualTo("eya@bank.tn");
        assertThat(response.getRole()).isEqualTo(Role.CLIENT);
        assertThat(response.isEnabled()).isTrue();

        // Le compte client doit declencher la creation automatique du compte bancaire
        // (evenement RabbitMQ consomme par account-service) et un email avec le mot de passe temporaire
        verify(userEventPublisher).publishUserCreated(any(UserCreatedEvent.class));
        verify(otpService).sendAccountCreatedEmail(eq("eya@bank.tn"), eq("Eya"), anyString());
        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    void createEmployee_shouldNotPublishUserCreatedEvent() {
        AdminCreateEmployeeRequest request = new AdminCreateEmployeeRequest(
                "Sami", "Ben Ali", "sami@bank.tn", Set.of(AdminPermission.CREDITS_VIEW)
        );

        when(userRepository.existsByEmail("sami@bank.tn")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(5L);
            return user;
        });

        AdminUserResponse response = adminService.createEmployee(request, "admin@bank.tn");

        assertThat(response.getRole()).isEqualTo(Role.ADMIN);
        assertThat(response.getPermissions()).containsExactly(AdminPermission.CREDITS_VIEW);

        // Contrairement a un client, un employe n'a pas de compte bancaire a creer
        verify(userEventPublisher, never()).publishUserCreated(any());
        verify(otpService).sendEmployeeAccountCreatedEmail(eq("sami@bank.tn"), eq("Sami"), anyString());
    }

    @Test
    void updateUserRole_shouldGrantAllPermissions_whenPromotingClientToAdmin() {
        User client = User.builder()
                .id(2L).email("client@bank.tn").firstName("Sami").lastName("Ben Ali")
                .role(Role.CLIENT).enabled(true).permissions(new HashSet<>())
                .build();

        when(userRepository.findById(2L)).thenReturn(Optional.of(client));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateUserRoleRequest request = new UpdateUserRoleRequest();
        request.setRole(Role.ADMIN);

        AdminUserResponse response = adminService.updateUserRole(2L, request, "admin@bank.tn");

        assertThat(response.getRole()).isEqualTo(Role.ADMIN);
        assertThat(response.getPermissions()).containsExactlyInAnyOrder(AdminPermission.values());
    }

    @Test
    void updateUserRole_shouldClearPermissions_whenDemotingAdminToClient() {
        User admin = User.builder()
                .id(3L).email("admin2@bank.tn").firstName("Nour").lastName("Trabelsi")
                .role(Role.ADMIN).enabled(true)
                .permissions(new HashSet<>(Set.of(AdminPermission.values())))
                .build();

        when(userRepository.findById(3L)).thenReturn(Optional.of(admin));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateUserRoleRequest request = new UpdateUserRoleRequest();
        request.setRole(Role.CLIENT);

        AdminUserResponse response = adminService.updateUserRole(3L, request, "admin@bank.tn");

        assertThat(response.getRole()).isEqualTo(Role.CLIENT);
        assertThat(response.getPermissions()).isEmpty();
    }

    @Test
    void updatePermissions_shouldThrow_whenTargetIsNotAdmin() {
        User client = User.builder()
                .id(4L).email("client2@bank.tn").firstName("Ali").lastName("Kammoun")
                .role(Role.CLIENT).enabled(true).permissions(new HashSet<>())
                .build();

        when(userRepository.findById(4L)).thenReturn(Optional.of(client));

        UpdatePermissionsRequest request = new UpdatePermissionsRequest(Set.of(AdminPermission.USERS_VIEW));

        assertThatThrownBy(() -> adminService.updatePermissions(4L, request, "admin@bank.tn"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("comptes administrateurs");

        verify(userRepository, never()).save(any());
    }

    @Test
    void updatePermissions_shouldUpdate_whenTargetIsAdmin() {
        User admin = User.builder()
                .id(6L).email("admin3@bank.tn").firstName("Rania").lastName("Jlassi")
                .role(Role.ADMIN).enabled(true)
                .permissions(new HashSet<>(Set.of(AdminPermission.values())))
                .build();

        when(userRepository.findById(6L)).thenReturn(Optional.of(admin));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdatePermissionsRequest request = new UpdatePermissionsRequest(Set.of(AdminPermission.CREDITS_VIEW, AdminPermission.CREDITS_VALIDATE));

        AdminUserResponse response = adminService.updatePermissions(6L, request, "admin@bank.tn");

        assertThat(response.getPermissions()).containsExactlyInAnyOrder(AdminPermission.CREDITS_VIEW, AdminPermission.CREDITS_VALIDATE);
    }
}