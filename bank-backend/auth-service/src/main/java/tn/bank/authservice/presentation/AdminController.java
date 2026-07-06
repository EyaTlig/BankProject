package tn.bank.authservice.presentation;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.bank.authservice.application.AdminService;
import tn.bank.authservice.application.AdminUserResponse;
import tn.bank.authservice.application.UpdateUserRoleRequest;
import tn.bank.authservice.application.UpdateUserStatusRequest;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping
    public ResponseEntity<List<AdminUserResponse>> getAllUsers() {
        return ResponseEntity.ok(adminService.getAllUsers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminUserResponse> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.getUserById(id));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<AdminUserResponse> updateStatus(
            @PathVariable Long id,
            @RequestBody UpdateUserStatusRequest request
    ) {
        return ResponseEntity.ok(adminService.updateUserStatus(id, request));
    }

    @PatchMapping("/{id}/role")
    public ResponseEntity<AdminUserResponse> updateRole(
            @PathVariable Long id,
            @RequestBody UpdateUserRoleRequest request
    ) {
        return ResponseEntity.ok(adminService.updateUserRole(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        adminService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@PathVariable Long id) {
        String tempPassword = adminService.resetPassword(id);
        return ResponseEntity.ok(Map.of(
                "message", "Mot de passe reinitialise avec succes",
                "temporaryPassword", tempPassword
        ));
    }
}
