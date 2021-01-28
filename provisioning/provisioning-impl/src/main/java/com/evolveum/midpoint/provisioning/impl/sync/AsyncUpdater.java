/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.sync;

import com.evolveum.midpoint.util.logging.LoggingUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.provisioning.api.AsyncUpdateEvent;
import com.evolveum.midpoint.provisioning.api.AsyncUpdateEventHandler;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContextFactory;
import com.evolveum.midpoint.provisioning.impl.ResourceObjectConverter;
import com.evolveum.midpoint.provisioning.impl.adoption.AdoptedAsyncChange;
import com.evolveum.midpoint.provisioning.impl.resourceobjects.ResourceObjectAsyncChangeListener;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
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

    public void processAsynchronousUpdates(ResourceShadowDiscriminator shadowCoordinates, AsyncUpdateEventHandler handler,
            Task callerTask, OperationResult callerResult)
            throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException,
            ExpressionEvaluationException {
        InternalMonitor.recordCount(InternalCounters.PROVISIONING_ALL_EXT_OPERATION_COUNT);

        ProvisioningContext globalContext = ctxFactory.create(shadowCoordinates, callerTask, callerResult);

        // This is a bit of hack to propagate information about async update channel to upper layers
        // e.g. to implement MID-5853. TODO fix this hack
        globalContext.setChannelOverride(SchemaConstants.CHANNEL_ASYNC_UPDATE_URI);

        EventsAcknowledgeGate acknowledgeGate = new EventsAcknowledgeGate();

        ResourceObjectAsyncChangeListener listener = (resourceObjectChange, opResult) -> {

            AdoptedAsyncChange change = new AdoptedAsyncChange(resourceObjectChange, changeProcessingBeans) {
                @Override
                public void acknowledge(boolean release, OperationResult result) {
                    super.acknowledge(release, result);
                    acknowledgeGate.acknowledgeIssuedEvent();
                }
            };
            change.preprocess(opResult);

            AsyncUpdateEvent event = new AsyncUpdateEventImpl(change);

            acknowledgeGate.registerIssuedEvent();
            try {
                handler.handle(event, opResult);
            } catch (Throwable t) {
                // We assume the event was not acknowledged yet.
                // Serious handler should never throw an exception.
                LoggingUtils.logUnexpectedException(LOGGER, "Got unexpected exception while handling an async update event", t);
                acknowledgeGate.acknowledgeIssuedEvent();
            }
        };

        resourceObjectConverter.listenForAsynchronousUpdates(globalContext, listener, callerResult);

        // There may be some events in processing - for example, if the async update task is suspended while
        // receiving a lot of events.
        acknowledgeGate.waitForIssuedEventsAcknowledge();
    }
}
