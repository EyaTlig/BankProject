package tn.bank.authservice.presentation;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.bank.authservice.application.AdminCreateClientRequest;
import tn.bank.authservice.application.AdminCreateEmployeeRequest;
import tn.bank.authservice.application.AdminService;
import tn.bank.authservice.application.AdminUserResponse;
import tn.bank.authservice.application.AuditLogResponse;
import tn.bank.authservice.application.UpdatePermissionsRequest;
import tn.bank.authservice.application.UpdateUserRoleRequest;
import tn.bank.authservice.application.UpdateUserStatusRequest;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @PreAuthorize("hasAuthority('PERM_USERS_MANAGE')")
    @PostMapping("/users")
    public ResponseEntity<AdminUserResponse> createClient(
            @Valid @RequestBody AdminCreateClientRequest request,
            Authentication authentication
    ) {
        AdminUserResponse response = adminService.createClient(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("hasAuthority('PERM_USERS_MANAGE')")
    @PostMapping("/employees")
    public ResponseEntity<AdminUserResponse> createEmployee(
            @Valid @RequestBody AdminCreateEmployeeRequest request,
            Authentication authentication
    ) {
        AdminUserResponse response = adminService.createEmployee(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("hasAuthority('PERM_USERS_VIEW')")
    @GetMapping("/users")
    public ResponseEntity<List<AdminUserResponse>> getAllUsers() {
        return ResponseEntity.ok(adminService.getAllUsers());
    }

    @PreAuthorize("hasAuthority('PERM_USERS_VIEW')")
    @GetMapping("/users/{id}")
    public ResponseEntity<AdminUserResponse> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.getUserById(id));
    }

    @PreAuthorize("hasAuthority('PERM_USERS_MANAGE')")
    @PatchMapping("/users/{id}/status")
    public ResponseEntity<AdminUserResponse> updateStatus(
            @PathVariable Long id,
            @RequestBody UpdateUserStatusRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(adminService.updateUserStatus(id, request, authentication.getName()));
    }

    @PreAuthorize("hasAuthority('PERM_USERS_MANAGE')")
    @PatchMapping("/users/{id}/role")
    public ResponseEntity<AdminUserResponse> updateRole(
            @PathVariable Long id,
            @RequestBody UpdateUserRoleRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(adminService.updateUserRole(id, request, authentication.getName()));
    }

    @PreAuthorize("hasAuthority('PERM_USERS_MANAGE')")
    @PatchMapping("/users/{id}/permissions")
    public ResponseEntity<AdminUserResponse> updatePermissions(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePermissionsRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(adminService.updatePermissions(id, request, authentication.getName()));
    }

    @PreAuthorize("hasAuthority('PERM_USERS_MANAGE')")
    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id, Authentication authentication) {
        adminService.deleteUser(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAuthority('PERM_USERS_MANAGE')")
    @PatchMapping("/users/{id}/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@PathVariable Long id, Authentication authentication) {
        adminService.resetPassword(id, authentication.getName());
        return ResponseEntity.ok(Map.of(
                "message", "Un nouveau mot de passe a été envoyé par email à l'utilisateur"
        ));
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<List<AuditLogResponse>> getAuditLogs() {
        return ResponseEntity.ok(adminService.getAuditLogs());
    }
}