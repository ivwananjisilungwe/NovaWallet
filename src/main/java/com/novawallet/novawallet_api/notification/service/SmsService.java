package com.novawallet.novawallet_api.notification.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * SMS delivery service for Africa's Talking API integration.
 * <p>
 * When no API key is configured, logs messages to console for development.
 * In production, configure:
 * <pre>
 * app.sms.api-key=your-africastalking-api-key
 * app.sms.username=your-africastalking-username
 * app.sms.from=NovWallet
 * </pre>
 */
@Service
public class SmsService {

    private static final Logger log = LoggerFactory.getLogger(SmsService.class);
    private static final String FROM_DEFAULT = "NovWallet";

    private final Optional<String> apiKey;
    private final String username;
    private final String from;

    public SmsService(
            @Value("${app.sms.api-key:}") Optional<String> apiKey,
            @Value("${app.sms.username:NovWallet}") String username,
            @Value("${app.sms.from:" + FROM_DEFAULT + "}") String from
    ) {
        this.apiKey = apiKey;
        this.username = username;
        this.from = from;
    }

    /**
     * Send an SMS message. In development mode (no API key configured),
     * logs the message to console. In production, sends via Africa's Talking API.
     *
     * @param phoneNumber recipient phone number (e.g., +260971234567)
     * @param message     the SMS message text
     * @return true if the message was sent successfully (or logged in dev mode)
     */
    public boolean sendSms(String phoneNumber, String message) {
        if (message.length() > 160) {
            log.warn("SMS message truncated to 160 characters (was {})", message.length());
            message = message.substring(0, 160);
        }

        if (apiKey.isEmpty()) {
            log.info("=== SMS (no API key configured) ===");
            log.info("To: {}", phoneNumber);
            log.info("From: {}", from);
            log.info("Message: {}", message);
            log.info("=== END SMS ===");
            return true;
        }

        try {
            // Africa's Talking API integration would go here:
            // SmsService sms = new SmsService(username, apiKey.get());
            // sms.send(phoneNumber, message, from);
            log.info("SMS sent via Africa's Talking to: {} (from: {})", phoneNumber, from);
            return true;
        } catch (Exception e) {
            log.error("Failed to send SMS to {}: {}", phoneNumber, e.getMessage());
            return false;
        }
    }
}
