/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows.sync;

import com.evolveum.midpoint.provisioning.api.ProvisioningOperationContext;
import com.evolveum.midpoint.schema.ResourceOperationCoordinates;
import com.evolveum.midpoint.schema.ResourceShadowCoordinates;
import com.evolveum.midpoint.util.logging.LoggingUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.provisioning.api.AsyncUpdateEvent;
import com.evolveum.midpoint.provisioning.api.AsyncUpdateEventHandler;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContextFactory;
import com.evolveum.midpoint.provisioning.impl.resourceobjects.ResourceObjectConverter;
import com.evolveum.midpoint.provisioning.impl.shadows.ShadowedAsyncChange;
import com.evolveum.midpoint.provisioning.impl.resourceobjects.ResourceObjectAsyncChangeListener;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * Implements Async Update functionality. (Currently not much, but this might change as we'll implement multi-threading.
 * Then we'll maybe find some code common with LiveSynchronizer.)
 */
@Component
public class AsyncUpdater {

    @SuppressWarnings("unused")
    private static final Trace LOGGER = TraceManager.getTrace(AsyncUpdater.class);

    @Autowired private ProvisioningContextFactory ctxFactory;
    @Autowired private ResourceObjectConverter resourceObjectConverter;
    @Autowired private ChangeProcessingBeans changeProcessingBeans;

    public void processAsynchronousUpdates(ResourceOperationCoordinates coordinates, AsyncUpdateEventHandler handler,
            Task callerTask, OperationResult callerResult)
            throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException,
            ExpressionEvaluationException {
        InternalMonitor.recordCount(InternalCounters.PROVISIONING_ALL_EXT_OPERATION_COUNT);

        ProvisioningContext globalContext = ctxFactory.createForBulkOperation(coordinates, new ProvisioningOperationContext(), callerTask, callerResult);

        IndividualEventsAcknowledgeGate<AsyncUpdateEvent> acknowledgeGate = new IndividualEventsAcknowledgeGate<>();

        ResourceObjectAsyncChangeListener listener = (resourceObjectChange, lTask, lResult) -> {

            ShadowedAsyncChange change = new ShadowedAsyncChange(resourceObjectChange, changeProcessingBeans);
            change.initialize(lTask, lResult);

            AsyncUpdateEvent event = new AsyncUpdateEventImpl(change) {
                @Override
                public void acknowledge(boolean release, OperationResult result) {
                    LOGGER.trace("Acknowledgement (release={}) sent for {}", release, this);
                    change.acknowledge(release, result);
                    acknowledgeGate.acknowledgeIssuedEvent(this);
                }
            };

            acknowledgeGate.registerIssuedEvent(event);
            try {
                handler.handle(event, lResult);
            } catch (Throwable t) {
                LoggingUtils.logUnexpectedException(LOGGER, "Got unexpected exception while handling an async update event", t);
                acknowledgeGate.acknowledgeIssuedEvent(event);
            }
        };

        resourceObjectConverter.listenForAsynchronousUpdates(globalContext, listener, callerResult);

        // There may be some events in processing - for example, if the async update task is suspended while
        // receiving a lot of events.
        acknowledgeGate.waitForIssuedEventsAcknowledge(callerResult);
    }
}
