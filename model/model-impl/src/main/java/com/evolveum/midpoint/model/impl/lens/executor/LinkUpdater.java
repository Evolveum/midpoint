/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.executor;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.common.SynchronizationUtils;
import com.evolveum.midpoint.model.api.context.SynchronizationIntent;
import com.evolveum.midpoint.model.api.context.SynchronizationPolicyDecision;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.lens.*;
import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.provisioning.api.ProvisioningOperationContext;
import com.evolveum.midpoint.provisioning.api.ProvisioningOperationOptions;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.provisioning.api.ShadowLivenessState;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.SchemaService;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;

/**
 * Takes care of updating the focus <-> shadow links during change execution.
 * (That includes `linkRef` and `synchronizationSituation`.)
 */
class LinkUpdater<F extends FocusType> {

    /** Intentionally using the main class for logs. At least for now. */
    private static final Trace LOGGER = TraceManager.getTrace(ChangeExecutor.class);

    /** Intentionally using the main class for operation results. At least for now. */
    private static final String OP_UPDATE_LINKS = ChangeExecutor.class.getName() + ".updateLinks";
    private static final String OP_LINK_ACCOUNT = ChangeExecutor.class.getName() + ".linkShadow";
    private static final String OP_UNLINK_ACCOUNT = ChangeExecutor.class.getName() + ".unlinkShadow";
    private static final String OP_UPDATE_SITUATION_IN_SHADOW = ChangeExecutor.class.getName() + ".updateSituationInShadow";

    @NotNull private final LensContext<?> context;
    @NotNull private final LensFocusContext<F> focusContext;
    @NotNull private final LensProjectionContext projCtx;

    /** OID of the projection. Not null after initial checks. */
    private final String projectionOid;

    /** Current liveness state of the shadow. */
    private final ShadowLivenessState shadowLivenessState;

    @NotNull private final Task task;

    // This is an attempt how it looks like if beans are declared explicitly for the class.
    // (An alternative would be to have single 'beans' field.)
    @NotNull private final PrismContext prismContext;
    @NotNull private final SchemaService schemaService;
    @NotNull private final RepositoryService repositoryService;
    @NotNull private final ProvisioningService provisioningService;
    @NotNull private final Clock clock;

    /**
     * In strict mode we do not tolerate problems with determined liveness state (e.g. if it's null or not
     * consistent with the projection "gone" flag). TODO set to be true only in tests; or remove altogether.
     */
    @SuppressWarnings("FieldCanBeLocal")
    private final boolean strictMode = false;

    LinkUpdater(@NotNull LensContext<?> context, @NotNull LensFocusContext<F> focusContext,
            @NotNull LensProjectionContext projCtx, @Nullable ShadowLivenessState shadowLivenessState,
            @NotNull Task task, @NotNull ModelBeans beans) {

        this.context = context;
        this.focusContext = focusContext;
        this.projCtx = projCtx;
        this.projectionOid = projCtx.getOid();
        this.shadowLivenessState = shadowLivenessState;
        this.task = task;

        this.prismContext = beans.prismContext;
        this.schemaService = beans.schemaService;
        this.repositoryService = beans.cacheRepositoryService;
        this.provisioningService = beans.provisioningService;
        this.clock = beans.clock;
    }

