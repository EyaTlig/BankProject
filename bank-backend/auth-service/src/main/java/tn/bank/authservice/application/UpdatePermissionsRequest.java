package tn.bank.authservice.application;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.bank.authservice.domain.AdminPermission;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePermissionsRequest {

    @NotNull(message = "La liste des permissions est obligatoire")
    private Set<AdminPermission> permissions;
}