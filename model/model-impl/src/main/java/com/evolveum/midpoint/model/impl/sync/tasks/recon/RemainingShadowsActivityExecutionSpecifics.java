/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.sync.tasks.recon;

import static com.evolveum.midpoint.util.MiscUtil.stateCheck;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ReconciliationWorkStateType.F_RESOURCE_OBJECTS_RECONCILIATION_START_TIMESTAMP;

import java.util.Collection;
import javax.xml.datatype.XMLGregorianCalendar;

import com.google.common.annotations.VisibleForTesting;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.repo.common.activity.state.ActivityState;
import com.evolveum.midpoint.repo.common.task.ActivityReportingOptions;
import com.evolveum.midpoint.repo.common.task.ItemProcessingRequest;
import com.evolveum.midpoint.repo.common.task.SearchBasedActivityExecution;
import com.evolveum.midpoint.repo.common.task.work.ItemDefinitionProvider;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FetchErrorReportingMethodType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReconciliationWorkStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Scans shadows for unfinished operations and tries to finish them.
 */
class RemainingShadowsActivityExecutionSpecifics
        extends PartialReconciliationActivityExecutionSpecifics {

    private static final Trace LOGGER = TraceManager.getTrace(RemainingShadowsActivityExecutionSpecifics.class);

    RemainingShadowsActivityExecutionSpecifics(@NotNull SearchBasedActivityExecution<ShadowType, ReconciliationWorkDefinition,
            ReconciliationActivityHandler, ?> activityExecution) {
        super(activityExecution);
    }

    @Override
    public boolean doesRequireDirectRepositoryAccess() {
        return true;
    }

    @Override
    public @NotNull ActivityReportingOptions getDefaultReportingOptions() {
        return new ActivityReportingOptions()
                .enableActionsExecutedStatistics(true)
                .enableSynchronizationStatistics(false);
        // TODO We will eventually want to provide sync statistics even for this part, in order to see transitions
        //  to DELETED situation. Unfortunately, now it's not possible, because we limit sync stats to the directly
        //  invoked change processing.
    }

    /**
     * We ignore other parameters like kind, intent or object class. This is a behavior inherited from pre-4.4.
     * TODO change it!
     */
    @Override
    public ObjectQuery customizeQuery(ObjectQuery configuredQuery, OperationResult result)
            throws SchemaException, ObjectNotFoundException {
        return getBeans().prismContext.queryFor(ShadowType.class)
                .block()
                    .item(ShadowType.F_FULL_SYNCHRONIZATION_TIMESTAMP).le(getReconciliationStartTimestamp(result))
                    .or().item(ShadowType.F_FULL_SYNCHRONIZATION_TIMESTAMP).isNull()
                .endBlock()
                    .and().item(ShadowType.F_RESOURCE_REF).ref(objectClassSpec.getResourceOid())
                    .and().item(ShadowType.F_OBJECT_CLASS).eq(objectClassSpec.getObjectClassDefinitionRequired().getTypeName())
                .build();
    }

    private @NotNull XMLGregorianCalendar getReconciliationStartTimestamp(OperationResult opResult)
            throws SchemaException, ObjectNotFoundException {
        ActivityState reconState = getActivityState().
                getParentActivityState(ReconciliationWorkStateType.COMPLEX_TYPE, opResult);
        XMLGregorianCalendar started =
                reconState.getWorkStatePropertyRealValue(F_RESOURCE_OBJECTS_RECONCILIATION_START_TIMESTAMP, XMLGregorianCalendar.class);
        stateCheck(started != null, "No reconciliation start timestamp in %s", reconState);
        return started;
    }

    // Ignoring configured search options. TODO ok?
    @Override
    public Collection<SelectorOptions<GetOperationOptions>> customizeSearchOptions(
            Collection<SelectorOptions<GetOperationOptions>> configuredOptions, OperationResult result) {
        return getBeans().schemaService.getOperationOptionsBuilder()
                .errorReportingMethod(FetchErrorReportingMethodType.FETCH_RESULT)
                .build();
    }

    @Override
    public ItemDefinitionProvider createItemDefinitionProvider() {
        return ItemDefinitionProvider.forObjectClassAttributes(objectClassSpec.getObjectClassDefinitionRequired());
    }

    @Override
    public boolean processObject(@NotNull PrismObject<ShadowType> shadow,
            @NotNull ItemProcessingRequest<PrismObject<ShadowType>> request,
            RunningTask workerTask, OperationResult result)
            throws CommonException {

        if (!objectsFilter.matches(shadow)) {
            result.recordNotApplicable();
            return true;
        }

        reconcileShadow(shadow, workerTask, result);
        return true;
    }

    private void reconcileShadow(PrismObject<ShadowType> shadow, Task task, OperationResult result)
            throws SchemaException, SecurityViolationException, CommunicationException,
            ConfigurationException, ExpressionEvaluationException, ObjectNotFoundException {
        LOGGER.trace("Reconciling shadow {}, fullSynchronizationTimestamp={}", shadow,
                shadow.asObjectable().getFullSynchronizationTimestamp());
        try {
            Collection<SelectorOptions<GetOperationOptions>> options;
            if (activityExecution.isDryRun()) {
                options = SelectorOptions.createCollection(GetOperationOptions.createDoNotDiscovery());
            } else {
                options = SelectorOptions.createCollection(GetOperationOptions.createForceRefresh());
            }
            getModelBeans().provisioningService.getObject(ShadowType.class, shadow.getOid(), options, task, result);
            // In normal case, we do not get ObjectNotFoundException. The provisioning simply discovers that the shadow
            // does not exist on the resource, and invokes the discovery that marks the shadow as dead and synchronizes it.
        } catch (ObjectNotFoundException e) {
            result.muteLastSubresultError();
            reactShadowGone(shadow, task, result);
        }
    }

    private void reactShadowGone(PrismObject<ShadowType> shadow, Task task, OperationResult result) throws SchemaException,
            ObjectNotFoundException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        getModelBeans().provisioningService.applyDefinition(shadow, task, result);
        ResourceObjectShadowChangeDescription change = new ResourceObjectShadowChangeDescription();
        change.setSourceChannel(QNameUtil.qNameToUri(SchemaConstants.CHANNEL_RECON));
        change.setResource(objectClassSpec.getResource().asPrismObject());
        ObjectDelta<ShadowType> shadowDelta = shadow.getPrismContext().deltaFactory().object()
                .createDeleteDelta(ShadowType.class, shadow.getOid());
        change.setObjectDelta(shadowDelta);
        change.setShadowedResourceObject(shadow);
        ModelImplUtils.clearRequestee(task);
        getModelBeans().eventDispatcher.notifyChange(change, task, result);
    }

    @VisibleForTesting
    public long getShadowReconCount() {
        return activityExecution.getTransientExecutionStatistics().getItemsProcessed();
    }
}
