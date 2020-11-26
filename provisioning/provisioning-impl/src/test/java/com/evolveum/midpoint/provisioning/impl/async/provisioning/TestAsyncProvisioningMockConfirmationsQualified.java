/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.async.provisioning;

import java.io.File;

/**
 * Mock target, using confirmations + qualified names.
 * Note that confirmations are not fully supported yet.
 */
public class TestAsyncProvisioningMockConfirmationsQualified extends TestAsyncProvisioningMock {

    private static final File RESOURCE_MOCK_FILE = new File(TEST_DIR, "resource-async-provisioning-mock-confirmation.xml");

    @Override
    protected File getResourceFile() {
        return RESOURCE_MOCK_FILE;
    }

    @Override
    protected boolean isUsingConfirmations() {
        return true;
    }

    @Override
    protected boolean isQualified() {
        return true;
    }
}
