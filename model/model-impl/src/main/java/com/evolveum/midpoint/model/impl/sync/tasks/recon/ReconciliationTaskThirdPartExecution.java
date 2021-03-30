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
import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.repo.common.task.*;
import com.evolveum.midpoint.schema.constants.SchemaConstants;

import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskUtil;
import com.evolveum.midpoint.util.QNameUtil;

import com.evolveum.midpoint.model.impl.tasks.AbstractIterativeModelTaskPartExecution;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.api.PreconditionViolationException;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FetchErrorReportingMethodType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import javax.xml.datatype.XMLGregorianCalendar;

/**
 * <p>Execution of shadow reconciliation (processes shadows that were not processed by the previous stage,
 * e.g. those that have been deleted).</p>
 *
 * TODO obtain "real" second stage start time, see getReconciliationStartTimestamp.
 */
@ItemProcessorClass(ReconciliationTaskThirdPartExecution.ItemProcessor.class)
@HandledObjectType(ShadowType.class)
class ReconciliationTaskThirdPartExecution
        extends AbstractIterativeModelTaskPartExecution
        <ShadowType,
                ReconciliationTaskHandler,
                ReconciliationTaskExecution,
                ReconciliationTaskThirdPartExecution,
                ReconciliationTaskThirdPartExecution.ItemProcessor> {

    ReconciliationTaskThirdPartExecution(ReconciliationTaskExecution taskExecution) {
        super(taskExecution);

        // TODO We will eventually want to provide sync statistics even for this part, in order to see transitions
        //  to DELETED situation. Unfortunately, now it's not possible, because we limit sync stats to the directly
        //  invoked change processing.
        reportingOptions.setEnableSynchronizationStatistics(false);

        setPartUri(ModelPublicConstants.RECONCILIATION_REMAINING_SHADOWS_PART_URI);
        setProcessShortNameCapitalized("Reconciliation (remaining shadows)");
        setContextDescription("for " + taskExecution.getTargetInfo().getContextDescription());
        setRequiresDirectRepositoryAccess();
    }

    @Override
    protected ObjectQuery createQuery(OperationResult opResult) throws SchemaException, ObjectNotFoundException,
            CommunicationException, ConfigurationException, ExpressionEvaluationException, SecurityViolationException {
        ObjectQuery initialQuery = getPrismContext().queryFor(ShadowType.class)
                .block()
                    .item(ShadowType.F_FULL_SYNCHRONIZATION_TIMESTAMP).le(getReconciliationStartTimestamp(opResult))
                    .or().item(ShadowType.F_FULL_SYNCHRONIZATION_TIMESTAMP).isNull()
                .endBlock()
                .and().item(ShadowType.F_RESOURCE_REF).ref(taskExecution.getResourceOid())
                .and().item(ShadowType.F_OBJECT_CLASS).eq(taskExecution.getObjectClassDefinition().getTypeName())
                .build();
        return taskExecution.createShadowQuery(initialQuery, opResult);
    }

    private XMLGregorianCalendar getReconciliationStartTimestamp(OperationResult result) throws SchemaException {
        Task rootTask = getRootTask(result);
        XMLGregorianCalendar lastReconStart = rootTask.getExtensionPropertyRealValue(
                SchemaConstants.MODEL_EXTENSION_LAST_RECONCILIATION_START_TIMESTAMP_PROPERTY_NAME);
        if (lastReconStart != null) {
            logger.trace("Last reconciliation start time: {}, determined from the extension of {}", lastReconStart, rootTask);
            return lastReconStart;
        } else {
            XMLGregorianCalendar implicitLastReconStart = taskExecution.startTimestamp;
            logger.trace("Last reconciliation start time: {}, determined as start timestamp of the respective part in {}",
                    implicitLastReconStart, localCoordinatorTask);
            return implicitLastReconStart;
        }
    }

    @Override
    protected Collection<SelectorOptions<GetOperationOptions>> createSearchOptions(OperationResult opResult) {
        return taskHandler.schemaService.getOperationOptionsBuilder()
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
        taskExecution.reconResult.setShadowReconCount(bucketStatistics.getItemsProcessed());
    }

    protected static class ItemProcessor
            extends AbstractSearchIterativeItemProcessor
            <ShadowType, ReconciliationTaskHandler, ReconciliationTaskExecution, ReconciliationTaskThirdPartExecution, ItemProcessor> {

        public ItemProcessor(ReconciliationTaskThirdPartExecution partExecution) {
            super(partExecution);
        }

        @Override
        protected boolean processObject(PrismObject<ShadowType> shadow,
                ItemProcessingRequest<PrismObject<ShadowType>> request,
                RunningTask workerTask, OperationResult result)
                throws CommonException, PreconditionViolationException {
            if (!taskExecution.objectsFilter.matches(shadow)) {
                result.recordNotApplicable();
                return true;
            }

            reconcileShadow(shadow, workerTask, result);
            return true;
        }

        private void reconcileShadow(PrismObject<ShadowType> shadow, Task task, OperationResult result)
                throws SchemaException, SecurityViolationException, CommunicationException,
                ConfigurationException, ExpressionEvaluationException, ObjectNotFoundException {
            logger.trace("Reconciling shadow {}, fullSynchronizationTimestamp={}", shadow,
                    shadow.asObjectable().getFullSynchronizationTimestamp());
            try {
                Collection<SelectorOptions<GetOperationOptions>> options;
                if (TaskUtil.isDryRun(task)) {
                    options = SelectorOptions.createCollection(GetOperationOptions.createDoNotDiscovery());
                } else {
                    options = SelectorOptions.createCollection(GetOperationOptions.createForceRefresh());
                }
                taskHandler.getProvisioningService().getObject(ShadowType.class, shadow.getOid(), options, task, result);
                // In normal case, we do not get ObjectNotFoundException. The provisioning simply discovers that the shadow
                // does not exist on the resource, and invokes the discovery that marks the shadow as dead and synchronizes it.
            } catch (ObjectNotFoundException e) {
                result.muteLastSubresultError();
                reactShadowGone(shadow, task, result);
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
            change.setShadowedResourceObject(shadow);
            ModelImplUtils.clearRequestee(task);
            taskHandler.eventDispatcher.notifyChange(change, task, result);
        }
    }
}
