/*
 * Copyright (c) 2010-2019 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

    public String startListeningForAsyncUpdates(ResourceShadowDiscriminator shadowCoordinates, Task task, OperationResult parentResult)
            throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException,
            ExpressionEvaluationException {
        InternalMonitor.recordCount(InternalCounters.PROVISIONING_ALL_EXT_OPERATION_COUNT);

        ProvisioningContext ctx = ctxFactory.create(shadowCoordinates, task, parentResult);
        ChangeListener listener = change -> {
            ProcessChangeRequest request = new ProcessChangeRequest(change, ctx, false);
            changeProcessor.execute(request, task, null, parentResult);
            // note that here's no try-catch block, as exceptions are converted to SystemException in default
            // implementation of ProcessChangeRequest (todo)
            if (task instanceof RunningTask) {
                ((RunningTask) task).incrementProgressAndStoreStatsIfNeeded();
            }
            return request.isSuccess();
        };
        return resourceObjectConverter.startListeningForAsyncUpdates(ctx, listener, parentResult);
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
