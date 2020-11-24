/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.async.provisioning;

import com.evolveum.midpoint.provisioning.ucf.api.async.AsyncProvisioningRequest;
import com.evolveum.midpoint.provisioning.ucf.api.async.StringAsyncProvisioningRequest;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;

import java.util.LinkedHashMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 *
 */
public abstract class TestAsyncProvisioningMock extends TestAsyncProvisioning {

    protected void testSanity() throws Exception {
        given();
        Task task = getTestTask();

        when();
        OperationResult testResult = provisioningService.testResource(resource.getOid(), task);

        then();
        assertSuccess(testResult);
    }

    @Override
    protected String getRequest() {
        dumpRequests();

        LinkedHashMap<Long, AsyncProvisioningRequest> requestsMap = MockAsyncProvisioningTarget.INSTANCE.getRequestsMap();
        assertThat(requestsMap.size()).as("requests #").isEqualTo(1);
        String text = ((StringAsyncProvisioningRequest) requestsMap.entrySet().iterator().next().getValue()).getStringValue();

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
