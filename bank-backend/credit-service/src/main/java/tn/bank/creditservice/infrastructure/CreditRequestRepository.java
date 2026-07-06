package tn.bank.creditservice.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.bank.creditservice.domain.CreditRequest;
import java.util.List;

public interface CreditRequestRepository extends JpaRepository<CreditRequest, Long> {
    List<CreditRequest> findByClientEmailOrderByCreatedAtDesc(String clientEmail);
}