/*

 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.sync;

import static com.evolveum.midpoint.common.SynchronizationUtils.createSynchronizationSituationAndDescriptionDelta;
import static com.evolveum.midpoint.prism.PrismObject.asObjectable;
import static com.evolveum.midpoint.schema.internals.InternalsConfig.consistencyChecks;

import java.util.List;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.common.SynchronizationUtils;
import com.evolveum.midpoint.model.api.correlator.CorrelationResult;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.sync.reactions.SynchronizationActionExecutor;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.delta.builder.S_ItemEntry;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Synchronization service receives change notifications from provisioning. It
 * decides which synchronization policy to use and evaluates it (correlation,
 * confirmation, situations, reaction, ...)
 * <p>
 * Note: don't autowire this bean by implementing class, as it is
 * proxied by Spring AOP. Use the interface instead.
 *
 * TODO improve the error handling for the whole class
 *
 * @author lazyman
 * @author Radovan Semancik
 */
@Service(value = "synchronizationService")
public class SynchronizationServiceImpl implements SynchronizationService {

    private static final Trace LOGGER = TraceManager.getTrace(SynchronizationServiceImpl.class);

    private static final String CLASS_NAME_WITH_DOT = SynchronizationServiceImpl.class.getName() + ".";

    private static final String OP_SETUP_SITUATION = CLASS_NAME_WITH_DOT + "setupSituation";
    private static final String OP_NOTIFY_CHANGE = CLASS_NAME_WITH_DOT + "notifyChange";

    @Autowired private SynchronizationExpressionsEvaluator synchronizationExpressionsEvaluator;
    @Autowired private PrismContext prismContext;
    @Autowired private Clock clock;
    @Autowired private ModelBeans beans;
    @Autowired private SynchronizationContextLoader synchronizationContextLoader;

    @Autowired
    @Qualifier("cacheRepositoryService")
    private RepositoryService repositoryService;

    @Override
    public void notifyChange(
            @NotNull ResourceObjectShadowChangeDescription change,
            @NotNull Task task,
            @NotNull OperationResult parentResult) {

        OperationResult result = parentResult.subresult(OP_NOTIFY_CHANGE)
                .addArbitraryObjectAsParam("change", change)
                .addArbitraryObjectAsContext("task", task)
                .build();

        try {
            logStart(change);
            checkConsistence(change);

            SynchronizationContext<?> syncCtx = synchronizationContextLoader.
                    loadSynchronizationContextFromChange(change, task, result);

            if (shouldSkipSynchronization(syncCtx, result)) {
                return;
            }

            assert syncCtx.getSynchronizationPolicy() != null;

            setupLinkedOwnerAndSituation(syncCtx, change, result);

            task.onSynchronizationStart(change.getItemProcessingIdentifier(), change.getShadowOid(), syncCtx.getSituation());

            ExecutionModeType executionMode = TaskUtil.getExecutionMode(task);
            boolean fullSync = executionMode == ExecutionModeType.FULL;
            boolean dryRun = executionMode == ExecutionModeType.DRY_RUN;

            saveSyncMetadata(syncCtx, change, fullSync, result);

            if (!dryRun) {
                new SynchronizationActionExecutor<>(syncCtx, change)
                        .react(result);
                // Note that exceptions from action execution are not propagated here.
            }

            LOGGER.debug("SYNCHRONIZATION: DONE (mode '{}') for {}", executionMode, change.getShadowedResourceObject());

        } catch (SystemException ex) {
            // avoid unnecessary re-wrap
            result.recordFatalError(ex);
            throw ex;
        } catch (Exception ex) {
            result.recordFatalError(ex);
            throw new SystemException(ex);
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    private void logStart(@NotNull ResourceObjectShadowChangeDescription change) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("SYNCHRONIZATION: received change notification:\n{}", DebugUtil.debugDump(change, 1));
        } else if (isLogDebug(change)) {
            LOGGER.debug("SYNCHRONIZATION: received change notification {}", change);
        }
    }

    /**
     * TODO: Consider situations when one account belongs to two different users. It should correspond to
     *  the {@link SynchronizationSituationType#DISPUTED} situation.
     */
    @Nullable
    private PrismObject<FocusType> findShadowOwner(String shadowOid, OperationResult result) throws SchemaException {
        ObjectQuery query = prismContext.queryFor(FocusType.class)
                .item(FocusType.F_LINK_REF).ref(shadowOid, null, PrismConstants.Q_ANY)
                .build();
        // TODO read-only later
        SearchResultList<PrismObject<FocusType>> owners =
                repositoryService.searchObjects(FocusType.class, query, null, result);

        if (owners.isEmpty()) {
            return null;
        }
        if (owners.size() > 1) {
            LOGGER.warn("Found {} owners for shadow oid {}, returning first owner.", owners.size(), shadowOid);
        }
        return owners.get(0);
    }

