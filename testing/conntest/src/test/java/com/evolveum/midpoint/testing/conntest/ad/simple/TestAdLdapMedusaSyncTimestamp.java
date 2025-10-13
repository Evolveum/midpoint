/*
 * Copyright (c) 2015-2016 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.testing.conntest.ad.simple;

import java.io.File;

/**
 * @author semancik
 */
public class TestAdLdapMedusaSyncTimestamp extends TestAdLdapMedusa {

    @Override
    protected File getResourceFile() {
        return new File(getBaseDir(), "resource-medusa-sync-timestamp.xml");
    }

    @Override
    protected void syncWait() throws InterruptedException {
        // Give us better chance to "catch" the event
        Thread.sleep(5000L);
    };

}
