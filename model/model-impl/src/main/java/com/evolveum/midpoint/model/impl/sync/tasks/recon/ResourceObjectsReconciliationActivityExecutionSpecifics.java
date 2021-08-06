/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.sync.tasks.recon;

import java.util.Collection;

import com.google.common.annotations.VisibleForTesting;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.impl.sync.tasks.Synchronizer;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.common.activity.ActivityExecutionException;
import com.evolveum.midpoint.repo.common.task.ActivityReportingOptions;
import com.evolveum.midpoint.repo.common.task.ItemProcessingRequest;
import com.evolveum.midpoint.repo.common.task.SearchBasedActivityExecution;
import com.evolveum.midpoint.repo.common.task.work.ItemDefinitionProvider;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FetchErrorReportingMethodType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Execution of resource objects reconciliation (the main part of reconciliation).
 */
public class ResourceObjectsReconciliationActivityExecutionSpecifics
        extends PartialReconciliationActivityExecutionSpecifics {

    private Synchronizer synchronizer;

    ResourceObjectsReconciliationActivityExecutionSpecifics(@NotNull SearchBasedActivityExecution<ShadowType,
            ReconciliationWorkDefinition, ReconciliationActivityHandler, ?> activityExecution) {
        super(activityExecution);
    }

    @Override
    public void beforeExecution(OperationResult opResult) throws CommonException, ActivityExecutionException {
        super.beforeExecution(opResult);
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
                activityExecution.isSimulate(),
                false);
    }

    // Ignoring configured search options. TODO ok?
    @Override
    public Collection<SelectorOptions<GetOperationOptions>> customizeSearchOptions(
            Collection<SelectorOptions<GetOperationOptions>> configuredOptions, OperationResult result) {
        // This is necessary to give ItemProcessingGatekeeper a chance to "see" errors in preprocessing.
        // At the same time, it ensures that an exception in preprocessing does not kill the whole searchObjectsIterative call.
        return getBeans().schemaService.getOperationOptionsBuilder()
                .errorReportingMethod(FetchErrorReportingMethodType.FETCH_RESULT)
                .build();
    }

    @Override
    public ItemDefinitionProvider createItemDefinitionProvider() {
        return ItemDefinitionProvider.forObjectClassAttributes(objectClassSpec.getObjectClassDefinition());
    }

    @Override
    public boolean processObject(@NotNull PrismObject<ShadowType> object,
            @NotNull ItemProcessingRequest<PrismObject<ShadowType>> request, RunningTask workerTask, OperationResult result)
            throws CommonException, ActivityExecutionException {
        synchronizer.synchronize(object, request.getIdentifier(), workerTask, result);
        return true;
    }

    @VisibleForTesting
    public long getResourceReconCount() {
        return activityExecution.getTransientExecutionStatistics().getItemsProcessed();
    }

    @VisibleForTesting
    public long getResourceReconErrors() {
        return activityExecution.getTransientExecutionStatistics().getErrors();
    }
}
