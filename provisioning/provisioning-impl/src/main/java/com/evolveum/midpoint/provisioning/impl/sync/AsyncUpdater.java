/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.sync;

import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContextFactory;
import com.evolveum.midpoint.provisioning.impl.ResourceObjectConverter;
import com.evolveum.midpoint.provisioning.ucf.api.ChangeListener;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AsyncUpdateListeningActivityInformationType;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
    @Autowired private ChangeProcessor changeProcessor;

    public String startListeningForAsyncUpdates(ResourceShadowDiscriminator shadowCoordinates, Task callerTask, OperationResult callerResult)
            throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException,
            ExpressionEvaluationException {
        InternalMonitor.recordCount(InternalCounters.PROVISIONING_ALL_EXT_OPERATION_COUNT);

        ProvisioningContext ctx = ctxFactory.create(shadowCoordinates, callerTask, callerResult);

        ChangeListener listener = (change, listenerTask, listenerResult) -> {
            ProcessChangeRequest request = new ProcessChangeRequest(change, ctx, false);
            changeProcessor.execute(request, listenerTask, null, listenerResult);
            // note that here's no try-catch block, as exceptions are converted to SystemException in default
            // implementation of ProcessChangeRequest (todo)

            // TODO TODO TODO this will not work now
            if (listenerTask instanceof RunningTask) {
                ((RunningTask) listenerTask).incrementProgressAndStoreStatsIfNeeded();
            }
            return request.isSuccess();
        };
        return resourceObjectConverter.startListeningForAsyncUpdates(ctx, listener, callerResult);
    }

    @SuppressWarnings("unused")
    public void stopListeningForAsyncUpdates(String listeningActivityHandle, Task task, OperationResult parentResult) {
        InternalMonitor.recordCount(InternalCounters.PROVISIONING_ALL_EXT_OPERATION_COUNT);
        resourceObjectConverter.stopListeningForAsyncUpdates(listeningActivityHandle, parentResult);
    }

    @SuppressWarnings("unused")
    public AsyncUpdateListeningActivityInformationType getAsyncUpdatesListeningActivityInformation(
            @NotNull String listeningActivityHandle, Task task, OperationResult parentResult) {
        return resourceObjectConverter.getAsyncUpdatesListeningActivityInformation(listeningActivityHandle, parentResult);
    }
}
