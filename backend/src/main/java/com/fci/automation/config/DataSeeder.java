package com.fci.automation.config;

import com.fci.automation.entity.User;
import com.fci.automation.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataSeeder.class);

    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Value("${ADMIN_USERNAME:admin}")
    private String adminUsername;

    @Value("${ADMIN_PASSWORD:SecureAdminPass2024!}")
    private String adminPassword;

    @Value("${USER_USERNAME:user}")
    private String userUsername;

    @Value("${USER_PASSWORD:SecureUserPass2024!}")
    private String userPassword;

    @Value("${BILL_USERNAME:bill}")
    private String billUsername;

    @Value("${BILL_PASSWORD:SecureBillPass2024!}")
    private String billPassword;

    @Override
    public void run(String... args) throws Exception {
        // Seed REAL Realm
        try {
            com.fci.automation.config.RealmContext.setRealm(com.fci.automation.config.RealmEnum.REAL);
            seedRealRealm();
        } finally {
            com.fci.automation.config.RealmContext.clear();
        }

        // Seed TEST Realm
        try {
            com.fci.automation.config.RealmContext.setRealm(com.fci.automation.config.RealmEnum.TEST);
            seedTestRealm();
        } finally {
            com.fci.automation.config.RealmContext.clear();
        }
    }

    private void seedRealRealm() {
        seedUser(adminUsername, adminPassword, User.Role.ADMIN);
        seedUser(userUsername, userPassword, User.Role.USER);
        seedUser(billUsername, billPassword, User.Role.BILL);
    }

    private void seedUser(String username, String password, User.Role role) {
        if (!isValid(password)) {
            return;
        }

        userRepository.findByUsername(username).ifPresentOrElse(
                user -> {
                    // Update existing user
                    boolean changed = false;
                    if (!passwordEncoder.matches(password, user.getPassword())) {
                        user.setPassword(passwordEncoder.encode(password));
                        changed = true;
                    }
                    if (user.getRole() != role) {
                        user.setRole(role);
                        changed = true;
                    }
                    if (changed) {
                        userRepository.save(user);
                        logger.info("SEEDER [REAL]: Updated user '{}'.", username);
                    }
                },
                () -> {
                    // Create new user
                    User user = new User();
                    user.setUsername(username);
                    user.setPassword(passwordEncoder.encode(password));
                    user.setRole(role);
                    userRepository.save(user);
                    logger.info("SEEDER [REAL]: Created user '{}'.", username);
                });
    }

    private void seedTestRealm() {
        // Hardcoded Test Users for Test Realm as per requirements
        createTestUser("testadmin", User.Role.ADMIN);
        createTestUser("testuser", User.Role.USER);
        createTestUser("testbill", User.Role.BILL);
    }

    private void createTestUser(String username, User.Role role) {
        if (!userRepository.existsByUsername(username)) {
            User user = new User();
            user.setUsername(username);
            user.setPassword(passwordEncoder.encode("test")); // Default test password
            user.setRole(role);
            userRepository.save(user);
            logger.info("SEEDER [TEST]: Created user '{}'.", username);
        }
    }

    private boolean isValid(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
