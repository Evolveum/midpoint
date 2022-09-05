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

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.google.common.annotations.VisibleForTesting;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunInstantiationContext;
import com.evolveum.midpoint.repo.common.activity.run.state.ActivityState;
import com.evolveum.midpoint.repo.common.activity.run.ActivityReportingCharacteristics;
import com.evolveum.midpoint.repo.common.activity.run.processing.ItemProcessingRequest;
import com.evolveum.midpoint.repo.common.activity.run.buckets.ItemDefinitionProvider;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SchemaService;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * Scans shadows for unfinished operations and tries to finish them.
 */
final class RemainingShadowsActivityRun
        extends PartialReconciliationActivityRun {

    private static final Trace LOGGER = TraceManager.getTrace(RemainingShadowsActivityRun.class);

    RemainingShadowsActivityRun(
            @NotNull ActivityRunInstantiationContext<ReconciliationWorkDefinition, ReconciliationActivityHandler> context,
            String shortNameCapitalized) {
        super(context, shortNameCapitalized);
        setInstanceReady();
    }

    @Override
    public boolean doesRequireDirectRepositoryAccess() {
        return true;
    }

    @Override
    public @NotNull ActivityReportingCharacteristics createReportingCharacteristics() {
        return super.createReportingCharacteristics()
                .actionsExecutedStatisticsSupported(true)
                .synchronizationStatisticsSupported(true);
    }

    /**
     * We ignore other parameters like kind, intent or object class. This is a behavior inherited from pre-4.4.
     * TODO change it!
     */
    @Override
    public ObjectQuery customizeQuery(ObjectQuery configuredQuery, OperationResult result)
            throws SchemaException, ObjectNotFoundException {

        // We doing dry run or preview, we must look after synchronizationTimestamp, because this is the one that
        // is updated in resource objects activity in dry run or preview mode. However, when doing execution,
        // we look after fullSynchronizationTimestamp.
        //
        // Besides being more logical, this allows us to run both preview and execution in a single reconciliation activity:
        // preview sets synchronization timestamps, keeping full sync timestamps intact. So this one can be used in
        // the execution activities to distinguish between shadows seen and not seen.
        ItemName syncTimestampItem =
                isFullExecution() ?
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
    public boolean processItem(@NotNull ShadowType shadow,
            @NotNull ItemProcessingRequest<ShadowType> request,
            RunningTask workerTask, OperationResult result)
            throws CommonException {

        if (!objectsFilter.matches(shadow.asPrismObject())) {
            result.recordNotApplicable();
            return true;
        }

        reconcileShadow(shadow, request.getIdentifier(), workerTask, result);
        return true;
    }

    /**
     * Originally we relied on provisioning discovery mechanism to handle objects that couldn't be found on the resource.
     * However, in order to detect errors in the processing, we need to have more strict control over the process:
     * the result must not be marked as `HANDLED_ERROR` as it's currently the case in provisioning handling.
     */
    private void reconcileShadow(ShadowType shadow, String requestIdentifier, Task task, OperationResult result)
            throws SchemaException, SecurityViolationException, CommunicationException,
            ConfigurationException, ExpressionEvaluationException, ObjectNotFoundException {
        LOGGER.trace("Reconciling shadow {}, fullSynchronizationTimestamp={}", shadow,
                shadow.getFullSynchronizationTimestamp());
        try {
            Collection<SelectorOptions<GetOperationOptions>> options =
                    SchemaService.get().getOperationOptionsBuilder()
                            .doNotDiscovery() // We are doing "discovery" ourselves
                            .errorReportingMethod(FetchErrorReportingMethodType.FORCED_EXCEPTION) // As well as complete handling!
                            .forceRefresh(!isDryRun())
                            .readOnly()
                            .build();
            PrismObject<ShadowType> shadowFetched =
                    getModelBeans().provisioningService.getObject(ShadowType.class, shadow.getOid(), options, task, result);
            handleNoException(shadowFetched, requestIdentifier, task, result);
        } catch (ObjectNotFoundException e) {
            handleObjectNotFoundException(shadow, requestIdentifier, e, task, result);
        }
    }

    private void handleNoException(
            PrismObject<ShadowType> shadowFetched, String requestIdentifier, Task task, OperationResult result) {
        // Here are e.g. protected shadows or tombstones. To keep the statistics reasonable, let us provide
        // a reason for synchronization exclusion.
        LOGGER.debug("ObjectNotFound was not thrown, so no need to issue DELETE sync event. Shadow: {}", shadowFetched);
        if (ShadowUtil.isProtected(shadowFetched)) {
            LOGGER.trace("Shadow is protected. Technically, signalling 'synchronization not needed' would be correct, "
                    + "but let's be more specific by providing the reason as 'protected'.");
            task.onSynchronizationExclusion(requestIdentifier, SynchronizationExclusionReasonType.PROTECTED);
            result.recordNotApplicable("Resource object exists (and it is protected)");
        } else {
            task.onSynchronizationExclusion(requestIdentifier, SynchronizationExclusionReasonType.SYNCHRONIZATION_NOT_NEEDED);
            result.recordNotApplicable("Resource object exists");
        }
    }

    private void handleObjectNotFoundException(
            ShadowType shadow,
            String requestIdentifier,
            ObjectNotFoundException e,
            Task task,
            OperationResult result) throws SchemaException, ObjectNotFoundException, CommunicationException,
            ConfigurationException, ExpressionEvaluationException, SecurityViolationException {
        if (!shadow.getOid().equals(e.getOid())) {
            LOGGER.debug("Got unrelated ObjectNotFoundException, rethrowing: " + e.getMessage(), e);
            throw e;
        }

        LOGGER.debug("We have a shadow that seemingly does not exist on the resource. Will handle that.");

        result.muteLastSubresultError();

        if (ShadowUtil.isDead(shadow) || !ShadowUtil.isExists(shadow)) {
            // Not sure when exactly this can occur.
            LOGGER.debug("Shadow already marked as dead and/or not existing. "
                    + "DELETE notification will not be issued. Shadow: {}", shadow);
            task.onSynchronizationExclusion(requestIdentifier, SynchronizationExclusionReasonType.SYNCHRONIZATION_NOT_NEEDED);
            result.recordNotApplicable("Shadow already marked dead and/or not existing");
            return;
        }

        reactShadowGone(shadow, requestIdentifier, task, result);
    }

    private void reactShadowGone(ShadowType originalShadow, String requestIdentifier, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException, SecurityViolationException {

        // We reload e.g. to get current "gone" status. Otherwise the clockwork is confused.
        PrismObject<ShadowType> shadow = reloadShadow(originalShadow, task, result);

        getModelBeans().provisioningService.applyDefinition(shadow, task, result);

        ResourceObjectShadowChangeDescription change = new ResourceObjectShadowChangeDescription();
        change.setSourceChannel(QNameUtil.qNameToUri(SchemaConstants.CHANNEL_RECON));
        change.setResource(objectClassSpec.getResource().asPrismObject());
        change.setObjectDelta(shadow.createDeleteDelta());
        change.setShadowedResourceObject(shadow);
        change.setSimulate(isPreview());
        change.setItemProcessingIdentifier(requestIdentifier); // To record synchronization state changes
        ModelImplUtils.clearRequestee(task);
        getModelBeans().eventDispatcher.notifyChange(change, task, result);
    }

    private PrismObject<ShadowType> reloadShadow(ShadowType originalShadow, Task task, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException {
        //noinspection CaughtExceptionImmediatelyRethrown
        try {
            // 1. not read-only because we modify the shadow afterwards
            // 2. using provisioning (not the repository) to get the lifecycle state;
            //    but using raw mode to avoid deleting dead shadows
            return getModelBeans().provisioningService.getObject(ShadowType.class, originalShadow.getOid(),
                    GetOperationOptions.createRawCollection(), task, result);
        } catch (ObjectNotFoundException e) {
            // TODO Could be the shadow deleted during preprocessing?
            //  Try to find out if it can occur.
            LOGGER.debug("Shadow disappeared. But we need to notify the model! Shadow: {}", originalShadow);

            originalShadow.setDead(true);
            originalShadow.setExists(false);
            originalShadow.setShadowLifecycleState(ShadowLifecycleStateType.TOMBSTONE);
            return originalShadow.asPrismObject();
        } catch (ExpressionEvaluationException | CommunicationException | SecurityViolationException | ConfigurationException e) {
            // These shouldn't occur, because we are going in NO FETCH mode. But they can; so let's just propagate them upwards.
            throw e;
        }
    }

    @VisibleForTesting
    long getShadowReconCount() {
        return transientRunStatistics.getItemsProcessed();
    }
}
