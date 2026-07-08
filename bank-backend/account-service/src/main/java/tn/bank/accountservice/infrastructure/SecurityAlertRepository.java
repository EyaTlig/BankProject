package tn.bank.accountservice.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.bank.accountservice.domain.SecurityAlert;
import tn.bank.accountservice.domain.SecurityAlertType;

import java.time.LocalDateTime;
import java.util.List;

public interface SecurityAlertRepository extends JpaRepository<SecurityAlert, Long> {

    List<SecurityAlert> findAllByOrderByCreatedAtDesc();

    long countByResolvedFalse();

    boolean existsByRelatedAccountNumberAndTypeAndResolvedFalseAndCreatedAtAfter(
            String relatedAccountNumber, SecurityAlertType type, LocalDateTime after
    );
}
