package tn.bank.authservice.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.bank.authservice.domain.LoginAttempt;

import java.time.LocalDateTime;

public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, Long> {
    long countByEmailAndSuccessFalseAndCreatedAtAfter(String email, LocalDateTime after);
}
