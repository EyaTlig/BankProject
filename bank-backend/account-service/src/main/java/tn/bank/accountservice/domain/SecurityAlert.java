package tn.bank.accountservice.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "security_alerts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SecurityAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SecurityAlertType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SecurityAlertSeverity severity;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "related_email")
    private String relatedEmail;

    @Column(name = "related_account_number")
    private String relatedAccountNumber;

    @Column(nullable = false)
    private boolean resolved;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;
}
