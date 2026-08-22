package com.novawallet.novawallet_api.bootstrap;

import com.novawallet.novawallet_api.user.entity.Role;
import com.novawallet.novawallet_api.user.entity.User;
import com.novawallet.novawallet_api.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Seeds the default admin user on startup — dev profile only.
 *
 * <p>Seeding is skipped unless the {@code dev} profile is active: production
 * admins must be provisioned deliberately (e.g. via APP_ADMIN_* environment
 * variables and an explicit bootstrap process), never auto-created at boot.
 *
 * <p>Credentials come exclusively from configuration/environment — they are
 * never hard-coded here and the password is never logged.
 */
@Component
public class AdminDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminDataInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Environment environment;

    @Value("${app.admin.first-name}")
    private String adminFirstName;

    @Value("${app.admin.last-name}")
    private String adminLastName;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Value("${app.admin.phone}")
    private String adminPhone;

    public AdminDataInitializer(UserRepository userRepository,
                                PasswordEncoder passwordEncoder,
                                Environment environment) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.environment = environment;
    }

    @Override
    public void run(String... args) {
        if (!isDevProfileActive()) {
            log.info("Admin seeding skipped — requires the 'dev' profile (active profiles: {})",
                    Arrays.toString(environment.getActiveProfiles()));
            return;
        }

        if (adminAlreadyExists()) {
            log.info("Admin user already exists — skipping creation");
            return;
        }

        User admin = User.builder()
                .firstName(adminFirstName)
                .lastName(adminLastName)
                .email(adminEmail)
                .phone(adminPhone)
                .passwordHash(passwordEncoder.encode(adminPassword))
                .role(Role.ADMIN)
                .emailVerified(true)
                .build();

        userRepository.save(admin);

        // Never log the admin password — it is supplied via configuration only.
        log.info("============================================================");
        log.info("  ADMIN USER SEEDED");
        log.info("  Email: {}", adminEmail);
        log.info("  Role:  ADMIN");
        log.info("  Password: <not logged — set via APP_ADMIN_PASSWORD>");
        log.info("============================================================");
    }

    private boolean isDevProfileActive() {
        return Arrays.asList(environment.getActiveProfiles()).contains("dev");
    }

    private boolean adminAlreadyExists() {
        return userRepository.findByEmail(adminEmail)
                .filter(user -> user.getRole() == Role.ADMIN)
                .isPresent();
    }
}
