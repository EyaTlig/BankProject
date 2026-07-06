package tn.bank.authservice.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.bank.authservice.domain.Role;
import tn.bank.authservice.domain.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByRole(Role role);
}
