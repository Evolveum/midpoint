/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.sync.tasks.recon;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.provisioning.api.ProvisioningOperationOptions;
import com.evolveum.midpoint.repo.cache.RepositoryCache;
import com.evolveum.midpoint.repo.common.activity.ActivityExecutionException;
import com.evolveum.midpoint.repo.common.activity.execution.ExecutionInstantiationContext;
import com.evolveum.midpoint.repo.common.task.ItemProcessingRequest;
import com.evolveum.midpoint.repo.common.task.ItemProcessor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Scans shadows for unfinished operations and tries to finish them.
 */
class OperationCompletionActivityExecution
        extends PartialReconciliationActivityExecution<OperationCompletionActivityExecution> {

    OperationCompletionActivityExecution(
            @NotNull ExecutionInstantiationContext<ReconciliationWorkDefinition, ReconciliationActivityHandler> context) {
        super(context, "Reconciliation (operation completion)");
    }

    @Override
    protected void initializeExecution(OperationResult opResult) throws CommonException, ActivityExecutionException {
        super.initializeExecution(opResult);
        setRequiresDirectRepositoryAccess();
    }

    /**
     * We ignore other parameters like kind, intent or object class. This is a behavior inherited from pre-4.4.
     */
    @Override
    protected ObjectQuery customizeQuery(ObjectQuery configuredQuery, OperationResult opResult) {
        return getPrismContext().queryFor(ShadowType.class)
                .item(ShadowType.F_RESOURCE_REF).ref(targetInfo.getResourceOid())
                .and()
                .exists(ShadowType.F_PENDING_OPERATION)
                .build();
    }

    @Override
    protected @NotNull ItemProcessor<PrismObject<ShadowType>> createItemProcessor(OperationResult opResult) {
        return createDefaultItemProcessor(this::processObject);
    }

    protected boolean processObject(PrismObject<ShadowType> object,
            ItemProcessingRequest<PrismObject<ShadowType>> request,
            RunningTask workerTask, OperationResult result)
            throws CommonException {

        RepositoryCache.enterLocalCaches(getModelBeans().cacheConfigurationManager);
        try {
            ProvisioningOperationOptions options = ProvisioningOperationOptions.createForceRetry(Boolean.TRUE);
            ModelImplUtils.clearRequestee(workerTask);
            getModelBeans().provisioningService.refreshShadow(object, options, workerTask, result);
            return true;
        } finally {
            workerTask.markObjectActionExecutedBoundary();
            RepositoryCache.exitLocalCaches();
        }
    }
}
