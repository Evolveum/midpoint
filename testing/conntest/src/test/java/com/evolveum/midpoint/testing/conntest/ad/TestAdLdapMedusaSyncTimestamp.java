/*
 * Copyright (c) 2015-2016 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.conntest.ad;

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
