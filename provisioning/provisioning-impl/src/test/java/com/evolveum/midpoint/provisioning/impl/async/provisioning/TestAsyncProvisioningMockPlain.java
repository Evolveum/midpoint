/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.async.provisioning;

import java.io.File;

/**
 * Mock target, plain serialization. (Simplified JSON.)
 */
public class TestAsyncProvisioningMockPlain extends TestAsyncProvisioningMock {

    private static final File RESOURCE_MOCK_FILE = new File(TEST_DIR, "resource-async-provisioning-mock-plain.xml");

    @Override
    protected File getResourceFile() {
        return RESOURCE_MOCK_FILE;
    }
}
