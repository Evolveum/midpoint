/*
 * Copyright (c) 2016-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.conntest.opendj;

import org.testng.annotations.Test;

import java.io.File;


/**
 * Almost same sa TestOpenDj, but there is unsafeNameHint setting and maybe
 * some other possibly risky and alternative connector settings.
 * Also, there is some pollution in that LDAP data, that is filtered out by additionalSearchFilter.
 *
 * @author semancik
 */
public class TestOpenDjUnsafe extends AbstractOpenDjNoiseTest {

    private static final int INITIAL_SYNC_TOKEN = 25;

    @Override
    protected File getResourceFile() {
        return new File(getBaseDir(), "resource-unsafe.xml");
    }

    @Override
    protected int getInitialSyncToken() {
        return INITIAL_SYNC_TOKEN;
    }

    // MID-6515
    /**
     * This addition should be filtered out by additionalSearchFilter.
     */
    @Test
    public void test840SyncAddAccountChaos() throws Exception {
        // See MID-6515
    }
}
