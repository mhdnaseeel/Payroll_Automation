package com.fci.automation.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = "com.fci.automation.repository", transactionManagerRef = "transactionManager")
public class DataSourceConfig {

    @org.springframework.beans.factory.annotation.Value("${spring.datasource.real.jdbc-url}")
    private String realUrl;

    @org.springframework.beans.factory.annotation.Value("${spring.datasource.real.username}")
    private String realUsername;

    @org.springframework.beans.factory.annotation.Value("${spring.datasource.real.password}")
    private String realPassword;

    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.real")
    public DataSource realDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.test")
    public DataSource testDataSource() {
        // SAFETY: Ensure 'test' schema exists using the REAL connection (which defaults
        // to public or no schema)
        // This prevents startup crashes when the Test DataSource tries to connect to a
        // non-existent schema.
        try (java.sql.Connection conn = java.sql.DriverManager.getConnection(realUrl, realUsername, realPassword);
                java.sql.Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE SCHEMA IF NOT EXISTS test");
        } catch (Exception e) {
            // Log warning but proceed; if schema creation fails due to permissions, the app
            // might still work if schema exists.
            System.err.println("DataSourceConfig WARNING: Could not ensure 'test' schema exists: " + e.getMessage());
        }

        // STRICT ISOLATION: Use HikariDataSource to enforce schema at connection level
        HikariDataSource ds = DataSourceBuilder.create()
                .type(HikariDataSource.class)
                .url(realUrl)
                .username(realUsername)
                .password(realPassword)
                .driverClassName("org.postgresql.Driver")
                .build();

        // This forces every connection from this pool to start with this schema
        ds.setSchema("test");

        return ds;
    }

    @Bean
    @Primary
    public DataSource dataSource() {
        RealmRoutingDataSource routingDataSource = new RealmRoutingDataSource();
        Map<Object, Object> targetDataSources = new HashMap<>();
        targetDataSources.put(RealmEnum.REAL, realDataSource());
        targetDataSources.put(RealmEnum.TEST, testDataSource());

        routingDataSource.setTargetDataSources(targetDataSources);

        // REMOVED DEFAULT FALLBACK: If RealmContext is null, this should fail rather
        // than leaking Public data.
        // routingDataSource.setDefaultTargetDataSource(realDataSource());

        return routingDataSource;
    }

}
