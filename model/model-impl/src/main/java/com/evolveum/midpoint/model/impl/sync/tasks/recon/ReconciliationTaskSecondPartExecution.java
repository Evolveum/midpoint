/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.sync.tasks.recon;

import java.util.Collection;
import java.util.function.Function;

import com.evolveum.midpoint.model.api.ModelPublicConstants;
import com.evolveum.midpoint.model.impl.sync.tasks.SyncTaskHelper;
import com.evolveum.midpoint.model.impl.sync.tasks.Synchronizer;
import com.evolveum.midpoint.model.impl.tasks.AbstractIterativeModelTaskPartExecution;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.api.PreconditionViolationException;
import com.evolveum.midpoint.repo.common.task.AbstractSearchIterativeItemProcessor;
import com.evolveum.midpoint.repo.common.task.HandledObjectType;
import com.evolveum.midpoint.repo.common.task.ItemProcessingRequest;
import com.evolveum.midpoint.repo.common.task.ItemProcessorClass;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FetchErrorReportingMethodType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Execution of resource objects reconciliation (the main part of reconciliation).
 */
@ItemProcessorClass(ReconciliationTaskSecondPartExecution.ItemProcessor.class)
@HandledObjectType(ShadowType.class)
class ReconciliationTaskSecondPartExecution
        extends AbstractIterativeModelTaskPartExecution
        <ShadowType,
                ReconciliationTaskHandler,
                ReconciliationTaskExecution,
                ReconciliationTaskSecondPartExecution,
                ReconciliationTaskSecondPartExecution.ItemProcessor> {

    private final Synchronizer synchronizer;

    ReconciliationTaskSecondPartExecution(ReconciliationTaskExecution taskExecution) {
        super(taskExecution);
        setPartUri(ModelPublicConstants.RECONCILIATION_RESOURCE_OBJECTS_PART_URI);
        setProcessShortNameCapitalized("Reconciliation (on resource)");
        setContextDescription("on " + taskExecution.getTargetInfo().getContextDescription());
        this.synchronizer = createSynchronizer();
    }

    private Synchronizer createSynchronizer() {
        SyncTaskHelper.TargetInfo targetInfo = taskExecution.getTargetInfo();
        return new Synchronizer(
                targetInfo.getResource(),
                targetInfo.getObjectClassDefinition(),
                taskExecution.getObjectsFilter(),
                taskHandler.getObjectChangeListener(),
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
        // This is necessary to give ItemProcessingGatekeeper a chance to "see" errors in preprocessing.
        // At the same time, it ensures that an exception in preprocessing does not kill the whole searchObjectsIterative call.
        // TODO generalize
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
        super.finish(opResult);
        taskExecution.reconResult.setResourceReconCount(statistics.getItemsProcessed());
        taskExecution.reconResult.setResourceReconErrors(statistics.getErrors());
    }

    protected class ItemProcessor
            extends AbstractSearchIterativeItemProcessor
            <ShadowType, ReconciliationTaskHandler, ReconciliationTaskExecution, ReconciliationTaskSecondPartExecution, ItemProcessor> {

        public ItemProcessor(ReconciliationTaskSecondPartExecution partExecution) {
            super(partExecution);
        }

        @Override
        protected boolean processObject(PrismObject<ShadowType> object, ItemProcessingRequest<PrismObject<ShadowType>> request,
                RunningTask workerTask, OperationResult result) throws CommonException, PreconditionViolationException {
            synchronizer.synchronize(object, request.getIdentifier(), workerTask, result);
            return true;
        }
    }
}
