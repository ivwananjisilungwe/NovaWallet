package com.novawallet.novawallet_api.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);
    private static final String FROM_DEFAULT = "noreply@novawallet.com";

    private final Optional<JavaMailSender> mailSender;
    private final String fromAddress;

    public MailService(
            Optional<JavaMailSender> mailSender,
            @Value("${app.mail.from:" + FROM_DEFAULT + "}") String fromAddress
    ) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    @Async
    public void sendVerificationEmail(String to, String firstName, String verificationToken) {
        String subject = "NovaWallet — Verify your email address";
        String verificationLink = "/v1/email/verify?token=" + verificationToken;
        String body = String.format("""
                Hi %s,
                
                Welcome to NovaWallet!
                
                Please verify your email address by clicking the link below:
                
                %s
                
                This link expires in 24 hours.
                
                If you did not create an account, please ignore this email.
                
                — NovaWallet Team""", firstName, verificationLink);

        sendEmail(to, subject, body);
    }

    @Async
    public void sendPasswordResetEmail(String to, String firstName, String resetToken) {
        String subject = "NovaWallet — Password Reset Request";
        String resetLink = "/v1/password/reset?token=" + resetToken;
        String body = String.format("""
                Hi %s,
                
                You requested a password reset for your NovaWallet account.
                
                Click the link below to reset your password:
                
                %s
                
                This link expires in 1 hour.
                
                If you did not request a password reset, please ignore this email.
                
                — NovaWallet Team""", firstName, resetLink);

        sendEmail(to, subject, body);
    }

    @Async
    public void sendKycApprovedEmail(String to, String firstName, int tier) {
        String subject = "NovaWallet — KYC Approved!";
        String body = String.format("""
                Hi %s,
                
                Great news! Your KYC verification has been approved.
                
                Your wallet has been created and is ready to use.
                Current KYC Tier: %d
                
                You can now send and receive money through your NovaWallet account.
                
                Thank you for choosing NovaWallet!
                
                — NovaWallet Team""", firstName, tier);

        sendEmail(to, subject, body);
    }

    private void sendEmail(String to, String subject, String body) {
        if (mailSender.isEmpty()) {
            log.info("=== EMAIL (no SMTP configured) ===");
            log.info("To: {}", to);
            log.info("Subject: {}", subject);
            log.info("Body:\n{}", body);
            log.info("=== END EMAIL ===");
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.get().send(message);
            log.info("Email sent to: {} (subject: {})", to, subject);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }
}
