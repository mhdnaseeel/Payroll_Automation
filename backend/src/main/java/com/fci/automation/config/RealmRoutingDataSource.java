package com.fci.automation.config;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

public class RealmRoutingDataSource extends AbstractRoutingDataSource {

    @Override
    protected Object determineCurrentLookupKey() {
        return RealmContext.getRealm();
    }
}
