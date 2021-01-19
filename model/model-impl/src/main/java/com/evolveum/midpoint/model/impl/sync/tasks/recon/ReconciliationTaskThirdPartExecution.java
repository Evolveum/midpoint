/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.sync.tasks.recon;

import java.util.Collection;
import java.util.function.Function;

import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.schema.constants.SchemaConstants;

import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskUtil;
import com.evolveum.midpoint.util.QNameUtil;

import com.evolveum.midpoint.model.impl.tasks.AbstractSearchIterativeModelTaskPartExecution;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.api.PreconditionViolationException;
import com.evolveum.midpoint.repo.common.task.AbstractSearchIterativeResultHandler;
import com.evolveum.midpoint.repo.common.task.HandledObjectType;
import com.evolveum.midpoint.repo.common.task.ResultHandlerClass;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FetchErrorReportingMethodType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import javax.xml.datatype.XMLGregorianCalendar;

/**
 * Execution of shadow reconciliation (processes shadows that were not processed by the previous stage,
 * e.g. those that have been deleted).
 *
 * TODO obtain "real" second stage start time, see getReconciliationStartTimestamp.
 */
@ResultHandlerClass(ReconciliationTaskThirdPartExecution.Handler.class)
@HandledObjectType(ShadowType.class)
class ReconciliationTaskThirdPartExecution
        extends AbstractSearchIterativeModelTaskPartExecution
        <ShadowType,
                ReconciliationTaskHandler,
                ReconciliationTaskExecution,
                ReconciliationTaskThirdPartExecution,
                ReconciliationTaskThirdPartExecution.Handler> {

    private static final Trace LOGGER = TraceManager.getTrace(ReconciliationTaskHandler.class);

    ReconciliationTaskThirdPartExecution(ReconciliationTaskExecution taskExecution) {
        super(taskExecution);
    }

    @Override
    protected ObjectQuery createQuery(OperationResult opResult) throws SchemaException, ObjectNotFoundException,
            CommunicationException, ConfigurationException, ExpressionEvaluationException, SecurityViolationException {
        ObjectQuery initialQuery = getPrismContext().queryFor(ShadowType.class)
                .block()
                    .item(ShadowType.F_FULL_SYNCHRONIZATION_TIMESTAMP).le(getReconciliationStartTimestamp())
                    .or().item(ShadowType.F_FULL_SYNCHRONIZATION_TIMESTAMP).isNull()
                .endBlock()
                .and().item(ShadowType.F_RESOURCE_REF).ref(taskExecution.getResourceOid())
                .and().item(ShadowType.F_OBJECT_CLASS).eq(taskExecution.getObjectClassDefinition().getTypeName())
                .build();
        return taskExecution.createShadowQuery(initialQuery, opResult);
    }

    @Override
    protected boolean requiresDirectRepositoryAccess(OperationResult opResult) {
        return true;
    }

    private XMLGregorianCalendar getReconciliationStartTimestamp() {
        // TODO TODO TODO
        //  We should provide start timestamp of the start of the second stage.
        //  It could be present in a separate task (!)
        return taskExecution.startTimestamp;
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
        taskExecution.reconResult.setShadowReconCount(resultHandler.getProgress());
    }

    protected static class Handler
            extends AbstractSearchIterativeResultHandler
            <ShadowType,
                    ReconciliationTaskHandler,
                    ReconciliationTaskExecution,
                    ReconciliationTaskThirdPartExecution,
                    ReconciliationTaskThirdPartExecution.Handler> {

        public Handler(ReconciliationTaskThirdPartExecution partExecution) {
            super(partExecution);
        }

        @Override
        protected boolean handleObject(PrismObject<ShadowType> shadow, RunningTask workerTask, OperationResult result)
                throws CommonException, PreconditionViolationException {
            ShadowType shadowBean = shadow.asObjectable();
            if (!taskExecution.objectsFilter.matches(shadowBean)) {
                return true;
            }

            LOGGER.trace("Shadow reconciliation of {}, fullSynchronizationTimestamp={}", shadow,
                    shadowBean.getFullSynchronizationTimestamp());
            reconcileShadow(shadow, workerTask, result);
            return true;
        }

        private void reconcileShadow(PrismObject<ShadowType> shadow, Task task, OperationResult result)
                throws SchemaException, SecurityViolationException, CommunicationException,
                ConfigurationException, ExpressionEvaluationException, ObjectNotFoundException {
            try {
                Collection<SelectorOptions<GetOperationOptions>> options;
                if (TaskUtil.isDryRun(task)) {
                    options = SelectorOptions.createCollection(GetOperationOptions.createDoNotDiscovery());
                } else {
                    options = SelectorOptions.createCollection(GetOperationOptions.createForceRefresh());
                }
                taskHandler.getProvisioningService().getObject(ShadowType.class, shadow.getOid(), options, task, result);
            } catch (ObjectNotFoundException e) {
                result.muteLastSubresultError();
                // Account is gone
                reactShadowGone(shadow, task, result); // actually, for deleted objects here is the recon code called second time
            }
        }

        private void reactShadowGone(PrismObject<ShadowType> shadow, Task task, OperationResult result) throws SchemaException,
                ObjectNotFoundException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
            taskHandler.getProvisioningService().applyDefinition(shadow, task, result);
            ResourceObjectShadowChangeDescription change = new ResourceObjectShadowChangeDescription();
            change.setSourceChannel(QNameUtil.qNameToUri(SchemaConstants.CHANNEL_RECON));
            change.setResource(taskExecution.getResource());
            ObjectDelta<ShadowType> shadowDelta = shadow.getPrismContext().deltaFactory().object()
                    .createDeleteDelta(ShadowType.class, shadow.getOid());
            change.setObjectDelta(shadowDelta);
            change.setCurrentShadow(shadow);
            ModelImplUtils.clearRequestee(task);
            taskHandler.changeNotificationDispatcher.notifyChange(change, task, result);
        }
    }
}