    /**
     * Checks for common reasons to skip synchronization:
     *
     * - no applicable synchronization policy,
     * - synchronization disabled,
     * - protected resource object.
     */
    private <F extends FocusType> boolean shouldSkipSynchronization(SynchronizationContext<F> syncCtx,
            OperationResult result) throws SchemaException {
        Task task = syncCtx.getTask();

        if (!syncCtx.hasApplicablePolicy()) {
            String message = "SYNCHRONIZATION no matching policy for " + syncCtx.getShadowedResourceObject() + " ("
                    + syncCtx.getShadowedResourceObject().getObjectClass() + ") " + " on " + syncCtx.getResource()
                    + ", ignoring change from channel " + syncCtx.getChannel();
            LOGGER.debug(message);
            List<PropertyDelta<?>> modifications = createShadowIntentAndSynchronizationTimestampDelta(syncCtx, false); // TODO record always full sync?
            executeShadowModifications(syncCtx.getShadowedResourceObject(), modifications, task, result);
            result.recordStatus(OperationResultStatus.NOT_APPLICABLE, message);
            task.onSynchronizationExclusion(syncCtx.getItemProcessingIdentifier(), SynchronizationExclusionReasonType.NO_SYNCHRONIZATION_POLICY);
            return true;
        }

        if (!syncCtx.isSynchronizationEnabled()) {
            String message = "SYNCHRONIZATION is not enabled for " + syncCtx.getResource()
                    + " ignoring change from channel " + syncCtx.getChannel();
            LOGGER.debug(message);
            List<PropertyDelta<?>> modifications = createShadowIntentAndSynchronizationTimestampDelta(syncCtx, true); // TODO record always full sync?
            executeShadowModifications(syncCtx.getShadowedResourceObject(), modifications, task, result);
            result.recordStatus(OperationResultStatus.NOT_APPLICABLE, message);
            task.onSynchronizationExclusion(syncCtx.getItemProcessingIdentifier(), SynchronizationExclusionReasonType.SYNCHRONIZATION_DISABLED);
            return true;
        }

        if (syncCtx.isProtected()) {
            List<PropertyDelta<?>> modifications = createShadowIntentAndSynchronizationTimestampDelta(syncCtx, true); // TODO record always full sync?
            executeShadowModifications(syncCtx.getShadowedResourceObject(), modifications, task, result);
            result.recordSuccess(); // Maybe "not applicable" would be better (it is so in Synchronizer class)
            task.onSynchronizationExclusion(syncCtx.getItemProcessingIdentifier(), SynchronizationExclusionReasonType.PROTECTED);
            LOGGER.debug("SYNCHRONIZATION: DONE for protected shadow {}", syncCtx.getShadowedResourceObject());
            return true;
        }

        return false;
    }

    private <F extends FocusType> List<PropertyDelta<?>> createShadowIntentAndSynchronizationTimestampDelta(
            SynchronizationContext<F> syncCtx, boolean saveIntent) throws SchemaException {
        Validate.notNull(syncCtx.getShadowedResourceObject(), "No current nor old shadow present");
        ShadowType applicableShadow = syncCtx.getShadowedResourceObject();
        PrismObject<ShadowType> shadowObject = syncCtx.getShadowedResourceObject().asPrismObject();
        List<PropertyDelta<?>> modifications = SynchronizationUtils.createSynchronizationTimestampsDeltas(shadowObject);
        if (saveIntent) {
            if (StringUtils.isNotBlank(syncCtx.getIntent()) && !syncCtx.getIntent().equals(applicableShadow.getIntent())) {
                PropertyDelta<String> intentDelta = prismContext.deltaFactory().property().createModificationReplaceProperty(ShadowType.F_INTENT,
                        shadowObject.getDefinition(), syncCtx.getIntent());
                modifications.add(intentDelta);
            }
            if (StringUtils.isNotBlank(syncCtx.getTag()) && !syncCtx.getTag().equals(applicableShadow.getTag())) {
                PropertyDelta<String> tagDelta = prismContext.deltaFactory().property().createModificationReplaceProperty(ShadowType.F_TAG,
                        shadowObject.getDefinition(), syncCtx.getTag());
                modifications.add(tagDelta);
            }
        }
        return modifications;
    }

