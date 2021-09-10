/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.sync.tasks.recon;

import com.evolveum.midpoint.model.impl.ModelBeans;

import com.google.common.annotations.VisibleForTesting;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.provisioning.api.ProvisioningOperationOptions;
import com.evolveum.midpoint.repo.cache.RepositoryCache;
import com.evolveum.midpoint.repo.common.activity.execution.ExecutionInstantiationContext;
import com.evolveum.midpoint.repo.common.task.ItemProcessingRequest;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Scans shadows for unfinished operations and tries to finish them.
 */
class OperationCompletionActivityExecution
        extends PartialReconciliationActivityExecution {

    OperationCompletionActivityExecution(
            @NotNull ExecutionInstantiationContext<ReconciliationWorkDefinition, ReconciliationActivityHandler> activityExecution,
            String shortNameCapitalized) {
        super(activityExecution, shortNameCapitalized);
    }

    @Override
    public boolean doesRequireDirectRepositoryAccess() {
        return true;
    }

    /**
     * We ignore other parameters like kind, intent or object class. This is a behavior inherited from pre-4.4.
     */
    @Override
    public ObjectQuery customizeQuery(ObjectQuery configuredQuery, OperationResult result) {
        return getBeans().prismContext.queryFor(ShadowType.class)
                .item(ShadowType.F_RESOURCE_REF).ref(objectClassSpec.getResourceOid())
                .and()
                .exists(ShadowType.F_PENDING_OPERATION)
                .build();
    }

    @Override
    public boolean processObject(@NotNull PrismObject<ShadowType> object,
            @NotNull ItemProcessingRequest<PrismObject<ShadowType>> request,
            RunningTask workerTask, OperationResult result)
            throws CommonException {

        RepositoryCache.enterLocalCaches(getModelBeans().cacheConfigurationManager);
        try {
            ProvisioningOperationOptions options = ProvisioningOperationOptions.createForceRetry(Boolean.TRUE);
            ModelImplUtils.clearRequestee(workerTask);
            getModelBeans().provisioningService.refreshShadow(object, options, workerTask, result);
            return true;
        } finally {
            RepositoryCache.exitLocalCaches();
        }
    }

    @VisibleForTesting
    long getUnOpsCount() {
        return transientExecutionStatistics.getItemsProcessed();
    }
}
