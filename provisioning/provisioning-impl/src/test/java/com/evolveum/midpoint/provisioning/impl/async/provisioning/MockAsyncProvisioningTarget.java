/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.async.provisioning;

import com.evolveum.midpoint.provisioning.ucf.api.async.AsyncProvisioningRequest;
import com.evolveum.midpoint.provisioning.ucf.api.async.AsyncProvisioningTarget;
import com.evolveum.midpoint.provisioning.ucf.impl.builtin.async.provisioning.AsyncProvisioningConnectorInstance;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AsyncProvisioningTargetType;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.LinkedHashMap;

/**
 * Mock async provisioning target - holding requests in memory.
 */
public class MockAsyncProvisioningTarget implements AsyncProvisioningTarget {

    private static final Trace LOGGER = TraceManager.getTrace(MockAsyncProvisioningTarget.class);

    static final MockAsyncProvisioningTarget INSTANCE = new MockAsyncProvisioningTarget();

    private final LinkedHashMap<Long, AsyncProvisioningRequest> requestsMap = new LinkedHashMap<>();

    @SuppressWarnings("unused")
    public static MockAsyncProvisioningTarget create(AsyncProvisioningTargetType configuration, AsyncProvisioningConnectorInstance connectorInstance) {
        LOGGER.info("create() method called");
        return INSTANCE;
    }

    @Override
    public void connect() {
        LOGGER.info("prepare() method called");
    }

    @Override
    public void disconnect() {
        LOGGER.info("done() method called");
    }

    @Override
    public @NotNull AsyncProvisioningTarget copy() {
        LOGGER.info("copy() method called");
        return INSTANCE;
    }

    @Override
    public void test(OperationResult result) {
        LOGGER.info("test() method called");
    }

    @Override
    public String send(AsyncProvisioningRequest request, OperationResult result) {
        LOGGER.info("send() method called: {}", request);

        long id = System.currentTimeMillis();
        requestsMap.put(id, request);
        return String.valueOf(id);
    }

    public Collection<AsyncProvisioningRequest> getRequests() {
        return requestsMap.values();
    }

    public LinkedHashMap<Long, AsyncProvisioningRequest> getRequestsMap() {
        return requestsMap;
    }

    public void clear() {
        requestsMap.clear();
    }
}
