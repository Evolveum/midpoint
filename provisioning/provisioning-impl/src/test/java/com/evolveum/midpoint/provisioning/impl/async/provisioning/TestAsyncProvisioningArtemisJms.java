/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.provisioning.impl.async.provisioning;

import java.io.File;

/**
 * Uses Artemis broker with midPoint using JMS API to access it.
 */
public class TestAsyncProvisioningArtemisJms extends TestAsyncProvisioningArtemis {

    private static final File RESOURCE_ARTEMIS_JMS_FILE = new File(TEST_DIR, "resource-async-provisioning-artemis-jms.xml");

    @Override
    protected File getResourceFile() {
        return RESOURCE_ARTEMIS_JMS_FILE;
    }
}
