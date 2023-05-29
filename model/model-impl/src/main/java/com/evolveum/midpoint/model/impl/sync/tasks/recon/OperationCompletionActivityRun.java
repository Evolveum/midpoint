/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.sync.tasks.recon;

import com.evolveum.midpoint.repo.common.activity.run.SearchSpecification;

import com.google.common.annotations.VisibleForTesting;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.provisioning.api.ProvisioningOperationOptions;
import com.evolveum.midpoint.repo.cache.RepositoryCache;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunInstantiationContext;
import com.evolveum.midpoint.repo.common.activity.run.processing.ItemProcessingRequest;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Scans shadows for unfinished operations and tries to finish them.
 */
final class OperationCompletionActivityRun
        extends PartialReconciliationActivityRun {

    OperationCompletionActivityRun(
            @NotNull ActivityRunInstantiationContext<ReconciliationWorkDefinition, ReconciliationActivityHandler> activityRun,
            String shortNameCapitalized) {
        super(activityRun, shortNameCapitalized);
        setInstanceReady();
    }

    @Override
    public boolean doesRequireDirectRepositoryAccess() {
        return true;
    }

    /**
     * We ignore other parameters like kind, intent or object class. This is a behavior inherited from pre-4.4.
     */
    @Override
    public void customizeQuery(@NotNull SearchSpecification<ShadowType> searchSpecification, OperationResult result) {
        searchSpecification.setQuery(
                getBeans().prismContext.queryFor(ShadowType.class)
                        .item(ShadowType.F_RESOURCE_REF).ref(processingScope.getResourceOid())
                        .and()
                        .exists(ShadowType.F_PENDING_OPERATION)
                        .build());
    }

    @Override
    public boolean processItem(@NotNull ShadowType object,
            @NotNull ItemProcessingRequest<ShadowType> request,
            RunningTask workerTask, OperationResult result)
            throws CommonException {

        RepositoryCache.enterLocalCaches(getModelBeans().cacheConfigurationManager);
        try {
            ProvisioningOperationOptions options = ProvisioningOperationOptions.createForceRetry(Boolean.TRUE);
            ModelImplUtils.clearRequestee(workerTask);
            getModelBeans().provisioningService.refreshShadow(object.asPrismObject(), options, workerTask, result);
            return true;
        } finally {
            RepositoryCache.exitLocalCaches();
        }
    }

    @VisibleForTesting
    long getUnOpsCount() {
        return transientRunStatistics.getItemsProcessed();
    }
}
