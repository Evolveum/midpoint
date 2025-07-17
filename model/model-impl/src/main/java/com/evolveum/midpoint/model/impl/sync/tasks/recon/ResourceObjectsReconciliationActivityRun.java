/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.sync.tasks.recon;

import com.evolveum.midpoint.repo.common.activity.run.ActivityRunException;

import com.evolveum.midpoint.repo.common.activity.run.SearchSpecification;

import com.evolveum.midpoint.schema.GetOperationOptions;

import com.google.common.annotations.VisibleForTesting;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.impl.sync.tasks.Synchronizer;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunInstantiationContext;
import com.evolveum.midpoint.repo.common.activity.run.ActivityReportingCharacteristics;
import com.evolveum.midpoint.repo.common.activity.run.processing.ItemProcessingRequest;
import com.evolveum.midpoint.repo.common.activity.run.buckets.ItemDefinitionProvider;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FetchErrorReportingMethodType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Execution of resource objects reconciliation (the main part of reconciliation).
 */
public final class ResourceObjectsReconciliationActivityRun
        extends PartialReconciliationActivityRun {

    private Synchronizer synchronizer;

    ResourceObjectsReconciliationActivityRun(
            @NotNull ActivityRunInstantiationContext<ReconciliationWorkDefinition, ReconciliationActivityHandler> context,
            String shortNameCapitalized) {
        super(context, shortNameCapitalized);
        setInstanceReady();
    }

    @Override
    public boolean beforeRun(OperationResult result) throws CommonException, ActivityRunException {
        if (!super.beforeRun(result)) {
            return false;
        }
        synchronizer = createSynchronizer();
        return true;
    }

    @Override
    public @NotNull ActivityReportingCharacteristics createReportingCharacteristics() {
        return super.createReportingCharacteristics()
                .actionsExecutedStatisticsSupported(true)
                .synchronizationStatisticsSupported(true);
    }

    private Synchronizer createSynchronizer() {
        return new Synchronizer(
                processingScope.getResource(),
                processingScope.getPostSearchFilter(),
                getModelBeans().eventDispatcher,
                SchemaConstants.CHANNEL_RECON,
                false);
    }

    // Ignoring configured search options. TODO ok?
    @Override
    public void customizeSearchOptions(SearchSpecification<ShadowType> searchSpecification, OperationResult result) {

        // We want to preserve "no fetch" option for the main reconciliation sub-activity.
        var noFetchRequested = GetOperationOptions.isNoFetch(searchSpecification.getSearchOptions());

        // Error reporting method: This is necessary to give ItemProcessingGatekeeper a chance to "see" errors in preprocessing.
        // At the same time, it ensures that an exception in preprocessing does not kill the whole searchObjectsIterative call.
        searchSpecification.setSearchOptions(
                getBeans().schemaService.getOperationOptionsBuilder()
                        .errorReportingMethod(FetchErrorReportingMethodType.FETCH_RESULT)
                        .noFetch(noFetchRequested)
                        .build());
    }

    @Override
    public ItemDefinitionProvider createItemDefinitionProvider() {
        return processingScope.createItemDefinitionProvider();
    }

    @Override
    public boolean processItem(@NotNull ShadowType object,
            @NotNull ItemProcessingRequest<ShadowType> request, RunningTask workerTask, OperationResult result)
            throws CommonException, ActivityRunException {
        synchronizer.synchronize(object.asPrismObject(), request.getIdentifier(), workerTask, result);
        return true;
    }

    @VisibleForTesting
    public long getResourceReconCount() {
        return transientRunStatistics.getItemsProcessed();
    }

    @VisibleForTesting
    public long getResourceReconErrors() {
        return transientRunStatistics.getErrors();
    }

    @Override
    protected String getChannelOverride() {
        return SchemaConstants.CHANNEL_RECON_URI;
    }
}
