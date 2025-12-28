package com.fci.automation.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RealmContext {
    private static final Logger logger = LoggerFactory.getLogger(RealmContext.class);

    private static final ThreadLocal<RealmEnum> currentRealm = new ThreadLocal<>();

    public static void setRealm(RealmEnum realm) {
        logger.debug("Setting Realm Context to: {}", realm);
        currentRealm.set(realm);
    }

    public static RealmEnum getRealm() {
        return currentRealm.get();
    }

    public static void clear() {
        logger.debug("Clearing Realm Context");
        currentRealm.remove();
    }
}
