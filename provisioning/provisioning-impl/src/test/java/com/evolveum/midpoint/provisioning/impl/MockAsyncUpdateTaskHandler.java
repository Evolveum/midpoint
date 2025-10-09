/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.provisioning.impl;

import com.evolveum.midpoint.schema.ResourceOperationCoordinates;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.provisioning.impl.mock.SynchronizationServiceMock;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * Mock of a async update task handler. It simply handles any events by passing
 * them directly to the mock sync service.
 */
@Component
public class MockAsyncUpdateTaskHandler {

    private static final Trace LOGGER = TraceManager.getTrace(MockAsyncUpdateTaskHandler.class);

    @Autowired private ProvisioningService provisioningService;
    @Autowired private SynchronizationServiceMock syncServiceMock;

    public void processUpdates(ResourceOperationCoordinates coords, Task task, OperationResult result)
            throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException,
            ExpressionEvaluationException {

        provisioningService.processAsynchronousUpdates(coords, (event, hResult) -> {
            if (event.isComplete()) {
                syncServiceMock.notifyChange(event.getChangeDescription(), task, hResult);
                event.acknowledge(true, hResult);
                return true;
            } else if (event.isNotApplicable()) {
                hResult.recordNotApplicable();
                event.acknowledge(true, hResult);
                return true;
            } else {
                LOGGER.error("Event is not complete:\n{}", event.debugDump());
                event.acknowledge(false, hResult);
                return false;
            }
        }, task, result);
    }
}
