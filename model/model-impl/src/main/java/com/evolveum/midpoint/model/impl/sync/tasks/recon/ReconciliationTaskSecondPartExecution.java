/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.sync.tasks.recon;

import java.util.Collection;
import java.util.function.Function;

import com.evolveum.midpoint.model.impl.sync.tasks.SyncTaskHelper;
import com.evolveum.midpoint.model.impl.sync.tasks.Synchronizer;
import com.evolveum.midpoint.model.impl.tasks.AbstractSearchIterativeModelTaskPartExecution;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.api.PreconditionViolationException;
import com.evolveum.midpoint.repo.common.task.AbstractSearchIterativeResultHandler;
import com.evolveum.midpoint.repo.common.task.HandledObjectType;
import com.evolveum.midpoint.repo.common.task.ResultHandlerClass;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FetchErrorReportingMethodType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Execution of resource objects reconciliation (the main part of reconciliation).
 */
@ResultHandlerClass(ReconciliationTaskSecondPartExecution.Handler.class)
@HandledObjectType(ShadowType.class)
class ReconciliationTaskSecondPartExecution
        extends AbstractSearchIterativeModelTaskPartExecution
        <ShadowType,
                ReconciliationTaskHandler,
                ReconciliationTaskExecution,
                ReconciliationTaskSecondPartExecution,
                ReconciliationTaskSecondPartExecution.Handler> {

    private static final Trace LOGGER = TraceManager.getTrace(ReconciliationTaskHandler.class);

    private final Synchronizer synchronizer;

    ReconciliationTaskSecondPartExecution(ReconciliationTaskExecution taskExecution) {
        super(taskExecution);
        this.synchronizer = createSynchronizer();
    }

    private Synchronizer createSynchronizer() {
        SyncTaskHelper.TargetInfo targetInfo = taskExecution.getTargetInfo();
        return new Synchronizer(
                targetInfo.getResource(),
                targetInfo.getObjectClassDefinition(),
                taskExecution.getObjectsFilter(),
                taskHandler.getObjectChangeListener(),
                taskHandler.taskTypeName,
                SchemaConstants.CHANNEL_RECON,
                taskExecution.partDefinition,
                false);
    }

    @Override
    protected ObjectQuery createQuery(OperationResult opResult) throws SchemaException, ObjectNotFoundException,
            CommunicationException, ConfigurationException, ExpressionEvaluationException, SecurityViolationException {
        return taskExecution.createShadowQuery(opResult);
    }

    @Override
    protected Collection<SelectorOptions<GetOperationOptions>> createSearchOptions(OperationResult opResult) {
        return taskHandler.schemaHelper.getOperationOptionsBuilder()
                .errorReportingMethod(FetchErrorReportingMethodType.FETCH_RESULT)
                .build();
    }

    @Override
    protected Function<ItemPath, ItemDefinition<?>> createItemDefinitionProvider() {
        return createItemDefinitionProviderForAttributes(taskExecution.getTargetInfo().getObjectClassDefinition());
    }

    @Override
    protected void finish(OperationResult opResult) throws SchemaException {
        taskExecution.reconResult.setResourceReconCount(resultHandler.getProgress());
        taskExecution.reconResult.setResourceReconErrors(resultHandler.getErrors());
    }

    protected class Handler
            extends AbstractSearchIterativeResultHandler
            <ShadowType,
                    ReconciliationTaskHandler,
                    ReconciliationTaskExecution,
                    ReconciliationTaskSecondPartExecution,
                    ReconciliationTaskSecondPartExecution.Handler> {

        public Handler(ReconciliationTaskSecondPartExecution partExecution) {
            super(partExecution);
        }

        @Override
        protected boolean handleObject(PrismObject<ShadowType> object, RunningTask workerTask, OperationResult result)
                throws CommonException, PreconditionViolationException {
            synchronizer.handleObject(object, workerTask, result);
            return true;
        }
    }
}
