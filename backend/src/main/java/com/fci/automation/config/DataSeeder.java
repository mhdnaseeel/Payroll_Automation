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
        if (isValid(adminPassword) && !userRepository.existsByUsername(adminUsername)) {
            User admin = new User();
            admin.setUsername(adminUsername); // "admin"
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setRole(User.Role.ADMIN);
            userRepository.save(admin);
            logger.info("SEEDER [REAL]: Created admin user '{}'.", adminUsername);
        }

        if (isValid(userPassword) && !userRepository.existsByUsername(userUsername)) {
            User user = new User();
            user.setUsername(userUsername);
            user.setPassword(passwordEncoder.encode(userPassword));
            user.setRole(User.Role.USER);
            userRepository.save(user);
            logger.info("SEEDER [REAL]: Created user '{}'.", userUsername);
        }

        if (isValid(billPassword) && !userRepository.existsByUsername(billUsername)) {
            User bill = new User();
            bill.setUsername(billUsername);
            bill.setPassword(passwordEncoder.encode(billPassword));
            bill.setRole(User.Role.BILL);
            userRepository.save(bill);
            logger.info("SEEDER [REAL]: Created bill user '{}'.", billUsername);
        }
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
