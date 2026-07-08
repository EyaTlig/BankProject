package tn.bank.authservice.application;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class SecurityAlertResponse {
    private Long id;
    private String type;
    private String severity;
    private String message;
    private String relatedEmail;
    private boolean resolved;
    private LocalDateTime createdAt;
}