    private void executeShadowModifications(ShadowType object, List<PropertyDelta<?>> modifications,
            Task task, OperationResult subResult) {
        try {
            repositoryService.modifyObject(ShadowType.class, object.getOid(), modifications, subResult);
            task.recordObjectActionExecuted(object.asPrismObject(), ChangeType.MODIFY, null);
        } catch (Throwable t) {
            task.recordObjectActionExecuted(object.asPrismObject(), ChangeType.MODIFY, t);
        }
    }

    private void checkConsistence(ResourceObjectShadowChangeDescription change) {
        Validate.notNull(change, "Resource object shadow change description must not be null.");
        Validate.notNull(change.getShadowedResourceObject(), "Current shadow must not be null.");
        Validate.notNull(change.getResource(), "Resource in change must not be null.");

        if (consistencyChecks) {
            change.checkConsistence();
        }
    }

    private <F extends FocusType> void setupLinkedOwnerAndSituation(SynchronizationContext<F> syncCtx,
            ResourceObjectShadowChangeDescription change, OperationResult parentResult) throws SchemaException {

        OperationResult result = parentResult.subresult(OP_SETUP_SITUATION)
                .setMinor()
                .addArbitraryObjectAsParam("syncCtx", syncCtx)
                .addArbitraryObjectAsParam("change", change)
                .build();

        LOGGER.trace("Determining situation for resource object shadow. Focus class: {}. Applicable policy: {}.",
                syncCtx.getFocusClass(), syncCtx.getPolicyName());

        try {
            findLinkedOwner(syncCtx, change, result);

            if (syncCtx.getLinkedOwner() == null || syncCtx.isCorrelatorsUpdateRequested()) {
                determineSituationWithCorrelators(syncCtx, change, result); // TODO change the name (if sorter is used)
            } else {
                determineSituationWithoutCorrelators(syncCtx, change, result);
            }
            logSituation(syncCtx, change);
        } catch (Exception ex) {
            result.recordFatalError(ex);
            LOGGER.error("Error occurred during resource object shadow owner lookup.");
            throw new SystemException(
                    "Error occurred during resource object shadow owner lookup, reason: " + ex.getMessage(), ex);
        } finally {
            result.close();
        }
    }

    private <F extends FocusType> void findLinkedOwner(SynchronizationContext<F> syncCtx,
            ResourceObjectShadowChangeDescription change, OperationResult result) throws SchemaException {

        if (syncCtx.getLinkedOwner() != null) {
            // TODO This never occurs. Clarify!
            return;
        }

        PrismObject<FocusType> owner = findShadowOwner(change.getShadowOid(), result);
        //noinspection unchecked
        syncCtx.setLinkedOwner((F) asObjectable(owner));
    }

    private <F extends FocusType> void determineSituationWithoutCorrelators(SynchronizationContext<F> syncCtx,
            ResourceObjectShadowChangeDescription change, OperationResult result) throws ConfigurationException {

        assert syncCtx.getLinkedOwner() != null;

        checkLinkedAndCorrelatedOwnersMatch(syncCtx, result);

        if (change.isDelete()) {
            syncCtx.setSituationIfNull(SynchronizationSituationType.DELETED);
        } else {
            syncCtx.setSituationIfNull(SynchronizationSituationType.LINKED);
        }
    }

    private <F extends FocusType> void checkLinkedAndCorrelatedOwnersMatch(SynchronizationContext<F> syncCtx,
            OperationResult result) throws ConfigurationException {
        F linkedOwner = syncCtx.getLinkedOwner();
        F correlatedOwner = syncCtx.getCorrelatedOwner(); // may be null; or may be provided by sync sorter

        LOGGER.trace("Shadow {} has linked owner: {}, correlated owner: {}", syncCtx.getShadowedResourceObject(),
                linkedOwner, correlatedOwner);

        if (correlatedOwner != null && linkedOwner != null && !correlatedOwner.getOid().equals(linkedOwner.getOid())) {
            LOGGER.error("Cannot synchronize {}, linked owner and expected owner are not the same. "
                    + "Linked owner: {}, expected owner: {}", syncCtx.getShadowedResourceObject(), linkedOwner, correlatedOwner);
            String msg = "Cannot synchronize " + syncCtx.getShadowedResourceObject()
                    + ", linked owner and expected owner are not the same. Linked owner: " + linkedOwner
                    + ", expected owner: " + correlatedOwner;
            result.recordFatalError(msg);
            throw new ConfigurationException(msg);
        }
    }

