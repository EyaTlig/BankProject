package tn.bank.authservice.application;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.bank.authservice.domain.AdminPermission;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminCreateEmployeeRequest {

    @NotBlank(message = "Le prénom est obligatoire")
    private String firstName;

    @NotBlank(message = "Le nom est obligatoire")
    private String lastName;

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "L'email n'est pas valide")
    private String email;

    /** Permissions accordees des la creation ; peut etre vide (droits ajustables ensuite) */
    private Set<AdminPermission> permissions;
}