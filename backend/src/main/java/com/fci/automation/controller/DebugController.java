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
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/db")
    public Map<String, Object> getDbInfo() {
        return Map.of(
                "current_schema", jdbcTemplate.queryForObject("SELECT current_schema()", String.class),
                "search_path", jdbcTemplate.queryForObject("SHOW search_path", String.class),
                "current_user", jdbcTemplate.queryForObject("SELECT current_user", String.class),
                "current_database", jdbcTemplate.queryForObject("SELECT current_database()", String.class),
                "realm_context", String.valueOf(com.fci.automation.config.RealmContext.getRealm()));
    }
}