    /**
     * EITHER (todo update the description):
     *
     * account is not linked to user. you have to use correlation and
     * confirmation rule to be sure user for this account doesn't exists
     * resourceShadow only contains the data that were in the repository before
     * the change. But the correlation/confirmation should work on the updated
     * data. Therefore let's apply the changes before running
     * correlation/confirmation
     *
     * OR
     *
     * We need to update the correlator state.
     */
    private <F extends FocusType> void determineSituationWithCorrelators(SynchronizationContext<F> syncCtx,
            ResourceObjectShadowChangeDescription change, OperationResult result)
            throws CommonException {

        if (change.isDelete()) {
            // account was deleted and it was not linked; there is nothing to do (not even updating the correlators)
            syncCtx.setSituationIfNull(SynchronizationSituationType.DELETED);
            return;
        }

        if (!syncCtx.isCorrelatorsUpdateRequested() && syncCtx.getCorrelatedOwner() != null) { // e.g. from sync sorter
            LOGGER.trace("Correlated owner present in synchronization context: {}", syncCtx.getCorrelatedOwner());
            syncCtx.setSituationIfNull(SynchronizationSituationType.UNLINKED);
            return;
        }

        PrismObject<? extends ShadowType> resourceObject = change.getShadowedResourceObject();
        ResourceType resource = change.getResource().asObjectable();
        setupResourceRefInShadowIfNeeded(resourceObject.asObjectable(), resource);

        evaluatePreMappings(syncCtx, result);

        if (syncCtx.isUpdatingCorrelatorsOnly()) {
            new CorrelationProcessing<>(syncCtx, beans)
                    .update(result);
            return;
        }

        CorrelationResult correlationResult =
                new CorrelationProcessing<>(syncCtx, beans)
                        .correlate(result);

        LOGGER.debug("Correlation result:\n{}", correlationResult.debugDumpLazily(1));

        SynchronizationSituationType state;
        F owner;
        switch (correlationResult.getSituation()) {
            case EXISTING_OWNER:
                state = SynchronizationSituationType.UNLINKED;
                //noinspection unchecked
                owner = (F) correlationResult.getOwner();
                break;
            case NO_OWNER:
                state = SynchronizationSituationType.UNMATCHED;
                owner = null;
                break;
            case UNCERTAIN:
            case ERROR:
                state = SynchronizationSituationType.DISPUTED;
                owner = null;
                break;
            default:
                throw new AssertionError(correlationResult.getSituation());
        }
        LOGGER.debug("Determined synchronization situation: {} with owner: {}", state, owner);

        syncCtx.setCorrelatedOwner(owner);
        syncCtx.setSituationIfNull(state);

        if (correlationResult.isError()) {
            // This is a very crude and preliminary error handling: we just write pending deltas to the shadow
            // (if there are any), to have a record of the unsuccessful correlation. Normally, we should do this
            // along with the other sync metadata. But the error handling in this class is not ready for it (yet).
            savePendingDeltas(syncCtx, result);
            correlationResult.throwCommonOrRuntimeExceptionIfPresent();
            throw new AssertionError("Not here");
        }
    }

