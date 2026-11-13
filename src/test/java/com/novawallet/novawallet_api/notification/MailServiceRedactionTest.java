package com.novawallet.novawallet_api.notification;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SEC-03: when SMTP is not configured, MailService must log the redacted
 * body and must never log the raw token.
 */
class MailServiceRedactionTest {

    private MailService mailService;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void setUp() {
        mailService = new MailService(java.util.Optional.empty(), "noreply@novawallet.com");

        Logger logger = (Logger) LoggerFactory.getLogger(MailService.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(Level.INFO);
    }

    @AfterEach
    void tearDown() {
        Logger logger = (Logger) LoggerFactory.getLogger(MailService.class);
        logger.detachAppender(appender);
        appender.stop();
    }

    private String capturedLog() {
        return appender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .reduce("", (a, b) -> a + "\n" + b);
    }

    @Test
    @DisplayName("Verification email body is logged redacted — raw token absent")
    void verificationEmailBodyRedacted() {
        String rawToken = "AbCdEfGh1jKlMnOpQrStUvWxYz1234567890";

        mailService.sendVerificationEmail("user@example.com", "John", rawToken);

        String log = capturedLog();
        assertThat(log).contains("EMAIL (no SMTP configured");
        assertThat(log).doesNotContain(rawToken);
        assertThat(log).contains("[REDACTED]");
    }

    @Test
    @DisplayName("Password reset email body is logged redacted — raw token absent")
    void resetEmailBodyRedacted() {
        String rawToken = "ZyXwVuTsRqPoNmLkJiHgFeDcBa9876543210";

        mailService.sendPasswordResetEmail("user@example.com", "John", rawToken);

        String log = capturedLog();
        assertThat(log).doesNotContain(rawToken);
        assertThat(log).contains("[REDACTED]");
    }

    @Test
    @DisplayName("The recipient and subject are still logged")
    void recipientAndSubjectLogged() {
        mailService.sendVerificationEmail("user@example.com", "John", "AbCdEfGh1jKlMnOpQrStUvWxYz1234567890");

        String log = capturedLog();
        assertThat(log).contains("user@example.com");
        assertThat(log).contains("NovaWallet — Verify your email address");
    }
}
