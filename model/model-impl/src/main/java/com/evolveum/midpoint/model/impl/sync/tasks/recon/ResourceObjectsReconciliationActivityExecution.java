/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.sync.tasks.recon;

import java.util.Collection;
import java.util.function.Function;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ExecutionModeType;

import com.google.common.annotations.VisibleForTesting;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.impl.sync.tasks.Synchronizer;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.common.activity.ActivityExecutionException;
import com.evolveum.midpoint.repo.common.activity.execution.ExecutionInstantiationContext;
import com.evolveum.midpoint.repo.common.task.ActivityReportingOptions;
import com.evolveum.midpoint.repo.common.task.ItemProcessor;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FetchErrorReportingMethodType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Execution of resource objects reconciliation (the main part of reconciliation).
 */
public class ResourceObjectsReconciliationActivityExecution
        extends PartialReconciliationActivityExecution<ResourceObjectsReconciliationActivityExecution> {

    private Synchronizer synchronizer;

    ResourceObjectsReconciliationActivityExecution(
            @NotNull ExecutionInstantiationContext<ReconciliationWorkDefinition, ReconciliationActivityHandler> context) {
        super(context, "Reconciliation (on resource)" + modeSuffix(context));
    }

    // TODO generalize
    private static String modeSuffix(
            ExecutionInstantiationContext<ReconciliationWorkDefinition, ReconciliationActivityHandler> context) {
        return context.getActivity().getWorkDefinition().getExecutionMode() == ExecutionModeType.SIMULATE ?
                " (simulated)" : "";
    }

    @Override
    protected void initializeExecution(OperationResult opResult) throws CommonException, ActivityExecutionException {
        super.initializeExecution(opResult);
        synchronizer = createSynchronizer();
    }

    @Override
    public @NotNull ActivityReportingOptions getDefaultReportingOptions() {
        return new ActivityReportingOptions()
                .enableActionsExecutedStatistics(true)
                .enableSynchronizationStatistics(true);
    }

    private Synchronizer createSynchronizer() {
        return new Synchronizer(
                objectClassSpec.getResource(),
                objectClassSpec.getObjectClassDefinitionRequired(),
                objectsFilter,
                getModelBeans().eventDispatcher,
                SchemaConstants.CHANNEL_RECON,
                isSimulate(),
                false);
    }

    // Ignoring configured search options. TODO ok?
    @Override
    protected Collection<SelectorOptions<GetOperationOptions>> customizeSearchOptions(Collection<SelectorOptions<GetOperationOptions>> configuredOptions, OperationResult opResult) {
        // This is necessary to give ItemProcessingGatekeeper a chance to "see" errors in preprocessing.
        // At the same time, it ensures that an exception in preprocessing does not kill the whole searchObjectsIterative call.
        return beans.schemaService.getOperationOptionsBuilder()
                .errorReportingMethod(FetchErrorReportingMethodType.FETCH_RESULT)
                .build();
    }

    @Override
    protected Function<ItemPath, ItemDefinition<?>> createItemDefinitionProvider() {
        return createItemDefinitionProviderForAttributes(objectClassSpec.getObjectClassDefinition());
    }

    @Override
    protected void finishExecution(OperationResult opResult) throws SchemaException {
//        taskExecution.reconResult.setResourceReconCount(bucketStatistics.getItemsProcessed());
//        taskExecution.reconResult.setResourceReconErrors(bucketStatistics.getErrors());
    }

    @Override
    protected @NotNull ItemProcessor<PrismObject<ShadowType>> createItemProcessor(OperationResult opResult) {
        return createDefaultItemProcessor(
                (object, request, workerTask, result) -> {
                    synchronizer.synchronize(object, request.getIdentifier(), workerTask, result);
                    return true;
                }
        );
    }

    @VisibleForTesting
    public long getResourceReconCount() {
        return executionStatistics.getItemsProcessed();
    }

    @VisibleForTesting
    public long getResourceReconErrors() {
        return executionStatistics.getErrors();
    }
}
