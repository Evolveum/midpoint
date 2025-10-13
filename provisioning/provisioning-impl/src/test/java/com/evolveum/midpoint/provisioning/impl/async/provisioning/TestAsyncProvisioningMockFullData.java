/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.provisioning.impl.async.provisioning;

import java.io.File;

/**
 * Mock target, full data (no deltas).
 */
public class TestAsyncProvisioningMockFullData extends TestAsyncProvisioningMock {

    private static final File RESOURCE_MOCK_FILE = new File(TEST_DIR, "resource-async-provisioning-mock-full-data.xml");

    @Override
    protected File getResourceFile() {
        return RESOURCE_MOCK_FILE;
    }

    @Override
    protected boolean isFullData() {
        return true;
    }
}
