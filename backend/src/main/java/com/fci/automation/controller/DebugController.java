package com.fci.automation.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/debug")
public class DebugController {

    @Autowired
    private com.fci.automation.config.DataSeeder dataSeeder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private org.springframework.context.ApplicationContext applicationContext;

    @org.springframework.web.bind.annotation.PostMapping("/reset-db")
    public Map<String, String> resetDatabase() {
        try {
            // 1. Identify Current Realm/Schema
            String currentSchema = jdbcTemplate.queryForObject("SELECT current_schema()", String.class);
            if ("public".equalsIgnoreCase(currentSchema)) {
                return Map.of("status", "error", "message",
                        "Cannot reset 'public' schema via debug endpoint for safety.");
            }

            // 2. Nuke Schema
            jdbcTemplate.execute("DROP SCHEMA IF EXISTS " + currentSchema + " CASCADE");
            jdbcTemplate.execute("CREATE SCHEMA " + currentSchema);

            // 3. Re-Seed
            // We need to re-run the DataSeeder logic for this specific realm
            // Since DataSeeder checks context, we ensure context is set (it should be via
            // Filter)
            // But DataSeeder.run() does EVERYTHING. We need targeted seeding.
            // Actually, simply calling dataSeeder.run() is safe because it checks exists
            // checks.
            // But we specifically need to initialize tables for the Test schema again.

            // Re-invoke DataSeeder
            dataSeeder.run();

            return Map.of("status", "success", "message",
                    "Schema '" + currentSchema + "' reset and re-seeded successfully.");
        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("status", "error", "message", e.getMessage());
        }
    }
}
