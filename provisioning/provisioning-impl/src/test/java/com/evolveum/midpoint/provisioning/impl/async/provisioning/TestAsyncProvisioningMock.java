/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.async.provisioning;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;

import org.testng.annotations.Test;

import java.io.File;

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
}
