package tn.bank.authservice.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.bank.authservice.domain.SecurityAlert;
import tn.bank.authservice.domain.SecurityAlertType;

import java.time.LocalDateTime;
import java.util.List;

public interface SecurityAlertRepository extends JpaRepository<SecurityAlert, Long> {

    List<SecurityAlert> findAllByOrderByCreatedAtDesc();

    long countByResolvedFalse();

    boolean existsByRelatedEmailAndTypeAndResolvedFalseAndCreatedAtAfter(
            String relatedEmail, SecurityAlertType type, LocalDateTime after
    );
}