    private <F extends FocusType> void evaluatePreMappings(SynchronizationContext<F> syncCtx, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
            ConfigurationException, ObjectNotFoundException {
        new PreMappingsEvaluation<>(syncCtx, beans)
                .evaluate(result);
    }

    // This is maybe not needed
    private void setupResourceRefInShadowIfNeeded(ShadowType shadow, ResourceType resource) {
        if (shadow.getResourceRef() == null) {
            ObjectReferenceType reference = new ObjectReferenceType();
            reference.setOid(resource.getOid());
            reference.setType(ResourceType.COMPLEX_TYPE);
            shadow.setResourceRef(reference);
        }
    }

    private <F extends FocusType> void logSituation(SynchronizationContext<F> syncCtx, ResourceObjectShadowChangeDescription change) {
        String syncSituationValue = syncCtx.getSituation() != null ? syncCtx.getSituation().value() : null;
        if (isLogDebug(change)) {
            LOGGER.debug("SYNCHRONIZATION: SITUATION: '{}', currentOwner={}, correlatedOwner={}",
                    syncSituationValue, syncCtx.getLinkedOwner(),
                    syncCtx.getCorrelatedOwner());
        } else {
            LOGGER.trace("SYNCHRONIZATION: SITUATION: '{}', currentOwner={}, correlatedOwner={}",
                    syncSituationValue, syncCtx.getLinkedOwner(),
                    syncCtx.getCorrelatedOwner());
        }
    }

    /**
     * Saves situation, timestamps, kind and intent (if needed)
     *
     * @param full if true, we consider this synchronization to be "full", and set the appropriate flag
     * in `synchronizationSituationDescription` as well as update `fullSynchronizationTimestamp`.
     */
    private <F extends FocusType> void saveSyncMetadata(SynchronizationContext<F> syncCtx,
            ResourceObjectShadowChangeDescription change, boolean full, OperationResult result) {

        try {
            ShadowType shadow = syncCtx.getShadowedResourceObject();

            // new situation description
            XMLGregorianCalendar now = clock.currentTimeXMLGregorianCalendar();
            syncCtx.addShadowDeltas(
                    createSynchronizationSituationAndDescriptionDelta(
                            shadow.asPrismObject(), syncCtx.getSituation(), change.getSourceChannel(), full, now));

            S_ItemEntry builder = prismContext.deltaFor(ShadowType.class);

            if (ShadowUtil.isNotKnown(shadow.getKind())) {
                builder = builder.item(ShadowType.F_KIND).replace(syncCtx.getKind());
            }

            if (ShadowUtil.isNotKnown(shadow.getIntent()) ||
                    syncCtx.isForceIntentChange() &&
                            !shadow.getIntent().equals(syncCtx.getIntent())) {
                builder = builder.item(ShadowType.F_INTENT).replace(syncCtx.getIntent());
            }

            if (shadow.getTag() == null && syncCtx.getTag() != null) {
                builder = builder.item(ShadowType.F_TAG).replace(syncCtx.getTag());
            }

            syncCtx.addShadowDeltas(
                    builder.asItemDeltas());

            savePendingDeltas(syncCtx, result);
        } catch (SchemaException e) {
            throw SystemException.unexpected(e, "while preparing shadow modifications");
        }
    }

    private void savePendingDeltas(SynchronizationContext<?> syncCtx, OperationResult result) throws SchemaException {
        Task task = syncCtx.getTask();
        ShadowType shadow = syncCtx.getShadowedResourceObject();
        String channel = syncCtx.getChannel();

        try {
            beans.cacheRepositoryService.modifyObject(
                    ShadowType.class,
                    shadow.getOid(),
                    syncCtx.getPendingShadowDeltas(),
                    result);
            syncCtx.clearPendingShadowDeltas();
            task.recordObjectActionExecuted(shadow.asPrismObject(), null, null, ChangeType.MODIFY, channel, null);
        } catch (ObjectNotFoundException ex) {
            task.recordObjectActionExecuted(shadow.asPrismObject(), null, null, ChangeType.MODIFY, channel, ex);
            // This may happen e.g. during some recon-livesync interactions.
            // If the shadow is gone then it is gone. No point in recording the
            // situation any more.
            LOGGER.debug(
                    "Could not update situation in account, because shadow {} does not exist any more (this may be harmless)",
                    shadow.getOid());
            syncCtx.setShadowExistsInRepo(false);
            result.getLastSubresult().setStatus(OperationResultStatus.HANDLED_ERROR);
        } catch (ObjectAlreadyExistsException | SchemaException ex) {
            task.recordObjectActionExecuted(shadow.asPrismObject(), ChangeType.MODIFY, ex);
            LoggingUtils.logException(LOGGER,
                    "### SYNCHRONIZATION # notifyChange(..): Save of synchronization situation failed: could not modify shadow "
                            + shadow.getOid() + ": " + ex.getMessage(),
                    ex);
            result.recordFatalError("Save of synchronization situation failed: could not modify shadow "
                    + shadow.getOid() + ": " + ex.getMessage(), ex);
            throw new SystemException("Save of synchronization situation failed: could not modify shadow "
                    + shadow.getOid() + ": " + ex.getMessage(), ex);
        } catch (Throwable t) {
            task.recordObjectActionExecuted(shadow.asPrismObject(), ChangeType.MODIFY, t);
            throw t;
        }
    }

    @Override
    public String getName() {
        return "model synchronization service";
    }

    private static boolean isLogDebug(ResourceObjectShadowChangeDescription change) {
        // Reconciliation changes are routine. Do not let them pollute the log files.
        return !SchemaConstants.CHANNEL_RECON_URI.equals(change.getSourceChannel());
    }

    private static <F extends FocusType> boolean isLogDebug(SynchronizationContext<F> syncCtx) {
        return !SchemaConstants.CHANNEL_RECON_URI.equals(syncCtx.getChannel());
    }
}
