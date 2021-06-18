/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows.task;

import com.evolveum.midpoint.repo.common.task.*;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.Task;

import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.repo.common.activity.execution.ExecutionInstantiationContext;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractActivityWorkStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Execution of a propagation task. It has always a single part, so the resource can be stored here.
 */
public class MultiPropagationActivityExecution
        extends AbstractSearchIterativeActivityExecution
        <ResourceType,
                MultiPropagationWorkDefinition,
                MultiPropagationActivityHandler,
                MultiPropagationActivityExecution,
                AbstractActivityWorkStateType> {

    private static final Trace LOGGER = TraceManager.getTrace(MultiPropagationActivityExecution.class);

    private static final String SHORT_NAME = "Multi-propagation";

    MultiPropagationActivityExecution(
            @NotNull ExecutionInstantiationContext<MultiPropagationWorkDefinition, MultiPropagationActivityHandler> context) {
        super(context, SHORT_NAME);
    }

    @Override
    public @NotNull ActivityReportingOptions getDefaultReportingOptions() {
        ActivityReportingOptions options = new ActivityReportingOptions();
        options.setPreserveStatistics(false);
        options.setEnableSynchronizationStatistics(false);
        options.setSkipWritingOperationExecutionRecords(true); // to avoid resource change (invalidates the caches)
        return options;
    }

    @Override
    protected @NotNull ItemProcessor<PrismObject<ResourceType>> createItemProcessor(OperationResult opResult) {
        return createDefaultItemProcessor(this::propagateOperationsOnResource);
    }

    private boolean propagateOperationsOnResource(PrismObject<ResourceType> resource,
            ItemProcessingRequest<PrismObject<ResourceType>> request, RunningTask workerTask, OperationResult taskResult)
            throws SchemaException {

        LOGGER.trace("Propagating provisioning operations on {}", resource);

        ObjectQuery shadowQuery = beans.prismContext.queryFor(ShadowType.class)
                .item(ShadowType.F_RESOURCE_REF).ref(resource.getOid())
                .and()
                .exists(ShadowType.F_PENDING_OPERATION)
                .build();

        beans.repositoryService.searchObjectsIterative(ShadowType.class, shadowQuery, (shadow, result) -> {
            propagateOperationsOnShadow(shadow, resource, workerTask, result);
            return true;
        }, null, true, taskResult);

        LOGGER.trace("Propagation of {} done", resource);
        return true;
    }

    private void propagateOperationsOnShadow(PrismObject<ShadowType> shadow, PrismObject<ResourceType> resource,
            Task workerTask, OperationResult result) {
        try {
            getActivityHandler().shadowsFacade.propagateOperations(resource, shadow, workerTask, result);
        } catch (CommonException | GenericFrameworkException | EncryptionException e) {
            throw new SystemException("Provisioning error: " + e.getMessage(), e);
        }
    }

    @Override
    protected void debugDumpExtra(StringBuilder sb, int indent) {
    }
}
