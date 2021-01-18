/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.sync.tasks.recon;

import com.evolveum.midpoint.model.impl.tasks.AbstractSearchIterativeModelTaskPartExecution;
import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.provisioning.api.ProvisioningOperationOptions;
import com.evolveum.midpoint.repo.api.PreconditionViolationException;
import com.evolveum.midpoint.repo.cache.RepositoryCache;
import com.evolveum.midpoint.repo.common.task.AbstractSearchIterativeResultHandler;
import com.evolveum.midpoint.repo.common.task.HandledObjectType;
import com.evolveum.midpoint.repo.common.task.ResultHandlerClass;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Scans shadows for unfinished operations and tries to finish them.
 */
@ResultHandlerClass(ReconciliationTaskFirstPartExecution.Handler.class)
@HandledObjectType(ShadowType.class)
class ReconciliationTaskFirstPartExecution
        extends AbstractSearchIterativeModelTaskPartExecution
        <ShadowType,
                ReconciliationTaskHandler,
                ReconciliationTaskExecution,
                ReconciliationTaskFirstPartExecution,
                ReconciliationTaskFirstPartExecution.Handler> {

    private static final Trace LOGGER = TraceManager.getTrace(ReconciliationTaskHandler.class);

    ReconciliationTaskFirstPartExecution(ReconciliationTaskExecution taskExecution) {
        super(taskExecution);
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
    protected boolean requiresDirectRepositoryAccess(OperationResult opResult) {
        return true;
    }

//    private void TODO() {
//        LOGGER.trace("Found {} accounts that were not successfully processed.", shadows.size());
//        reconResult.setUnOpsCount(shadows.size());
//
//        String message = "Processing unfinished operations done. Out of " + shadows.size() + " objects, "
//                + processedSuccess + " were processed successfully and processing of " + processedFailure + " resulted in failure. " +
//                "Total time spent: " + (System.currentTimeMillis() - startedAll) + " ms. " +
//                (!task.canRun() ? "Was interrupted during processing." : "");
//
//    }
//
//    private boolean scanForUnfinishedOperations(RunningTask task, String resourceOid, ReconciliationTaskResult reconResult,
//            OperationResult result) throws SchemaException {
//
//        long startedAll = System.currentTimeMillis();
//
//        for (PrismObject<ShadowType> shadow : shadows) {
//
//        }
//
//
//        opResult.computeStatus();
//        result.createSubresult(opResult.getOperation()+".statistics").recordStatus(opResult.getStatus(), message);
//
//        LOGGER.debug("{}. Result: {}", message, opResult.getStatus());
//        return task.canRun();
//    }

    @Override
    protected void finish(OperationResult opResult) throws SchemaException {
        taskExecution.reconResult.setUnOpsCount(resultHandler.getProgress());
    }

    protected static class Handler
            extends AbstractSearchIterativeResultHandler
            <ShadowType,
                    ReconciliationTaskHandler,
                    ReconciliationTaskExecution,
                    ReconciliationTaskFirstPartExecution,
                    ReconciliationTaskFirstPartExecution.Handler> {

        public Handler(ReconciliationTaskFirstPartExecution partExecution) {
            super(partExecution);
        }

        @Override
        protected boolean handleObject(PrismObject<ShadowType> object, RunningTask workerTask, OperationResult result)
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
