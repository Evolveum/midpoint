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

import com.evolveum.midpoint.prism.path.ItemName;

import com.evolveum.midpoint.schema.SchemaService;

import com.evolveum.midpoint.schema.util.ShadowUtil;

import com.google.common.annotations.VisibleForTesting;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.prism.PrismObject;
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

        // We doing dry run or simulation, we must look after synchronizationTimestamp, because this is the one that
        // is updated in resource objects activity in dry run or simulation mode. However, when doing execution,
        // we look after fullSynchronizationTimestamp.
        //
        // Besides being more logical, this allows us to run both simulation and execution in a single reconciliation activity:
        // simulation sets synchronization timestamps, keeping full sync timestamps intact. So this one can be used in
        // the execution activities to distinguish between shadows seen and not seen.
        ItemName syncTimestampItem =
                activityExecution.isExecute() ?
                        ShadowType.F_FULL_SYNCHRONIZATION_TIMESTAMP :
                        ShadowType.F_SYNCHRONIZATION_TIMESTAMP;

        return getBeans().prismContext.queryFor(ShadowType.class)
                .block()
                    .item(syncTimestampItem).le(getReconciliationStartTimestamp(result))
                    .or().item(syncTimestampItem).isNull()
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

    /**
     * Originally we relied on provisioning discovery mechanism to handle objects that couldn't be found on the resource.
     * However, in order to detect errors in the processing, we need to have more strict control over the process:
     * the result must not be marked as `HANDLED_ERROR` as it's currently the case in provisioning handling.
     */
    private void reconcileShadow(PrismObject<ShadowType> shadow, Task task, OperationResult result)
            throws SchemaException, SecurityViolationException, CommunicationException,
            ConfigurationException, ExpressionEvaluationException, ObjectNotFoundException {
        LOGGER.trace("Reconciling shadow {}, fullSynchronizationTimestamp={}", shadow,
                shadow.asObjectable().getFullSynchronizationTimestamp());
        try {
            Collection<SelectorOptions<GetOperationOptions>> options =
                    SchemaService.get().getOperationOptionsBuilder()
                            .doNotDiscovery() // We are doing "discovery" ourselves
                            .errorReportingMethod(FetchErrorReportingMethodType.FORCED_EXCEPTION) // As well as complete handling!
                            .forceRefresh(!activityExecution.isDryRun())
                            .build();
            getModelBeans().provisioningService.getObject(ShadowType.class, shadow.getOid(), options, task, result);
        } catch (ObjectNotFoundException e) {
            handleObjectNotFoundException(shadow, e, task, result);
        }
    }

    private void handleObjectNotFoundException(PrismObject<ShadowType> shadow, ObjectNotFoundException e, Task task,
            OperationResult result) throws SchemaException, ObjectNotFoundException, CommunicationException,
            ConfigurationException, ExpressionEvaluationException {
        if (!shadow.getOid().equals(e.getOid())) {
            LOGGER.debug("Got unrelated ObjectNotFoundException, rethrowing: " + e.getMessage(), e);
            throw e;
        }

        LOGGER.debug("We have a shadow that seemingly does not exist on the resource. Will handle that.");

        result.muteLastSubresultError();

        if (ShadowUtil.isDead(shadow) || !ShadowUtil.isExists(shadow)) {
            LOGGER.debug("Shadow already marked as dead and/or not existing. "
                    + "DELETE notification will not be issued. Shadow: {}", shadow);
            return;
        }

        reactShadowGone(shadow, task, result);
    }

    private void reactShadowGone(PrismObject<ShadowType> originalShadow, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException {

        // We reload e.g. to get current tombstone status. Otherwise the clockwork is confused.
        PrismObject<ShadowType> shadow = reloadShadow(originalShadow, result);

        getModelBeans().provisioningService.applyDefinition(shadow, task, result);

        ResourceObjectShadowChangeDescription change = new ResourceObjectShadowChangeDescription();
        change.setSourceChannel(QNameUtil.qNameToUri(SchemaConstants.CHANNEL_RECON));
        change.setResource(objectClassSpec.getResource().asPrismObject());
        change.setObjectDelta(shadow.createDeleteDelta());
        change.setShadowedResourceObject(shadow);
        change.setSimulate(activityExecution.isSimulate());
        ModelImplUtils.clearRequestee(task);
        getModelBeans().eventDispatcher.notifyChange(change, task, result);
    }

    private PrismObject<ShadowType> reloadShadow(PrismObject<ShadowType> originalShadow, OperationResult result)
            throws SchemaException {
        try {
            return getBeans().repositoryService.getObject(ShadowType.class, originalShadow.getOid(), null, result);
        } catch (ObjectNotFoundException e) {
            // TODO Could be the shadow deleted during preprocessing?
            //  Try to find out if it can occur.
            LOGGER.debug("Shadow disappeared. But we need to notify the model! Shadow: {}", originalShadow);

            originalShadow.asObjectable().setDead(true);
            originalShadow.asObjectable().setExists(false);
            return originalShadow;
        }
    }

    @VisibleForTesting
    public long getShadowReconCount() {
        return activityExecution.getTransientExecutionStatistics().getItemsProcessed();
    }
}
