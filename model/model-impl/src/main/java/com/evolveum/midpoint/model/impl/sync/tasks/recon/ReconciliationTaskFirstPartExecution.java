/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.sync.tasks.recon;

import com.evolveum.midpoint.model.api.ModelPublicConstants;
import com.evolveum.midpoint.model.impl.tasks.AbstractIterativeModelTaskPartExecution;
import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.provisioning.api.ProvisioningOperationOptions;
import com.evolveum.midpoint.repo.api.PreconditionViolationException;
import com.evolveum.midpoint.repo.cache.RepositoryCache;
import com.evolveum.midpoint.repo.common.task.AbstractSearchIterativeItemProcessor;
import com.evolveum.midpoint.repo.common.task.HandledObjectType;
import com.evolveum.midpoint.repo.common.task.ItemProcessingRequest;
import com.evolveum.midpoint.repo.common.task.ItemProcessorClass;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Scans shadows for unfinished operations and tries to finish them.
 */
@ItemProcessorClass(ReconciliationTaskFirstPartExecution.ItemProcessor.class)
@HandledObjectType(ShadowType.class)
class ReconciliationTaskFirstPartExecution
        extends AbstractIterativeModelTaskPartExecution
        <ShadowType,
                ReconciliationTaskHandler,
                ReconciliationTaskExecution,
                ReconciliationTaskFirstPartExecution,
                ReconciliationTaskFirstPartExecution.ItemProcessor> {

    ReconciliationTaskFirstPartExecution(ReconciliationTaskExecution taskExecution) {
        super(taskExecution);
        reportingOptions.setEnableSynchronizationStatistics(false);

        setPartUri(ModelPublicConstants.RECONCILIATION_OPERATION_COMPLETION_PART_URI);
        setProcessShortNameCapitalized("Reconciliation (operation completion)");
        setContextDescription("on " + taskExecution.getTargetInfo().getContextDescription());
        setRequiresDirectRepositoryAccess();
    }

    @Override
    protected ObjectQuery createQuery(OperationResult opResult) throws SchemaException {
        return getPrismContext().queryFor(ShadowType.class)
                .item(ShadowType.F_RESOURCE_REF).ref(taskExecution.getResourceOid())
                .and()
                .exists(ShadowType.F_PENDING_OPERATION)
                .build();
    }

    @Override
    protected void finish(OperationResult opResult) throws SchemaException {
        super.finish(opResult);
        taskExecution.reconResult.setUnOpsCount(bucketStatistics.getItemsProcessed());
    }

    protected static class ItemProcessor
            extends AbstractSearchIterativeItemProcessor
            <ShadowType,
                    ReconciliationTaskHandler,
                    ReconciliationTaskExecution,
                    ReconciliationTaskFirstPartExecution,
                    ItemProcessor> {

        public ItemProcessor(ReconciliationTaskFirstPartExecution partExecution) {
            super(partExecution);
        }

        @Override
        protected boolean processObject(PrismObject<ShadowType> object,
                ItemProcessingRequest<PrismObject<ShadowType>> request,
                RunningTask workerTask, OperationResult result)
                throws CommonException, PreconditionViolationException {
            RepositoryCache.enterLocalCaches(taskHandler.cacheConfigurationManager);
            try {
                ProvisioningOperationOptions options = ProvisioningOperationOptions.createForceRetry(Boolean.TRUE);
                ModelImplUtils.clearRequestee(workerTask);
                taskHandler.getProvisioningService().refreshShadow(object, options, workerTask, result);
                return true;
            } finally {
                workerTask.markObjectActionExecutedBoundary();
                RepositoryCache.exitLocalCaches();
            }
        }
    }
}