    void updateLinks(OperationResult parentResult) throws SchemaException, ObjectNotFoundException, ConfigurationException {

        OperationResult result = parentResult.subresult(OP_UPDATE_LINKS)
                .setMinor()
                .build();

        try {
            if (checkOidPresent()) {
                return;
            }
            updateLinksInternal(result);

        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    private void updateLinksInternal(OperationResult result) throws ObjectNotFoundException, SchemaException {

        SynchronizationPolicyDecision decision = projCtx.getSynchronizationPolicyDecision();
        SynchronizationIntent intent = projCtx.getSynchronizationIntent();

        LOGGER.trace("updateLinksInternal starting with sync decision: {}, sync intent: {}, gone: {}, shadow in repo: {}",
                decision, intent, projCtx.isGone(), projCtx.isShadowExistsInRepo());

        if (focusContext.isDelete()) {

            LOGGER.trace("Nothing to link from, because focus is being deleted. But we need to update the situation in shadow.");
            updateSituationInShadow(null, false, result);

        } else if (!projCtx.isShadowExistsInRepo()) {

            LOGGER.trace("Nothing to link to, because the shadow is not in repository. Removing linkRef from focus.");
            deleteLinkRefFromFocus(result);

        } else if (decision == SynchronizationPolicyDecision.UNLINK) {

            LOGGER.trace("Explicitly requested link to be removed. So removing it from the focus and the shadow.");
            deleteLinkCompletely(result);

        } else if (projCtx.isGone()) {

            if (strictMode && shadowLivenessState == null) {
                throw new IllegalStateException("Null liveness state? " + projCtx.toHumanReadableString());
            }

            if (shadowLivenessState == null || shadowLivenessState == ShadowLivenessState.DEAD) {
                LOGGER.trace("Projection is gone. Link should be 'related'.");
                setLinkedAsRelated(result);
            } else {
                if (strictMode) {
                    throw new IllegalStateException("Goner with liveness state = " + shadowLivenessState + ": " +
                            projCtx.toHumanReadableString());
                } else {
                    LOGGER.warn("Projection is gone but shadow liveness state is {}. Context: {}. Setting the link "
                            + "according to the state.", shadowLivenessState, projCtx.toHumanReadableString());
                    setLinkedFromLivenessState(result);
                }
            }

        } else if (decision == SynchronizationPolicyDecision.IGNORE) {

            LOGGER.trace("Projection is ignored. Keeping link as is.");

        } else {

            if (strictMode && shadowLivenessState == null) {
                throw new IllegalStateException("Null liveness state? " + projCtx.toHumanReadableString());
            }

            setLinkedFromLivenessState(result);

        }
    }

    private void deleteLinkRefFromFocus(OperationResult result) throws ObjectNotFoundException, SchemaException {
        LOGGER.trace("Deleting linkRef from focus if present");
        List<ObjectReferenceType> matchingLinks = getMatchingLinks();
        if (matchingLinks.isEmpty()) {
            LOGGER.trace("Skipping. No matching links for {} found.", projectionOid);
            return;
        }

        LOGGER.debug("Deleting linkRef {} from focus {}", matchingLinks, focusContext.getObjectCurrent());
        ObjectDelta<F> delta = prismContext.deltaFor(focusContext.getObjectTypeClass())
                .item(FocusType.F_LINK_REF).deleteRealValues(CloneUtil.cloneCollectionMembers(matchingLinks))
                .asObjectDelta(focusContext.getOid());
        executeFocusDelta(delta, OP_UNLINK_ACCOUNT, result);
    }

    @NotNull
    private List<ObjectReferenceType> getMatchingLinks() {
        assert projectionOid != null;
        PrismObject<F> objectCurrent = focusContext.getObjectCurrent(); // We don't care about objectNew.
        if (objectCurrent != null) {
            return objectCurrent.asObjectable().getLinkRef().stream()
                    .filter(ref -> projectionOid.equals(ref.getOid()))
                    .collect(Collectors.toList());
        } else {
            return emptyList();
        }
    }

    private void executeFocusDelta(ObjectDelta<F> delta, String opName, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException {

        PrismObject<F> focus = requireNonNull(focusContext.getObjectAny());
        Class<F> focusType = focusContext.getObjectTypeClass();
        String channel = focusContext.getLensContext().getChannel();
        OperationResult result = parentResult.createSubresult(opName);
        try {
            boolean real = task.isExecutionFullyPersistent();
            if (real) {
                repositoryService.modifyObject(focusType, focus.getOid(), delta.getModifications(), result);
            }
            task.recordObjectActionExecuted(focus, focusType, focus.getOid(), ChangeType.MODIFY, channel, null);
        } catch (ObjectAlreadyExistsException ex) {
            task.recordObjectActionExecuted(focus, focusType, focus.getOid(), ChangeType.MODIFY, channel, ex);
            result.recordException(ex);
            throw new SystemException(ex);
        } catch (Throwable t) {
            task.recordObjectActionExecuted(focus, focusType, focus.getOid(), ChangeType.MODIFY, channel, t);
            result.recordException(t);
            throw t;
        } finally {
            result.close();
            if (delta != null) {
                focusContext.addToExecutedDeltas(
                        LensUtil.createObjectDeltaOperation(delta, result, focusContext, projCtx.getResource()));
            }
        }
    }

    private void setLinkedFromLivenessState(OperationResult result) throws SchemaException, ObjectNotFoundException {
        LOGGER.trace("Setting link according to the liveness state: {}", shadowLivenessState);
        if (shadowLivenessState == null) {
            // Temporary workaround for MID-8653; TODO why we get here during simulation?
            LOGGER.debug("Null shadow liveness state in {}, using legacy criteria", projCtx.toHumanReadableString());
            setLinkedFromLegacyCriteria(result);
            return;
        }
        switch (shadowLivenessState) {
            case DELETED:
                LOGGER.trace("Shadow is not in repository, deleting linkRef");
                deleteLinkRefFromFocus(result);
                break;
            case DEAD:
                LOGGER.trace("Shadow is dead, linking as dead");
                setLinkedAsRelated(result);
                break;
            case LIVE:
                LOGGER.trace("Shadow is live, linking as live");
                setLinkedNormally(result);
                break;
            default:
                throw new AssertionError(shadowLivenessState);
        }
    }

    /**
     * TODO remove this code eventually
     */
    private void setLinkedFromLegacyCriteria(OperationResult result) throws SchemaException, ObjectNotFoundException {

        SynchronizationPolicyDecision decision = projCtx.getSynchronizationPolicyDecision();
        SynchronizationIntent intent = projCtx.getSynchronizationIntent();

        if (decision == SynchronizationPolicyDecision.DELETE) {

            // 1. Shadow does exist in repo. So, by definition, we want to keep the link.
            // 2. But the link should be invisible, so org:related should be used.
            LOGGER.trace("Shadow is present but the decision is {}. Link should be 'related'.", decision);
            setLinkedAsRelated(result);

        } else if (decision == SynchronizationPolicyDecision.BROKEN) {

            // 1. Broken accounts can be either 'being deleted' or not.
            // 2. We know that the shadow exists, so the link should be present.
            // 3. Let us try to base our decision on synchronization intent.
            if (intent == SynchronizationIntent.UNLINK || intent == SynchronizationIntent.DELETE) {
                LOGGER.trace("Shadow is present, projection is broken, and intent was {}. Link should be 'related'.", intent);
                setLinkedAsRelated(result);
            } else {
                LOGGER.trace("Shadow is present, projection is broken, and intent was {}. Link should be 'default'.", intent);
                setLinkedNormally(result);
            }

        } else {

            LOGGER.trace("Projection seems to be alive (decision = {}). Link should be 'default'.", decision);
            setLinkedNormally(result);
        }
    }

    private void deleteLinkCompletely(OperationResult result) throws ObjectNotFoundException, SchemaException {
        deleteLinkRefFromFocus(result);
        updateSituationInShadow(null, false, result);
    }

    private void setLinkedAsRelated(OperationResult result) throws SchemaException, ObjectNotFoundException {
        setLinkRefInFocus(SchemaConstants.ORG_RELATED, result);
        updateSituationInShadow(SynchronizationSituationType.LINKED, false, result);
    }

    private void setLinkedNormally(OperationResult result) throws SchemaException, ObjectNotFoundException {
        setLinkRefInFocus(SchemaConstants.ORG_DEFAULT, result);
        // This is the most frequent case. Most probably, the shadow is correctly loaded in the projection context.
        // Hence, we can save one repository access by not loading it.
        updateSituationInShadow(SynchronizationSituationType.LINKED, true, result);
    }

    private void setLinkRefInFocus(QName relation, OperationResult result)
            throws SchemaException, ObjectNotFoundException {
        LOGGER.trace("Setting linkRef in focus if needed (relation = {})", relation);
        List<ObjectReferenceType> matchingLinks = getMatchingLinks();
        List<ObjectReferenceType> linksWithWrongRelation = matchingLinks.stream()
                .filter(ref -> !QNameUtil.match(ref.getRelation(), relation))
                .collect(Collectors.toList());
        List<ObjectReferenceType> linksWithGoodRelation = matchingLinks.stream()
                .filter(ref -> QNameUtil.match(ref.getRelation(), relation))
                .collect(Collectors.toList());
        if (!linksWithGoodRelation.isEmpty() && linksWithWrongRelation.isEmpty()) {
            LOGGER.trace("Skipping. Appropriate linkRef already exists and nothing extra: {}", linksWithGoodRelation);
            return;
        }

        List<ObjectReferenceType> linksToAdd;
        if (linksWithGoodRelation.isEmpty()) {
            linksToAdd = singletonList(
                    new ObjectReferenceType()
                            .oid(projectionOid)
                            .type(ShadowType.COMPLEX_TYPE)
                            .relation(relation));
        } else {
            linksToAdd = emptyList();
        }

        LOGGER.debug("Linking shadow {} to focus {}", projectionOid, focusContext.getObjectCurrent());
        ObjectDelta<F> delta = prismContext.deltaFor(focusContext.getObjectTypeClass())
                .item(FocusType.F_LINK_REF)
                .deleteRealValues(CloneUtil.cloneCollectionMembers(linksWithWrongRelation))
                .addRealValues(linksToAdd)
                .asObjectDelta(focusContext.getOid());
        executeFocusDelta(delta, OP_LINK_ACCOUNT, result);
    }

    private boolean checkOidPresent() throws SchemaException, ConfigurationException {
        if (projCtx.getOid() != null) {
            return false;
        }
        if (projCtx.isBroken()) {
            // This seems to be OK. In quite a strange way, but still OK.
            LOGGER.trace("Shadow OID not present in broken context, not updating links. Context: {}",
                    projCtx.toHumanReadableString());
            return true;
        }
        if (!projCtx.isVisible()) {
            LOGGER.trace("Shadow OID not present in invisible account -> not updating links. In: {}",
                    projCtx.toHumanReadableString());
            return true;
        }
        LOGGER.error("Projection {} has null OID, this should not happen, context:\n{}", projCtx.toHumanReadableString(),
                projCtx.debugDump());
        throw new IllegalStateException("Projection " + projCtx.toHumanReadableString() + " has null OID, this should not happen");
    }

    private void updateSituationInShadow(
            SynchronizationSituationType newSituation, boolean skipLoadingShadow, OperationResult parentResult) {
        OperationResult result = parentResult.subresult(OP_UPDATE_SITUATION_IN_SHADOW)
                .setMinor()
                .addArbitraryObjectAsParam("situation", newSituation)
                .addParam("accountRef", projectionOid)
                .build();
        try {
            LOGGER.trace("updateSituationInShadow: called with newSituation={}", newSituation);
            PrismObject<ShadowType> currentShadow = getCurrentShadow(skipLoadingShadow, result);
            if (currentShadow == null) {
                return;
            }

            SynchronizationSituationType currentSynchronizationSituation = currentShadow.asObjectable().getSynchronizationSituation();
            if (currentSynchronizationSituation == SynchronizationSituationType.DELETED && ShadowUtil.isDead(currentShadow.asObjectable())) {
                LOGGER.trace("Skipping update of synchronization situation for deleted dead shadow");
                result.recordSuccess();
                return;
            }

            // Note there can be some discrepancies between computed situation and the one that will be really present
            // in the repository after the task finishes. It can occur if the modify operation does not succeed.
            task.onSynchronizationSituationChange(context.getItemProcessingIdentifier(), projectionOid, newSituation);
            projCtx.setSynchronizationSituationResolved(newSituation);

            if (isSkipWhenNoChange()) {
                if (newSituation == currentSynchronizationSituation) {
                    LOGGER.trace("Skipping update of synchronization situation because there is no change ({})",
                            currentSynchronizationSituation);
                    result.recordSuccess();
                    return;
                } else {
                    LOGGER.trace("Updating synchronization situation {} -> {}", currentSynchronizationSituation, newSituation);
                }
            }

            XMLGregorianCalendar now = clock.currentTimeXMLGregorianCalendar();
            // We consider the shadow to be fully synchronized, as we are processing its owner in the clockwork now.
            boolean fullSynchronization = projCtx.hasFullShadow() && task.isExecutionFullyPersistent();
            List<ItemDelta<?, ?>> syncSituationDeltas =
                    SynchronizationUtils.createSynchronizationSituationAndDescriptionDelta(
                            currentShadow.asObjectable(), newSituation, task.getChannel(), fullSynchronization, now);

            boolean real = task.isExecutionFullyPersistent();
            if (real) {
                executeShadowDelta(syncSituationDeltas, result);
            }
            LOGGER.trace("Situation in projection {} was updated to {} (real={})", projCtx, newSituation, real);
        } catch (Exception ex) {
            result.recordFatalError(ex);
            throw new SystemException(ex.getMessage(), ex);
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    private @Nullable PrismObject<ShadowType> getCurrentShadow(boolean skipLoadingShadow, OperationResult result) {

        if (skipLoadingShadow && projCtx.getObjectCurrent() != null) {
            LOGGER.trace("Skipping loading shadow, using the one from the context.");
            return projCtx.getObjectCurrent();
        }

        try {
            return provisioningService.getObject(
                    ShadowType.class,
                    projectionOid,
                    schemaService.getOperationOptionsBuilder()
                            .readOnly()
                            .noFetch()
                            .allowNotFound(true)
                            .build(),
                    task, result);
        } catch (ObjectNotFoundException ex) {
            LOGGER.trace("Shadow is gone, skipping modifying situation in shadow.");
            result.muteLastSubresultError();
            result.recordSuccess();
            task.onSynchronizationSituationChange(context.getItemProcessingIdentifier(), projectionOid, null); // TODO or what?
            return null;
        } catch (Exception ex) {
            LOGGER.trace("Problem with getting shadow, skipping modifying situation in shadow.");
            result.recordPartialError(ex);
            task.onSynchronizationSituationChange(context.getItemProcessingIdentifier(), projectionOid, null); // TODO or what?
            return null;
        }
    }

    private void executeShadowDelta(List<ItemDelta<?, ?>> syncSituationDeltas, OperationResult result)
            throws SchemaException, CommunicationException, ConfigurationException, SecurityViolationException,
            PolicyViolationException, ObjectAlreadyExistsException, ExpressionEvaluationException {
        try {
            ModelImplUtils.setRequestee(task, focusContext);
            ProvisioningOperationOptions options = ProvisioningOperationOptions.createCompletePostponed(false);
            options.setDoNotDiscovery(true);
            ProvisioningOperationContext ctx = context.createProvisioningOperationContext();
            provisioningService.modifyObject(
                    ShadowType.class, projectionOid, syncSituationDeltas, null, options, ctx, task, result);
        } catch (ObjectNotFoundException ex) {
            // if the object not found exception is thrown, it's ok..probably
            // the account was deleted by previous execution of changes..just
            // log in the trace the message for the user..
            LOGGER.debug("Situation in account could not be updated. Account not found on the resource.");
        } finally {
            ModelImplUtils.clearRequestee(task);
        }
    }

    private boolean isSkipWhenNoChange() {
        InternalsConfigurationType internalsConfig = context.getInternalsConfiguration();
        return internalsConfig != null && internalsConfig.getSynchronizationSituationUpdating() != null &&
                Boolean.TRUE.equals(internalsConfig.getSynchronizationSituationUpdating().isSkipWhenNoChange());
    }
}
