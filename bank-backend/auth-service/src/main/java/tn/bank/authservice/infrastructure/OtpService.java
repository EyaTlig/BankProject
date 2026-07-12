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

    public void sendEmployeeAccountCreatedEmail(String toEmail, String firstName, String temporaryPassword) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Votre accès administrateur a été créé - Bank Platform");
        message.setText(
                "Bonjour " + firstName + ",\n\n" +
                        "Un compte collaborateur (accès au panneau d'administration) a été créé pour vous.\n\n" +
                        "Email : " + toEmail + "\n" +
                        "Mot de passe temporaire : " + temporaryPassword + "\n\n" +
                        "Merci de vous connecter puis de modifier ce mot de passe dès que possible.\n" +
                        "Vos droits d'accès ont été configurés par un administrateur et peuvent être ajustés à tout moment.\n\n" +
                        "L'équipe Bank Platform"
        );
        mailSender.send(message);
    }

    public void sendAccountCreatedEmail(String toEmail, String firstName, String temporaryPassword) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Votre compte a été créé - Bank Platform");
        message.setText(
                "Bonjour " + firstName + ",\n\n" +
                        "Un compte client a été créé pour vous par notre équipe.\n\n" +
                        "Email : " + toEmail + "\n" +
                        "Mot de passe temporaire : " + temporaryPassword + "\n\n" +
                        "Merci de vous connecter puis de modifier ce mot de passe dès que possible.\n\n" +
                        "L'équipe Bank Platform"
        );
        mailSender.send(message);
    }

    public void sendPasswordResetEmail(String toEmail, String temporaryPassword) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Réinitialisation de votre mot de passe - Bank Platform");
        message.setText(
                "Bonjour,\n\n" +
                        "Un administrateur a réinitialisé votre mot de passe.\n\n" +
                        "Votre nouveau mot de passe temporaire est : " + temporaryPassword + "\n\n" +
                        "Merci de vous connecter puis de le modifier dès que possible.\n" +
                        "Si vous n'êtes pas à l'origine de cette demande, contactez votre banque immédiatement.\n\n" +
                        "L'équipe Bank Platform"
        );
        mailSender.send(message);
    }
}