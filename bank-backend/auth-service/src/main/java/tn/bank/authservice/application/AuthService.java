package tn.bank.authservice.application;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import tn.bank.authservice.domain.Role;
import tn.bank.authservice.domain.User;
import tn.bank.authservice.infrastructure.JwtService;
import tn.bank.authservice.infrastructure.OtpService;
import tn.bank.authservice.infrastructure.UserEventPublisher;
import tn.bank.authservice.infrastructure.UserRepository;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final OtpService otpService;
    private final UserEventPublisher userEventPublisher;
    private final SecurityMonitoringService securityMonitoringService;

    public RegisterResponse register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Un compte existe déjà avec cet email");
        }

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.CLIENT)
                .enabled(true)
                .build();

        User savedUser = userRepository.save(user);

        userEventPublisher.publishUserCreated(
                new UserCreatedEvent(
                        savedUser.getEmail(),
                        savedUser.getFirstName(),
                        savedUser.getLastName()
                )
        );

        return new RegisterResponse(
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getFirstName(),
                savedUser.getLastName(),
                "Compte créé avec succès"
        );
    }

    public LoginResponse login(LoginRequest request) {

        String ipAddress = currentClientIp();

        User user = userRepository.findByEmail(request.getEmail())
                .orElse(null);

        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            securityMonitoringService.recordLoginAttempt(request.getEmail(), false, ipAddress);
            throw new IllegalArgumentException("Email ou mot de passe incorrect");
        }

        securityMonitoringService.recordLoginAttempt(request.getEmail(), true, ipAddress);

        String otpCode = otpService.generateOtp();
        user.setOtpCode(otpCode);
        user.setOtpExpiration(LocalDateTime.now().plusMinutes(otpService.getExpirationMinutes()));
        userRepository.save(user);

        otpService.sendOtpEmail(user.getEmail(), otpCode);

        return new LoginResponse(
                null,
                user.getEmail(),
                "Un code de vérification a été envoyé par email",
                true
        );
    }

    private String currentClientIp() {
        try {
            HttpServletRequest httpRequest = ((ServletRequestAttributes)
                    RequestContextHolder.currentRequestAttributes()).getRequest();
            String forwardedFor = httpRequest.getHeader("X-Forwarded-For");
            if (forwardedFor != null && !forwardedFor.isBlank()) {
                return forwardedFor.split(",")[0].trim();
            }
            return httpRequest.getRemoteAddr();
        } catch (Exception e) {
            return "unknown";
        }
    }

    public LoginResponse verifyOtp(VerifyOtpRequest request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable"));

        if (user.getOtpCode() == null || user.getOtpExpiration() == null) {
            throw new IllegalArgumentException("Aucun code en attente, veuillez vous reconnecter");
        }

        if (LocalDateTime.now().isAfter(user.getOtpExpiration())) {
            throw new IllegalArgumentException("Le code a expiré, veuillez vous reconnecter");
        }

        if (!user.getOtpCode().equals(request.getOtpCode())) {
            throw new IllegalArgumentException("Code incorrect");
        }

        user.setOtpCode(null);
        user.setOtpExpiration(null);
        userRepository.save(user);

        java.util.Set<String> permissionNames = user.getPermissions().stream()
                .map(Enum::name)
                .collect(java.util.stream.Collectors.toSet());
        String token = jwtService.generateToken(user.getEmail(), user.getRole().name(), permissionNames);

        return new LoginResponse(
                token,
                user.getEmail(),
                "Connexion réussie",
                false
        );
    }
}