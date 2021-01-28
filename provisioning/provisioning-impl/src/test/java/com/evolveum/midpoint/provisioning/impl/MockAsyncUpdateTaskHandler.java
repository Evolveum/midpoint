/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.provisioning.impl.mock.SynchronizationServiceMock;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
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

    public void processUpdates(ResourceShadowDiscriminator coords, Task task, OperationResult result)
            throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException,
            ExpressionEvaluationException {

        provisioningService.processAsynchronousUpdates(coords, (event, hResult) -> {
            if (event.isComplete()) {
                syncServiceMock.notifyChange(event.getChangeDescription(), task, hResult);
                event.acknowledge(true, hResult);
                return true;
            } else {
                // TODO
                LOGGER.error("Event is not complete:\n{}", event.debugDump());
                event.acknowledge(false, hResult);
                return false;
            }
        }, task, result);
    }
}
