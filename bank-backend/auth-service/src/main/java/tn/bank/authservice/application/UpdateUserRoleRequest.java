package tn.bank.authservice.application;

import lombok.Data;
import tn.bank.authservice.domain.Role;

@Data
public class UpdateUserRoleRequest {
    private Role role;
}
