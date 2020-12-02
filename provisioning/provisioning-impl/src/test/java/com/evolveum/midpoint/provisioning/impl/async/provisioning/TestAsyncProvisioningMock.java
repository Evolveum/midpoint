/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.async.provisioning;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;

import com.evolveum.midpoint.provisioning.ucf.api.async.AsyncProvisioningRequest;

/**
 * Tests with mock target.
 */
public abstract class TestAsyncProvisioningMock extends TestAsyncProvisioning {

    @Override
    protected String getRequest() {
        LinkedHashMap<Long, AsyncProvisioningRequest> requestsMap = MockAsyncProvisioningTarget.INSTANCE.getRequestsMap();
        assertThat(requestsMap.size()).as("requests #").isEqualTo(1);
        String text = requestsMap.entrySet().iterator().next().getValue().asString();

        clearRequests();
        return text;
    }

    @Override
    protected void dumpRequests() {
        displayMap("Requests", MockAsyncProvisioningTarget.INSTANCE.getRequestsMap());
    }

    @Override
    protected void clearRequests() {
        MockAsyncProvisioningTarget.INSTANCE.clear();
    }
}
