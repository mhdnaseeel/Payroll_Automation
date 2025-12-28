package com.fci.automation.config;

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

    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.real")
    public DataSource realDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.test")
    public DataSource testDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean
    @Primary
    public DataSource dataSource() {
        RealmRoutingDataSource routingDataSource = new RealmRoutingDataSource();
        Map<Object, Object> targetDataSources = new HashMap<>();
        targetDataSources.put(RealmEnum.REAL, realDataSource());
        targetDataSources.put(RealmEnum.TEST, testDataSource());

        routingDataSource.setTargetDataSources(targetDataSources);

        // Default to REAL if something goes wrong (Safety net)
        routingDataSource.setDefaultTargetDataSource(realDataSource());

        return routingDataSource;
    }

}
