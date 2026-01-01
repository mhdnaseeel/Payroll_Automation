package com.fci.automation.config;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

public class RealmRoutingDataSource extends AbstractRoutingDataSource {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(RealmRoutingDataSource.class);

    @Override
    protected Object determineCurrentLookupKey() {
        RealmEnum realm = RealmContext.getRealm();
        // logger.info("ROUTING: resolving datasource key -> {}", realm);
        // Commented out to avoid log spam, but useful for deep debug if needed.
        return realm;
    }
}
