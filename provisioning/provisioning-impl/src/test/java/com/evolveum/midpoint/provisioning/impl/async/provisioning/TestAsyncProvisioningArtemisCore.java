/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.async.provisioning;

import java.io.File;

/**
 * Uses Artemis broker with midPoint using Core API to access it.
 */
public class TestAsyncProvisioningArtemisCore extends TestAsyncProvisioningArtemis {

    private static final File RESOURCE_ARTEMIS_CORE_FILE = new File(TEST_DIR, "resource-async-provisioning-artemis-core.xml");

    @Override
    protected File getResourceFile() {
        return RESOURCE_ARTEMIS_CORE_FILE;
    }
}
