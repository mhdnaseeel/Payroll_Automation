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

    @Value("${ADMIN_PASSWORD:}")
    private String adminPassword;

    @Value("${USER_USERNAME:user}")
    private String userUsername;

    @Value("${USER_PASSWORD:}")
    private String userPassword;

    @Value("${BILL_USERNAME:bill}")
    private String billUsername;

    @Value("${BILL_PASSWORD:}")
    private String billPassword;

    @Override
    public void run(String... args) throws Exception {
        // --- Production Users (Only created if Password Env Var is set) ---

        if (isValid(adminPassword) && !userRepository.existsByUsername(adminUsername)) {
            User admin = new User();
            admin.setUsername(adminUsername);
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setRole(User.Role.ADMIN);
            userRepository.save(admin);
            logger.info("SEEDER: Created default admin user (from Env Var).");
        } else if (!isValid(adminPassword) && !userRepository.existsByUsername(adminUsername)) {
            logger.warn("SEEDER: Skipped '{}' creation. ADMIN_PASSWORD not set.", adminUsername);
        }

        if (isValid(userPassword) && !userRepository.existsByUsername(userUsername)) {
            User user = new User();
            user.setUsername(userUsername);
            user.setPassword(passwordEncoder.encode(userPassword));
            user.setRole(User.Role.USER);
            userRepository.save(user);
            logger.info("SEEDER: Created default regular user (from Env Var).");
        }

        if (isValid(billPassword) && !userRepository.existsByUsername(billUsername)) {
            User bill = new User();
            bill.setUsername(billUsername);
            bill.setPassword(passwordEncoder.encode(billPassword));
            bill.setRole(User.Role.BILL);
            userRepository.save(bill);
            logger.info("SEEDER: Created default bill user (from Env Var).");
        }

    }

    private boolean isValid(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
