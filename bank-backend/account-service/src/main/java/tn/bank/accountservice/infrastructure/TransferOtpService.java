package tn.bank.accountservice.infrastructure;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.security.SecureRandom;

@Service
@RequiredArgsConstructor
public class TransferOtpService {

    private final JavaMailSender mailSender;

    @Value("${transfer.otp.expiration-minutes}")
    private int expirationMinutes;

    public String generateOtp() {
        SecureRandom random = new SecureRandom();
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }

    public int getExpirationMinutes() {
        return expirationMinutes;
    }

    public void sendTransferOtpEmail(String toEmail, String otpCode, BigDecimal amount, String destinationAccount) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Confirmation de virement - Bank Platform");
        message.setText(
                "Bonjour,\n\n" +
                        "Vous avez initié un virement de " + amount + " DT vers le compte " + destinationAccount + ".\n\n" +
                        "Votre code de confirmation est : " + otpCode + "\n\n" +
                        "Ce code est valable " + expirationMinutes + " minutes.\n" +
                        "Si vous n'êtes pas à l'origine de cette demande, ne partagez pas ce code et contactez votre banque.\n\n" +
                        "L'équipe Bank Platform"
        );
        mailSender.send(message);
    }

    public void sendExecutionReminder(String toEmail, BigDecimal amount, String destinationAccount, java.time.LocalDate executionDate) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Rappel virement permanent - Bank Platform");
        message.setText(
                "Bonjour,\n\n" +
                        "Votre virement permanent de " + amount + " DT vers le compte " + destinationAccount +
                        " sera exécuté demain (" + executionDate + ").\n\n" +
                        "Assurez-vous d'avoir un solde suffisant.\n\n" +
                        "L'équipe Bank Platform"
        );
        mailSender.send(message);
    }

    public void sendExecutionConfirmation(String toEmail, BigDecimal amount, String destinationAccount, BigDecimal newBalance) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Virement permanent exécuté - Bank Platform");
        message.setText(
                "Bonjour,\n\n" +
                        "Votre virement permanent de " + amount + " DT vers le compte " + destinationAccount +
                        " a été exécuté avec succès.\n\n" +
                        "Nouveau solde : " + newBalance + " DT\n\n" +
                        "L'équipe Bank Platform"
        );
        mailSender.send(message);
    }

    public void sendExecutionFailedAlert(String toEmail, BigDecimal amount, String destinationAccount, String reason) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Échec de virement permanent - Bank Platform");
        message.setText(
                "Bonjour,\n\n" +
                        "Votre virement permanent de " + amount + " DT vers le compte " + destinationAccount +
                        " n'a pas pu être exécuté.\n\n" +
                        "Raison : " + reason + "\n\n" +
                        "Veuillez vérifier votre solde. Le virement sera retenté à la prochaine échéance.\n\n" +
                        "L'équipe Bank Platform"
        );
        mailSender.send(message);
    }
}