package tn.bank.authservice.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.bank.authservice.domain.AuditLog;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findAllByOrderByCreatedAtDesc();
}