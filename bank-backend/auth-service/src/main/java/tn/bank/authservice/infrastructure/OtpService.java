package tn.bank.authservice.infrastructure;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Service
@RequiredArgsConstructor
public class OtpService {

    private final JavaMailSender mailSender;

    @Value("${otp.expiration-minutes}")
    private int expirationMinutes;

    public String generateOtp() {
        SecureRandom random = new SecureRandom();
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }

    public int getExpirationMinutes() {
        return expirationMinutes;
    }

    public void sendOtpEmail(String toEmail, String otpCode) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Votre code de vérification - Bank Platform");
        message.setText(
                "Bonjour,\n\n" +
                "Votre code de vérification est : " + otpCode + "\n\n" +
                "Ce code est valable " + expirationMinutes + " minutes.\n" +
                "Si vous n'avez pas demandé ce code, ignorez cet email.\n\n" +
                "L'équipe Bank Platform"
        );
        mailSender.send(message);
    }
}
