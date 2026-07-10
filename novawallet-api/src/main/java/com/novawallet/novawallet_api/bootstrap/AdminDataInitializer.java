package com.novawallet.novawallet_api.bootstrap;

import com.novawallet.novawallet_api.user.entity.Role;
import com.novawallet.novawallet_api.user.entity.User;
import com.novawallet.novawallet_api.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminDataInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.first-name:Admin}")
    private String adminFirstName;

    @Value("${app.admin.last-name:User}")
    private String adminLastName;

    @Value("${app.admin.email:admin@novawallet.com}")
    private String adminEmail;

    @Value("${app.admin.password:Admin@123}")
    private String adminPassword;

    @Value("${app.admin.phone:+260000000000}")
    private String adminPhone;

    public AdminDataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
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

        log.info("============================================================");
        log.info("  DEFAULT ADMIN CREATED");
        log.info("  Email:    {}", adminEmail);
        log.info("  Password: {}", adminPassword);
        log.info("  Role:     ADMIN");
        log.info("============================================================");
    }

    private boolean adminAlreadyExists() {
        return userRepository.findByEmail(adminEmail)
                .filter(user -> user.getRole() == Role.ADMIN)
                .isPresent();
    }
}
