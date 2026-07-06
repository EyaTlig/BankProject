package tn.bank.authservice.application;

import lombok.Data;

@Data
public class UpdateUserStatusRequest {
    private boolean enabled;
}
