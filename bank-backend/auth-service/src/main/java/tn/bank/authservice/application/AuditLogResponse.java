package tn.bank.authservice.application;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class AuditLogResponse {
    private Long id;
    private String action;
    private String targetEmail;
    private String performedBy;
    private String details;
    private LocalDateTime createdAt;
}