package com.fci.automation.config;

import com.fci.automation.entity.User;
import com.fci.automation.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        if (!userRepository.existsByUsername("admin")) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRole(User.Role.ADMIN);
            userRepository.save(admin);
            System.out.println("SEEDER: Created default admin user.");
        }

        if (!userRepository.existsByUsername("user")) {
            User user = new User();
            user.setUsername("user");
            user.setPassword(passwordEncoder.encode("user123"));
            user.setRole(User.Role.USER);
            userRepository.save(user);
            System.out.println("SEEDER: Created default regular user.");
        }

        if (!userRepository.existsByUsername("bill")) {
            User bill = new User();
            bill.setUsername("bill");
            bill.setPassword(passwordEncoder.encode("bill123"));
            bill.setRole(User.Role.BILL);
            userRepository.save(bill);
            System.out.println("SEEDER: Created default bill user.");
        }
    }
}
