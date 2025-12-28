package com.fci.automation.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

@Component
public class SchemaInitializer {

    private final DataSource realDataSource;
    private final DataSource testDataSource;
    private final ResourceLoader resourceLoader;

    @Autowired
    public SchemaInitializer(
            @Qualifier("realDataSource") DataSource realDataSource,
            @Qualifier("testDataSource") DataSource testDataSource,
            ResourceLoader resourceLoader) {
        this.realDataSource = realDataSource;
        this.testDataSource = testDataSource;
        this.resourceLoader = resourceLoader;
    }

    @PostConstruct
    public void init() {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator(
                resourceLoader.getResource("classpath:db-init.sql"));

        // Don't fail if script is missing, but it SHOULD be there
        populator.setContinueOnError(false);
        populator.setIgnoreFailedDrops(true);

        // Execute on REAL
        populator.execute(realDataSource);

        // Execute on TEST
        populator.execute(testDataSource);
    }
}
