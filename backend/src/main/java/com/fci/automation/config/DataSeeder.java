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

    @Value("${ADMIN_PASSWORD:admin123}")
    private String adminPassword;

    @Value("${USER_PASSWORD:user123}")
    private String userPassword;

    @Value("${BILL_PASSWORD:bill123}")
    private String billPassword;

    @Override
    public void run(String... args) throws Exception {
        if (!userRepository.existsByUsername("admin")) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setRole(User.Role.ADMIN);
            userRepository.save(admin);
            userRepository.save(admin);
            logger.info("SEEDER: Created default admin user.");
        }

        if (!userRepository.existsByUsername("user")) {
            User user = new User();
            user.setUsername("user");
            user.setPassword(passwordEncoder.encode(userPassword));
            user.setRole(User.Role.USER);
            userRepository.save(user);
            userRepository.save(user);
            logger.info("SEEDER: Created default regular user.");
        }

        if (!userRepository.existsByUsername("bill")) {
            User bill = new User();
            bill.setUsername("bill");
            bill.setPassword(passwordEncoder.encode(billPassword));
            bill.setRole(User.Role.BILL);
            userRepository.save(bill);
            userRepository.save(bill);
            logger.info("SEEDER: Created default bill user.");
        }
    }
}
