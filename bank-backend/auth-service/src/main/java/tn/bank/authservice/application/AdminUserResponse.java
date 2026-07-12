package tn.bank.authservice.application;

import lombok.AllArgsConstructor;
import lombok.Data;
import tn.bank.authservice.domain.AdminPermission;
import tn.bank.authservice.domain.Role;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@AllArgsConstructor
public class AdminUserResponse {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private Role role;
    private boolean enabled;
    private Set<AdminPermission> permissions;
}