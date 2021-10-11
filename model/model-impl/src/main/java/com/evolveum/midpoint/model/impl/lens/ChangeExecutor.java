/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens;

import static com.evolveum.midpoint.model.api.ProgressInformation.ActivityType.FOCUS_OPERATION;
import static com.evolveum.midpoint.model.api.ProgressInformation.ActivityType.RESOURCE_OBJECT_OPERATION;
import static com.evolveum.midpoint.model.api.ProgressInformation.StateType.ENTERING;
import static com.evolveum.midpoint.prism.PrismContainerValue.asContainerables;
import static com.evolveum.midpoint.schema.internals.InternalsConfig.consistencyChecks;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.xml.bind.JAXBElement;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.api.context.SynchronizationIntent;
import com.evolveum.midpoint.wf.api.WorkflowManager;

import org.apache.commons.lang.BooleanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.common.SynchronizationUtils;
import com.evolveum.midpoint.model.api.ModelAuthorizationAction;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.ProgressInformation;
import com.evolveum.midpoint.model.api.context.SynchronizationPolicyDecision;
import com.evolveum.midpoint.model.impl.ModelObjectResolver;
import com.evolveum.midpoint.model.common.expression.ExpressionEnvironment;
import com.evolveum.midpoint.model.common.expression.ModelExpressionThreadLocalHolder;
import com.evolveum.midpoint.model.impl.lens.projector.credentials.CredentialsProcessor;
import com.evolveum.midpoint.model.impl.lens.projector.focus.FocusConstraintsChecker;
import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.delta.*;
import com.evolveum.midpoint.prism.equivalence.EquivalenceStrategy;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.xnode.PrimitiveXNode;
import com.evolveum.midpoint.prism.xnode.XNodeFactory;
import com.evolveum.midpoint.provisioning.api.ProvisioningOperationOptions;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.api.*;
import com.evolveum.midpoint.repo.common.expression.*;
import com.evolveum.midpoint.repo.common.util.RepoCommonUtils;
import com.evolveum.midpoint.schema.*;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.expression.ExpressionProfile;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.ExceptionUtil;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.OwnerResolver;
import com.evolveum.midpoint.security.api.SecurityContextManager;
import com.evolveum.midpoint.security.enforcer.api.AuthorizationParameters;
import com.evolveum.midpoint.security.enforcer.api.SecurityEnforcer;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;
import com.evolveum.prism.xml.ns._public.types_3.RawType;

/**
 * @author semancik
 */
@Component
public class ChangeExecutor {

    private static final Trace LOGGER = TraceManager.getTrace(ChangeExecutor.class);

    private static final String OPERATION_EXECUTE_DELTA = ChangeExecutor.class.getName() + ".executeDelta";
    private static final String OPERATION_EXECUTE = ChangeExecutor.class.getName() + ".execute";
    private static final String OPERATION_EXECUTE_FOCUS = OPERATION_EXECUTE + ".focus";
    private static final String OPERATION_EXECUTE_PROJECTION = OPERATION_EXECUTE + ".projection";
    private static final String OPERATION_LINK_ACCOUNT = ChangeExecutor.class.getName() + ".linkShadow";
    private static final String OPERATION_UNLINK_ACCOUNT = ChangeExecutor.class.getName() + ".unlinkShadow";
    private static final String OPERATION_UPDATE_SITUATION_IN_SHADOW = ChangeExecutor.class.getName() + ".updateSituationInShadow";

    @Autowired private TaskManager taskManager;
    @Autowired(required = false) private WorkflowManager workflowManager; // not available e.g. during tests
    @Autowired @Qualifier("cacheRepositoryService") private transient RepositoryService cacheRepositoryService;
    @Autowired private ProvisioningService provisioning;
    @Autowired private PrismContext prismContext;
    @Autowired private ExpressionFactory expressionFactory;
    @Autowired private SecurityEnforcer securityEnforcer;
    @Autowired private SecurityContextManager securityContextManager;
    @Autowired private Clock clock;
    @Autowired private ModelObjectResolver objectResolver;
    @Autowired private OperationalDataManager metadataManager;
    @Autowired private CredentialsProcessor credentialsProcessor;
    @Autowired private ClockworkConflictResolver clockworkConflictResolver;

    private PrismObjectDefinition<UserType> userDefinition = null;
    private PrismObjectDefinition<ShadowType> shadowDefinition = null;

    @PostConstruct
    private void locateDefinitions() {
        userDefinition = prismContext.getSchemaRegistry()
                .findObjectDefinitionByCompileTimeClass(UserType.class);
        shadowDefinition = prismContext.getSchemaRegistry()
                .findObjectDefinitionByCompileTimeClass(ShadowType.class);
    }

    // returns true if current operation has to be restarted, see
    // ObjectAlreadyExistsException handling (TODO specify more exactly)
    public <O extends ObjectType> boolean executeChanges(LensContext<O> context, Task task,
            OperationResult parentResult) throws ObjectAlreadyExistsException, ObjectNotFoundException,
            SchemaException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException, PreconditionViolationException, PolicyViolationException {

        OperationResult result = parentResult.createSubresult(OPERATION_EXECUTE);

        try {

            // FOCUS

            context.checkAbortRequested();

            LensFocusContext<O> focusContext = context.getFocusContext();
            if (focusContext != null) {
                ObjectDelta<O> focusDelta = focusContext.getWaveExecutableDelta(context.getExecutionWave());

                focusDelta = applyPendingObjectPolicyStateModifications(focusContext, focusDelta);
                focusDelta = applyPendingAssignmentPolicyStateModifications(focusContext, focusDelta);

                if (focusDelta == null && !context.hasProjectionChange()) {
                    LOGGER.trace("Skipping focus change execute, because user delta is null");
                } else {

                    if (focusDelta == null) {
                        focusDelta = focusContext.getObjectAny().createModifyDelta();
                    }

                    ArchetypePolicyType archetypePolicy = focusContext.getArchetypePolicyType();
                    applyObjectPolicy(focusContext, focusDelta, archetypePolicy);

                    OperationResult subResult = result.createSubresult(
                            OPERATION_EXECUTE_FOCUS + "." + focusContext.getObjectTypeClass().getSimpleName());

                    try {
                        // Will remove credential deltas or hash them
                        focusDelta = credentialsProcessor.transformFocusExecutionDelta(context, focusDelta);
                    } catch (EncryptionException e) {
                        recordFatalError(subResult, result, null, e);
                        result.computeStatus();
                        throw new SystemException(e.getMessage(), e);
                    }

                    applyLastProvisioningTimestamp(context, focusDelta);

                    try {

                        context.reportProgress(new ProgressInformation(FOCUS_OPERATION, ENTERING));

                        ConflictResolutionType conflictResolution = ModelExecuteOptions
                                .getFocusConflictResolution(context.getOptions());

                        executeDelta(focusDelta, focusContext, context, null, conflictResolution, null, task, subResult);

                        if (focusDelta.isAdd() && focusDelta.getOid() != null) {
                            clockworkConflictResolver.createConflictWatcherAfterFocusAddition(context, focusDelta.getOid(),
                                    focusDelta.getObjectToAdd().getVersion());
                        }
                        subResult.computeStatus();

                    } catch (SchemaException | ObjectNotFoundException | CommunicationException | ConfigurationException | SecurityViolationException | ExpressionEvaluationException | RuntimeException e) {
                        recordFatalError(subResult, result, null, e);
                        throw e;

                    } catch (PreconditionViolationException e) {

                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("Modification precondition failed for {}: {}", focusContext.getHumanReadableName(),
                                    e.getMessage());
                        }
                        //                    TODO: fatal error if the conflict resolution is "error" (later)
                        result.recordHandledError(e);
                        throw e;

                    } catch (ObjectAlreadyExistsException e) {
                        subResult.computeStatus();
                        if (!subResult.isSuccess() && !subResult.isHandledError()) {
                            subResult.recordFatalError(e);
                        }
                        result.computeStatusComposite();
                        throw e;
                    } finally {
                        context.reportProgress(new ProgressInformation(FOCUS_OPERATION, subResult));
                    }
                }
            }

            // PROJECTIONS

            context.checkAbortRequested();

            boolean restartRequested = false;

            for (LensProjectionContext projCtx : context.getProjectionContexts()) {
                if (projCtx.getWave() != context.getExecutionWave()) {
                    LOGGER.trace("Skipping projection context {} because its wave ({}) is different from execution wave ({})",
                            projCtx.toHumanReadableString(), projCtx.getWave(), context.getExecutionWave());
                    continue;
                }

                if (!projCtx.isCanProject()) {
                    LOGGER.trace("Skipping projection context {} because canProject is false", projCtx.toHumanReadableString());
                    continue;
                }

                // we should not get here, but just to be sure
                if (projCtx.getSynchronizationPolicyDecision() == SynchronizationPolicyDecision.IGNORE) {
                    LOGGER.trace("Skipping ignored projection context {}", projCtx.toHumanReadableString());
                    continue;
                }

                OperationResult subResult = result.subresult(OPERATION_EXECUTE_PROJECTION + "." + projCtx.getObjectTypeClass().getSimpleName())
                        .addParam("resource", projCtx.getResource())
                        .addArbitraryObjectAsContext("discriminator", projCtx.getResourceShadowDiscriminator())
                        .build();

                PrismObject<ShadowType> shadowAfterModification = null;
                try {
                    LOGGER.trace("Executing projection context {}", projCtx.toHumanReadableString());

                    context.checkAbortRequested();

                    context.reportProgress(new ProgressInformation(RESOURCE_OBJECT_OPERATION,
                            projCtx.getResourceShadowDiscriminator(), ENTERING));

                    executeReconciliationScript(projCtx, context, BeforeAfterType.BEFORE, task, subResult);

                    ObjectDelta<ShadowType> projDelta = projCtx.getExecutableDelta();

                    if (shouldBeDeleted(projDelta, projCtx)) {
                        projDelta = prismContext.deltaFactory().object()
                                .createDeleteDelta(projCtx.getObjectTypeClass(), projCtx.getOid());
                    }

                    if (projCtx.getSynchronizationPolicyDecision() == SynchronizationPolicyDecision.BROKEN) {
                        if (context.getFocusContext() != null
                                && context.getFocusContext().getDelta() != null
                                && context.getFocusContext().getDelta().isDelete()
                                && context.getOptions() != null
                                && ModelExecuteOptions.isForce(context.getOptions())) {
                            if (projDelta == null) {
                                projDelta = prismContext.deltaFactory().object()
                                        .createDeleteDelta(projCtx.getObjectTypeClass(), projCtx.getOid());
                            }
                        }
                        if (projDelta != null && projDelta.isDelete()) {

                            shadowAfterModification = executeDelta(projDelta, projCtx, context, null, null, projCtx.getResource(), task,
                                    subResult);

                        }
                    } else {

                        if (projDelta == null || projDelta.isEmpty()) {
                            LOGGER.trace("No change for {}", projCtx.getResourceShadowDiscriminator());
                            shadowAfterModification = projCtx.getObjectCurrent();
                            if (focusContext != null) {
                                updateLinks(context, focusContext, projCtx, shadowAfterModification, task, subResult);
                            }

                            // Make sure post-reconcile delta is always executed,
                            // even if there is no change
                            executeReconciliationScript(projCtx, context, BeforeAfterType.AFTER, task,
                                    subResult);

                            subResult.computeStatus();
                            subResult.recordNotApplicableIfUnknown();
                            continue;

                        } else if (projDelta.isDelete() && projCtx.getResourceShadowDiscriminator() != null
                                && projCtx.getResourceShadowDiscriminator().getOrder() > 0) {
                            // HACK ... for higher-order context check if this was
                            // already deleted
                            LensProjectionContext lowerOrderContext = LensUtil.findLowerOrderContext(context,
                                    projCtx);
                            if (lowerOrderContext != null && lowerOrderContext.isDelete()) {
                                // We assume that this was already executed
                                subResult.setStatus(OperationResultStatus.NOT_APPLICABLE);
                                continue;
                            }
                        }

                        shadowAfterModification = executeDelta(projDelta, projCtx, context, null, null, projCtx.getResource(), task, subResult);

                        if (projCtx.isAdd() && shadowAfterModification != null) {
                            projCtx.setExists(true);
                        }

                    }

                    subResult.computeStatus();
                    if (focusContext != null) {
                        updateLinks(context, focusContext, projCtx, shadowAfterModification, task, subResult);
                    }

                    executeReconciliationScript(projCtx, context, BeforeAfterType.AFTER, task, subResult);

                    subResult.computeStatus();
                    subResult.recordNotApplicableIfUnknown();

                } catch (SchemaException | ObjectNotFoundException | PreconditionViolationException | CommunicationException |
                        ConfigurationException | SecurityViolationException | PolicyViolationException | ExpressionEvaluationException | RuntimeException | Error e) {
                    recordProjectionExecutionException(e, projCtx, subResult, SynchronizationPolicyDecision.BROKEN);

                    // We still want to update the links here. E.g. this may be live sync case where we discovered new account
                    // try to reconcile, but the reconciliation fails. We still want this shadow linked to user.
                    if (focusContext != null) {
                        updateLinks(context, focusContext, projCtx, shadowAfterModification, task, subResult);
                    }

                    ModelImplUtils.handleConnectorErrorCriticality(projCtx.getResource(), e, subResult);

                } catch (ObjectAlreadyExistsException e) {

                    // This exception is quite special. We have to decide how bad this really is.
                    // This may be rename conflict. Which would be bad.
                    // Or this may be attempt to create account that already exists and just needs
                    // to be linked. Which is no big deal and consistency mechanism (discovery) will
                    // easily handle that. In that case it is done in "another task" which is
                    // quasi-asynchornously executed from provisioning by calling notifyChange.
                    // Once that is done then the account is already linked. And all we need to do
                    // is to restart this whole operation.

                    // check if this is a repeated attempt - OAEE was not handled
                    // correctly, e.g. if creating "Users" user in AD, whereas
                    // "Users" is SAM Account Name which is used by a built-in group
                    // - in such case, mark the context as broken
                    if (isRepeatedAlreadyExistsException(projCtx)) {
                        // This is the bad case. Currently we do not do anything more intelligent than to look for
                        // repeated error. If we get OAEE twice then this is bad and we thow up.
                        // TODO: do something smarter here
                        LOGGER.debug("Repeated ObjectAlreadyExistsException detected, marking projection {} as broken", projCtx.toHumanReadableString());
                        recordProjectionExecutionException(e, projCtx, subResult,
                                SynchronizationPolicyDecision.BROKEN);
                        continue;
                    }

                    // in his case we do not need to set account context as
                    // broken, instead we need to restart projector for this
                    // context to recompute new account or find out if the
                    // account was already linked..
                    // and also do not set fatal error to the operation result, this
                    // is a special case
                    // if it is fatal, it will be set later
                    // but we need to set some result
                    subResult.recordSuccess();
                    restartRequested = true;
                    LOGGER.debug("ObjectAlreadyExistsException for projection {}, requesting projector restart", projCtx.toHumanReadableString());
                    // we will process remaining projections when retrying the wave
                    break;

                } finally {
                    context.reportProgress(new ProgressInformation(RESOURCE_OBJECT_OPERATION,
                            projCtx.getResourceShadowDiscriminator(), subResult));
                }
            }

            // Result computation here needs to be slightly different
            result.computeStatusComposite();
            return restartRequested;

        } catch (Throwable t) {
            result.recordThrowableIfNeeded(t);      // last resort: to avoid UNKNOWN subresults
            throw t;
        }
    }

    private <O extends ObjectType> ObjectDelta<O> applyPendingObjectPolicyStateModifications(LensFocusContext<O> focusContext,
            ObjectDelta<O> focusDelta) throws SchemaException {
        for (ItemDelta<?, ?> itemDelta : focusContext.getPendingObjectPolicyStateModifications()) {
            focusDelta = focusContext.swallowToDelta(focusDelta, itemDelta);
        }
        focusContext.clearPendingObjectPolicyStateModifications();
        return focusDelta;
    }

    private <O extends ObjectType> ObjectDelta<O> applyPendingAssignmentPolicyStateModifications(LensFocusContext<O> focusContext, ObjectDelta<O> focusDelta)
            throws SchemaException {
        for (Map.Entry<AssignmentSpec, List<ItemDelta<?, ?>>> entry : focusContext
                .getPendingAssignmentPolicyStateModifications().entrySet()) {
            PlusMinusZero mode = entry.getKey().mode;
            if (mode == PlusMinusZero.MINUS) {
                continue;       // this assignment is being thrown out anyway, so let's ignore it (at least for now)
            }
            AssignmentType assignmentToFind = entry.getKey().assignment;
            List<ItemDelta<?, ?>> modifications = entry.getValue();
            if (modifications.isEmpty()) {
                continue;
            }
            LOGGER.trace("Applying policy state modifications for {} ({}):\n{}", assignmentToFind, mode,
                    DebugUtil.debugDumpLazily(modifications));
            if (mode == PlusMinusZero.ZERO) {
                if (assignmentToFind.getId() == null) {
                    throw new IllegalStateException("Existing assignment with null id: " + assignmentToFind);
                }
                for (ItemDelta<?, ?> modification : modifications) {
                    focusDelta = focusContext.swallowToDelta(focusDelta, modification);
                }
            } else {
                assert mode == PlusMinusZero.PLUS;
                if (focusDelta != null && focusDelta.isAdd()) {
                    swallowIntoValues(((FocusType) focusDelta.getObjectToAdd().asObjectable()).getAssignment(),
                            assignmentToFind, modifications);
                } else {
                    ContainerDelta<AssignmentType> assignmentDelta = focusDelta != null ?
                            focusDelta.findContainerDelta(FocusType.F_ASSIGNMENT) : null;
                    if (assignmentDelta == null) {
                        throw new IllegalStateException(
                                "We have 'plus' assignment to modify but there's no assignment delta. Assignment="
                                        + assignmentToFind + ", objectDelta=" + focusDelta);
                    }
                    if (assignmentDelta.isReplace()) {
                        swallowIntoValues(asContainerables(assignmentDelta.getValuesToReplace()), assignmentToFind,
                                modifications);
                    } else if (assignmentDelta.isAdd()) {
                        swallowIntoValues(asContainerables(assignmentDelta.getValuesToAdd()), assignmentToFind,
                                modifications);
                    } else {
                        throw new IllegalStateException(
                                "We have 'plus' assignment to modify but there're no values to add or replace in assignment delta. Assignment="
                                        + assignmentToFind + ", objectDelta=" + focusDelta);
                    }
                }
            }
        }
        focusContext.clearPendingAssignmentPolicyStateModifications();
        return focusDelta;
    }

    private void swallowIntoValues(Collection<AssignmentType> assignments, AssignmentType assignmentToFind, List<ItemDelta<?, ?>> modifications)
            throws SchemaException {
        for (AssignmentType assignment : assignments) {
            PrismContainerValue<?> pcv = assignment.asPrismContainerValue();
            PrismContainerValue<?> pcvToFind = assignmentToFind.asPrismContainerValue();
            if (pcv.representsSameValue(pcvToFind, false) || pcv.equals(pcvToFind, EquivalenceStrategy.REAL_VALUE_CONSIDER_DIFFERENT_IDS)) {
                // TODO what if ID of the assignment being added is changed in repo? Hopefully it will be not.
                for (ItemDelta<?, ?> modification : modifications) {
                    ItemPath newParentPath = modification.getParentPath().rest(2);        // killing assignment + ID
                    ItemDelta<?, ?> pathRelativeModification = modification.cloneWithChangedParentPath(newParentPath);
                    pathRelativeModification.applyTo(pcv);
                }
                return;
            }
        }
        // TODO change to warning
        throw new IllegalStateException("We have 'plus' assignment to modify but it couldn't be found in assignment delta. Assignment=" + assignmentToFind + ", new assignments=" + assignments);
    }

    private <O extends ObjectType> void applyLastProvisioningTimestamp(LensContext<O> context, ObjectDelta<O> focusDelta) throws SchemaException {
        if (!context.hasProjectionChange()) {
            return;
        }
        if (focusDelta.isAdd()) {

            PrismObject<O> objectToAdd = focusDelta.getObjectToAdd();
            PrismContainer<MetadataType> metadataContainer = objectToAdd.findOrCreateContainer(ObjectType.F_METADATA);
            metadataContainer.getRealValue().setLastProvisioningTimestamp(clock.currentTimeXMLGregorianCalendar());

        } else if (focusDelta.isModify()) {

            PropertyDelta<XMLGregorianCalendar> provTimestampDelta = prismContext.deltaFactory().property().createModificationReplaceProperty(
                    ItemPath.create(ObjectType.F_METADATA, MetadataType.F_LAST_PROVISIONING_TIMESTAMP),
                    context.getFocusContext().getObjectDefinition(),
                    clock.currentTimeXMLGregorianCalendar());
            focusDelta.addModification(provTimestampDelta);

        }
    }

    private boolean shouldBeDeleted(ObjectDelta<ShadowType> accDelta, LensProjectionContext accCtx) {
        return (accDelta == null || accDelta.isEmpty())
                && (accCtx.getSynchronizationPolicyDecision() == SynchronizationPolicyDecision.DELETE
                || accCtx.getSynchronizationIntent() == SynchronizationIntent.DELETE);
    }

    private <O extends ObjectType> boolean isRepeatedAlreadyExistsException(
            LensProjectionContext projContext) {
        int deltas = projContext.getExecutedDeltas().size();
        LOGGER.trace("isRepeatedAlreadyExistsException starting; number of executed deltas = {}", deltas);
        if (deltas < 2) {
            return false;
        }
        LensObjectDeltaOperation<ShadowType> lastDeltaOp = projContext.getExecutedDeltas().get(deltas - 1);
        LensObjectDeltaOperation<ShadowType> previousDeltaOp = projContext.getExecutedDeltas()
                .get(deltas - 2);
        // TODO check also previous execution result to see if it's
        // AlreadyExistException?
        ObjectDelta<ShadowType> lastDelta = lastDeltaOp.getObjectDelta();
        ObjectDelta<ShadowType> previousDelta = previousDeltaOp.getObjectDelta();
        boolean rv;
        if (lastDelta.isAdd() && previousDelta.isAdd()) {
            rv = isEquivalentAddDelta(lastDelta.getObjectToAdd(), previousDelta.getObjectToAdd());
        } else if (lastDelta.isModify() && previousDelta.isModify()) {
            rv = isEquivalentModifyDelta(lastDelta.getModifications(), previousDelta.getModifications());
        } else {
            rv = false;
        }
        LOGGER.trace(
                "isRepeatedAlreadyExistsException returning {}; based of comparison of previousDelta:\n{}\nwith lastDelta:\n{}",
                rv, previousDelta, lastDelta);
        return rv;
    }

    private boolean isEquivalentModifyDelta(Collection<? extends ItemDelta<?, ?>> modifications1,
            Collection<? extends ItemDelta<?, ?>> modifications2) {
        Collection<? extends ItemDelta<?, ?>> attrDeltas1 = ItemDeltaCollectionsUtil
                .findItemDeltasSubPath(modifications1, ShadowType.F_ATTRIBUTES);
        Collection<? extends ItemDelta<?, ?>> attrDeltas2 = ItemDeltaCollectionsUtil
                .findItemDeltasSubPath(modifications2, ShadowType.F_ATTRIBUTES);
        //noinspection unchecked,RedundantCast
        return MiscUtil.unorderedCollectionEquals((Collection) attrDeltas1, (Collection) attrDeltas2);
    }

    private boolean isEquivalentAddDelta(PrismObject<ShadowType> object1, PrismObject<ShadowType> object2) {
        PrismContainer attributes1 = object1.findContainer(ShadowType.F_ATTRIBUTES);
        PrismContainer attributes2 = object2.findContainer(ShadowType.F_ATTRIBUTES);
        if (attributes1 == null || attributes2 == null || attributes1.size() != 1
                || attributes2.size() != 1) { // suspicious cases
            return false;
        }
        return attributes1.getValue().equivalent(attributes2.getValue());
    }

    private <O extends ObjectType> void applyObjectPolicy(LensFocusContext<O> focusContext,
            ObjectDelta<O> focusDelta, ArchetypePolicyType archetypePolicy) {
        if (archetypePolicy == null) {
            return;
        }
        PrismObject<O> objectNew = focusContext.getObjectNew();
        if (focusDelta.isAdd() && objectNew.getOid() == null) {

            for (ItemConstraintType itemConstraintType : archetypePolicy.getItemConstraint()) {
                processItemConstraint(focusContext, objectNew, itemConstraintType);
            }
            // Deprecated
            for (ItemConstraintType itemConstraintType : archetypePolicy.getPropertyConstraint()) {
                processItemConstraint(focusContext, objectNew, itemConstraintType);
            }

        }
    }

    private <O extends ObjectType> void processItemConstraint(LensFocusContext<O> focusContext, PrismObject<O> objectNew, ItemConstraintType itemConstraintType) {
        if (BooleanUtils.isTrue(itemConstraintType.isOidBound())) {
            ItemPath itemPath = itemConstraintType.getPath().getItemPath();
            PrismProperty<Object> prop = objectNew.findProperty(itemPath);
            String stringValue = prop.getRealValue().toString();
            focusContext.setOid(stringValue);
        }
    }

    private <P extends ObjectType> void recordProjectionExecutionException(Throwable e,
            LensProjectionContext accCtx, OperationResult subResult, SynchronizationPolicyDecision decision) {
        subResult.recordFatalError(e);
        LOGGER.error("Error executing changes for {}: {}", accCtx.toHumanReadableString(), e.getMessage(), e);
        if (decision != null) {
            accCtx.setSynchronizationPolicyDecision(decision);
        }
    }

    private void recordFatalError(OperationResult subResult, OperationResult result, String message,
            Throwable e) {
        if (message == null) {
            message = e.getMessage();
        }
        subResult.recordFatalError(e);
        if (result != null) {
            result.computeStatusComposite();
        }
    }

    /**
     * Make sure that the account is linked (or unlinked) as needed.
     */
    private <O extends ObjectType, F extends FocusType> void updateLinks(LensContext<?> context,
            LensFocusContext<O> focusObjectContext, LensProjectionContext projCtx,
            PrismObject<ShadowType> shadowAfterModification,
            Task task, OperationResult result) throws ObjectNotFoundException, SchemaException {
        if (focusObjectContext == null) {
            return;
        }
        Class<O> objectTypeClass = focusObjectContext.getObjectTypeClass();
        if (!FocusType.class.isAssignableFrom(objectTypeClass)) {
            return;
        }
        //noinspection unchecked
        LensFocusContext<F> focusContext = (LensFocusContext<F>) focusObjectContext;

        if (projCtx.getResourceShadowDiscriminator() != null
                && projCtx.getResourceShadowDiscriminator().getOrder() > 0) {
            // Don't mess with links for higher-order contexts. The link should
            // be dealt with
            // during processing of zero-order context.
            return;
        }

        String projOid = projCtx.getOid();
        if (projOid == null) {
            if (projCtx.getSynchronizationPolicyDecision() == SynchronizationPolicyDecision.BROKEN) {
                // This seems to be OK. In quite a strange way, but still OK.
                return;
            }
            LOGGER.error("Projection {} has null OID, this should not happen, context:\n{}", projCtx.toHumanReadableString(), projCtx.debugDump());
            throw new IllegalStateException("Projection " + projCtx.toHumanReadableString() + " has null OID, this should not happen");
        }

        if (linkShouldExist(focusContext, projCtx, shadowAfterModification, result)) {
            // Link should exist
            PrismObject<F> objectCurrent = focusContext.getObjectCurrent();
            if (objectCurrent != null) {
                for (ObjectReferenceType linkRef : objectCurrent.asObjectable().getLinkRef()) {
                    if (projOid.equals(linkRef.getOid())) {
                        // Already linked, nothing to do, only be sure, the situation is set with the good value
                        LOGGER.trace("Updating situation in already linked shadow.");
                        updateSituationInShadow(task, SynchronizationSituationType.LINKED, context, focusObjectContext, projCtx, result);
                        return;
                    }
                }
            }
            // Not linked, need to link
            linkShadow(focusContext.getOid(), projOid, focusObjectContext, projCtx, task, result);
            // be sure, that the situation is set correctly
            LOGGER.trace("Updating situation after shadow was linked.");
            updateSituationInShadow(task, SynchronizationSituationType.LINKED, context, focusObjectContext, projCtx, result);
        } else {
            // Link should NOT exist
            if (!focusContext.isDelete()) {
                PrismObject<F> objectCurrent = focusContext.getObjectCurrent();
                // it is possible that objectCurrent is null (and objectNew is
                // non-null), in case of User ADD operation (MID-2176)
                if (objectCurrent != null) {
                    PrismReference linkRef = objectCurrent.findReference(FocusType.F_LINK_REF);
                    if (linkRef != null) {
                        for (PrismReferenceValue linkRefVal : linkRef.getValues()) {
                            if (linkRefVal.getOid().equals(projOid)) {
                                // Linked, need to unlink
                                unlinkShadow(focusContext.getOid(), linkRefVal, focusObjectContext, projCtx, task, result);
                            }
                        }
                    }
                }
            }

            // This should NOT be UNLINKED. We just do not know the situation here. Reflect that in the shadow.
            LOGGER.trace("Resource object {} unlinked from the user, updating also situation in shadow.", projOid);
            updateSituationInShadow(task, null, context, focusObjectContext, projCtx, result);
            // Not linked, that's OK
        }
    }

    private <F extends FocusType> boolean linkShouldExist(LensFocusContext<F> focusContext, LensProjectionContext projCtx, PrismObject<ShadowType> shadowAfterModification, OperationResult result) {
        if (focusContext.isDelete()) {
            // if we delete focus, link doesn't exist anymore, but be sure, that the situation is updated in shadow
            return false;
        }
        if (!projCtx.isShadowExistsInRepo()) {
            // Nothing to link to
            return false;
        }
        if (projCtx.getSynchronizationPolicyDecision() == SynchronizationPolicyDecision.UNLINK) {
            return false;
        }
        if (isEmptyThombstone(projCtx)) {
            return false;
        }
        if (projCtx.getSynchronizationPolicyDecision() == SynchronizationPolicyDecision.DELETE
                || projCtx.isDelete()) {
            return shadowAfterModification != null;
        }
        if (projCtx.hasPendingOperations()) {
            return true;
        }
        return true;
    }

    /**
     * Return true if this projection is just a linkRef that points to no
     * shadow.
     */
    private boolean isEmptyThombstone(LensProjectionContext projCtx) {
        return projCtx.getResourceShadowDiscriminator() != null
                && projCtx.getResourceShadowDiscriminator().isTombstone()
                && projCtx.getObjectCurrent() == null;
    }

    private <F extends ObjectType> void linkShadow(String userOid, String shadowOid,
            LensElementContext<F> focusContext, LensProjectionContext projCtx, Task task,
            OperationResult parentResult) throws ObjectNotFoundException, SchemaException {

        Class<F> typeClass = focusContext.getObjectTypeClass();
        if (!FocusType.class.isAssignableFrom(typeClass)) {
            return;
        }

        String channel = focusContext.getLensContext().getChannel();

        LOGGER.debug("Linking shadow " + shadowOid + " to focus " + userOid);
        OperationResult result = parentResult.createSubresult(OPERATION_LINK_ACCOUNT);
        PrismReferenceValue linkRef = prismContext.itemFactory().createReferenceValue();
        linkRef.setOid(shadowOid);
        linkRef.setTargetType(ShadowType.COMPLEX_TYPE);
        Collection<? extends ItemDelta> linkRefDeltas = prismContext.deltaFactory().reference()
                .createModificationAddCollection(FocusType.F_LINK_REF, getUserDefinition(), linkRef);

        try {
            cacheRepositoryService.modifyObject(typeClass, userOid, linkRefDeltas, result);
            task.recordObjectActionExecuted(focusContext.getObjectAny(), typeClass, userOid,
                    ChangeType.MODIFY, channel, null);
        } catch (ObjectAlreadyExistsException ex) {
            task.recordObjectActionExecuted(focusContext.getObjectAny(), typeClass, userOid,
                    ChangeType.MODIFY, channel, ex);
            throw new SystemException(ex);
        } catch (Throwable t) {
            task.recordObjectActionExecuted(focusContext.getObjectAny(), typeClass, userOid,
                    ChangeType.MODIFY, channel, t);
            throw t;
        } finally {
            result.computeStatus();
            ObjectDelta<F> userDelta = prismContext.deltaFactory().object().createModifyDelta(userOid, linkRefDeltas, typeClass
            );
            LensObjectDeltaOperation<F> userDeltaOp = LensUtil.createObjectDeltaOperation(userDelta, result,
                    focusContext, projCtx);
            focusContext.addToExecutedDeltas(userDeltaOp);
        }

    }

    private PrismObjectDefinition<UserType> getUserDefinition() {
        return userDefinition;
    }

    private <F extends ObjectType> void unlinkShadow(String focusOid, PrismReferenceValue accountRef,
            LensElementContext<F> focusContext, LensProjectionContext projCtx, Task task,
            OperationResult parentResult) throws ObjectNotFoundException, SchemaException {

        Class<F> typeClass = focusContext.getObjectTypeClass();
        if (!FocusType.class.isAssignableFrom(typeClass)) {
            return;
        }

        String channel = focusContext.getLensContext().getChannel();

        LOGGER.debug("Unlinking shadow {} from focus {}", accountRef.getOid(), focusOid);
        OperationResult result = parentResult.createSubresult(OPERATION_UNLINK_ACCOUNT);
        Collection<? extends ItemDelta> accountRefDeltas = prismContext.deltaFactory().reference().createModificationDeleteCollection(
                FocusType.F_LINK_REF, getUserDefinition(), accountRef.clone());

        try {
            cacheRepositoryService.modifyObject(typeClass, focusOid, accountRefDeltas, result);
            task.recordObjectActionExecuted(focusContext.getObjectAny(), typeClass, focusOid,
                    ChangeType.MODIFY, channel, null);
        } catch (ObjectAlreadyExistsException ex) {
            task.recordObjectActionExecuted(focusContext.getObjectAny(), typeClass, focusOid,
                    ChangeType.MODIFY, channel, ex);
            result.recordFatalError(ex);
            throw new SystemException(ex);
        } catch (Throwable t) {
            task.recordObjectActionExecuted(focusContext.getObjectAny(), typeClass, focusOid,
                    ChangeType.MODIFY, channel, t);
            throw t;
        } finally {
            result.computeStatus();
            ObjectDelta<F> userDelta = prismContext.deltaFactory().object()
                    .createModifyDelta(focusOid, accountRefDeltas, typeClass
                    );
            LensObjectDeltaOperation<F> userDeltaOp = LensUtil.createObjectDeltaOperation(userDelta, result,
                    focusContext, projCtx);
            focusContext.addToExecutedDeltas(userDeltaOp);
        }

    }

    private <F extends ObjectType> void updateSituationInShadow(Task task, SynchronizationSituationType newSituation,
            LensContext<?> context, LensFocusContext<F> focusContext, LensProjectionContext projectionCtx,
            OperationResult parentResult) throws SchemaException {

        String projectionOid = projectionCtx.getOid();

        OperationResult result = parentResult.createMinorSubresult(OPERATION_UPDATE_SITUATION_IN_SHADOW);
        result.addArbitraryObjectAsParam("situation", newSituation);
        result.addParam("accountRef", projectionOid);

        projectionCtx.setSynchronizationSituationResolved(newSituation);

        PrismObject<ShadowType> currentShadow;
        GetOperationOptions getOptions = GetOperationOptions.createNoFetch();
        getOptions.setAllowNotFound(true);
        try {
            // TODO consider skipping this operation - at least in some cases
            currentShadow = provisioning.getObject(ShadowType.class, projectionOid,
                    SelectorOptions.createCollection(getOptions), task, result);
        } catch (ObjectNotFoundException ex) {
            LOGGER.trace("Shadow is gone, skipping modifying situation in shadow.");
            result.recordSuccess();
            return;
        } catch (Exception ex) {
            LOGGER.trace("Problem with getting shadow, skipping modifying situation in shadow.");
            result.recordPartialError(ex);
            return;
        }

        SynchronizationSituationType currentSynchronizationSituation = currentShadow.asObjectable().getSynchronizationSituation();
        if (currentSynchronizationSituation == SynchronizationSituationType.DELETED && ShadowUtil.isDead(currentShadow.asObjectable())) {
            LOGGER.trace("Skipping update of synchronization situation for deleted dead shadow");
            result.recordSuccess();
            return;
        }

        InternalsConfigurationType internalsConfig = context.getInternalsConfiguration();
        boolean cansSkip =
                internalsConfig != null && internalsConfig.getSynchronizationSituationUpdating() != null &&
                        Boolean.TRUE.equals(internalsConfig.getSynchronizationSituationUpdating().isSkipWhenNoChange());
        if (cansSkip) {
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
        List<PropertyDelta<?>> syncSituationDeltas = SynchronizationUtils
                .createSynchronizationSituationAndDescriptionDelta(currentShadow, newSituation, task.getChannel(),
                        projectionCtx.hasFullShadow(), now, prismContext);

        try {
            ModelImplUtils.setRequestee(task, focusContext);
            ProvisioningOperationOptions options = ProvisioningOperationOptions.createCompletePostponed(false);
            options.setDoNotDiscovery(true);
            provisioning.modifyObject(ShadowType.class, projectionOid, syncSituationDeltas, null, options, task, result);
            LOGGER.trace("Situation in projection {} was updated to {}.", projectionCtx, newSituation);
        } catch (ObjectNotFoundException ex) {
            // if the object not found exception is thrown, it's ok..probably
            // the account was deleted by previous execution of changes..just
            // log in the trace the message for the user..
            LOGGER.debug(
                    "Situation in account could not be updated. Account not found on the resource. Skipping modifying situation in account");
            return;
        } catch (Exception ex) {
            result.recordFatalError(ex);
            throw new SystemException(ex.getMessage(), ex);
        } finally {
            ModelImplUtils.clearRequestee(task);
        }
        // if everything is OK, add result of the situation modification to the
        // parent result
        result.recordSuccess();
    }

    /**
     * @return Returns estimate of the object after modification. Or null if the object was deleted.
     * NOTE: this is only partially implemented (only for shadow delete).
     */
    private <T extends ObjectType, F extends ObjectType> PrismObject<T> executeDelta(ObjectDelta<T> objectDelta,
            LensElementContext<T> objectContext, LensContext<F> context, ModelExecuteOptions options,
            ConflictResolutionType conflictResolution, ResourceType resource, Task task, OperationResult parentResult)
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException,
            CommunicationException, ConfigurationException, SecurityViolationException,
            PolicyViolationException, ExpressionEvaluationException, PreconditionViolationException {

        if (objectDelta == null) {
            throw new IllegalArgumentException("Null change");
        }

        if (objectDelta.getOid() == null) {
            objectDelta.setOid(objectContext.getOid());
        }

        objectDelta = computeDeltaToExecute(objectDelta, objectContext);
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("computeDeltaToExecute returned:\n{}",
                    objectDelta != null ? objectDelta.debugDump(1) : "(null)");
        }

        if (objectDelta == null || objectDelta.isEmpty()) {
            LOGGER.debug("Skipping execution of delta because it was already executed: {}", objectContext);
            return objectContext.getObjectCurrent();
        }

        if (InternalsConfig.consistencyChecks) {
            objectDelta.checkConsistence(ConsistencyCheckScope.fromBoolean(consistencyChecks));
        }

        // Other types than focus types may not be definition-complete (e.g.
        // accounts and resources are completed in provisioning)
        if (FocusType.class.isAssignableFrom(objectDelta.getObjectTypeClass())) {
            objectDelta.assertDefinitions();
        }

        LensUtil.setDeltaOldValue(objectContext, objectDelta);

        if (LOGGER.isTraceEnabled()) {
            logDeltaExecution(objectDelta, context, resource, null, task);
        }

        OperationResult result = parentResult.createSubresult(OPERATION_EXECUTE_DELTA);
        PrismObject<T> objectAfterModification = null;

        try {
            if (objectDelta.getChangeType() == ChangeType.ADD) {
                objectAfterModification = executeAddition(objectDelta, context, objectContext, options, resource, task, result);
            } else if (objectDelta.getChangeType() == ChangeType.MODIFY) {
                executeModification(objectDelta, context, objectContext, options, conflictResolution, resource, task, result);
            } else if (objectDelta.getChangeType() == ChangeType.DELETE) {
                objectAfterModification = executeDeletion(objectDelta, context, objectContext, options, resource, task, result);
            }

            // To make sure that the OID is set (e.g. after ADD operation)
            LensUtil.setContextOid(context, objectContext, objectDelta.getOid());

        } finally {

            result.computeStatus();
            if (objectContext != null) {
                if (!objectDelta.hasCompleteDefinition()) {
                    throw new SchemaException("object delta does not have complete definition");
                }
                LensObjectDeltaOperation<T> objectDeltaOp = LensUtil.createObjectDeltaOperation(
                        objectDelta.clone(), result, objectContext, null, resource);
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Recording executed delta:\n{}", objectDeltaOp.shorterDebugDump(1));
                }
                objectContext.addToExecutedDeltas(objectDeltaOp);
                if (result.isTracingNormal(ModelExecuteDeltaTraceType.class)) {
                    TraceType trace = new ModelExecuteDeltaTraceType(prismContext)
                            .delta(objectDeltaOp.clone().toLensObjectDeltaOperationType());     // todo kill operation result?
                    result.addTrace(trace);
                }
            } else {
                if (result.isTracingNormal(ModelExecuteDeltaTraceType.class)) {
                    LensObjectDeltaOperation<T> objectDeltaOp = new LensObjectDeltaOperation<>(objectDelta);    // todo
                    TraceType trace = new ModelExecuteDeltaTraceType(prismContext)
                            .delta(objectDeltaOp.toLensObjectDeltaOperationType());
                    result.addTrace(trace);
                }
            }

            if (LOGGER.isDebugEnabled()) {
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("EXECUTION result {}", result.getLastSubresult());
                } else {
                    // Execution of deltas was not logged yet
                    logDeltaExecution(objectDelta, context, resource, result.getLastSubresult(), task);
                }
            }
        }

        return objectAfterModification;
    }

    private <T extends ObjectType, F extends FocusType> void removeExecutedItemDeltas(
            ObjectDelta<T> objectDelta, LensElementContext<T> objectContext) {
        if (objectContext == null) {
            return;
        }

        if (objectDelta == null || objectDelta.isEmpty()) {
            return;
        }

        if (objectDelta.getModifications() == null || objectDelta.getModifications().isEmpty()) {
            return;
        }

        List<LensObjectDeltaOperation<T>> executedDeltas = objectContext.getExecutedDeltas();
        for (LensObjectDeltaOperation<T> executedDelta : executedDeltas) {
            ObjectDelta<T> executed = executedDelta.getObjectDelta();
            Iterator<? extends ItemDelta> objectDeltaIterator = objectDelta.getModifications().iterator();
            while (objectDeltaIterator.hasNext()) {
                ItemDelta d = objectDeltaIterator.next();
                if (executed.containsModification(d, EquivalenceStrategy.LITERAL_IGNORE_METADATA) || d.isEmpty()) {     // todo why literal?
                    objectDeltaIterator.remove();
                }
            }
        }
    }

//    // TODO beware - what if the delta was executed but not successfully?
//    private <T extends ObjectType, F extends FocusType> boolean alreadyExecuted(ObjectDelta<T> objectDelta,
//            LensElementContext<T> objectContext) {
//        if (objectContext == null) {
//            return false;
//        }
//        if (LOGGER.isTraceEnabled()) {
//            LOGGER.trace("Checking for already executed delta:\n{}\nIn deltas:\n{}", objectDelta.debugDump(),
//                    DebugUtil.debugDump(objectContext.getExecutedDeltas()));
//        }
//        return ObjectDeltaOperation.containsDelta(objectContext.getExecutedDeltas(), objectDelta);
//    }

    /**
     * Was this object already added? (temporary method, should be removed soon)
     */
    private <T extends ObjectType> boolean wasAdded(List<LensObjectDeltaOperation<T>> executedOperations,
            String oid) {
        for (LensObjectDeltaOperation operation : executedOperations) {
            if (operation.getObjectDelta().isAdd() && oid.equals(operation.getObjectDelta().getOid())
                    && !operation.getExecutionResult().isFatalError()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Computes delta to execute, given a list of already executes deltas. See
     * below.
     */
    private <T extends ObjectType> ObjectDelta<T> computeDeltaToExecute(ObjectDelta<T> objectDelta,
            LensElementContext<T> objectContext) throws SchemaException {
        if (objectContext == null) {
            return objectDelta;
        }
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Computing delta to execute from delta:\n{}\nGiven these executed deltas:\n{}",
                    objectDelta.debugDump(1), LensObjectDeltaOperation.shorterDebugDump(objectContext.getExecutedDeltas(), 1));
        }
        List<LensObjectDeltaOperation<T>> executedDeltas = objectContext.getExecutedDeltas();
        ObjectDelta<T> diffDelta = computeDiffDelta(executedDeltas, objectDelta);

        // One more check: is the computed delta idempotent related to objectCurrent?
        // Currently we deal only with focusContext because of safety; and also because this check is a reaction
        // in change to focus context secondary delta swallowing code (MID-5207).
        //
        // LookupTableType operation optimization is not available here, because it looks like that isRedundant
        // does not work reliably for key-based row deletions (MID-5276).
        if (diffDelta != null && objectContext instanceof LensFocusContext<?> &&
                !objectContext.isOfType(LookupTableType.class) &&
                diffDelta.isRedundant(objectContext.getObjectCurrent(), false)) {
            LOGGER.trace("delta is idempotent related to {}", objectContext.getObjectCurrent());
            return null;
        }
        return diffDelta;
    }

    /**
     * Compute a "difference delta" - given that executedDeltas were executed,
     * and objectDelta is about to be executed; eliminates parts that have
     * already been done. It is meant as a kind of optimization (for MODIFY
     * deltas) and error avoidance (for ADD deltas).
     * <p>
     * Explanation for ADD deltas: there are situations where an execution wave
     * is restarted - when unexpected AlreadyExistsException is reported from
     * provisioning. However, in such cases, duplicate ADD Focus deltas were
     * generated. So we (TEMPORARILY!) decided to filter them out here.
     * <p>
     * Unfortunately, this mechanism is not well-defined, and seems to work more
     * "by accident" than "by design". It should be replaced with something more
     * serious. Perhaps by re-reading current focus state when repeating a wave?
     * Actually, it is a supplement for rewriting ADD->MODIFY deltas in
     * LensElementContext.getFixedPrimaryDelta. That method converts primary
     * deltas (and as far as I know, that is the only place where this problem
     * should occur). Nevertheless, for historical and safety reasons I keep
     * also the processing in this method.
     * <p>
     * Anyway, currently it treats only three cases:
     * 1) if the objectDelta is present in the list of executed deltas
     * 2) if the objectDelta is ADD, and another ADD delta is there (then the difference is computed)
     * 3) if objectDelta is MODIFY or DELETE and previous delta was MODIFY
     */
    private <T extends ObjectType> ObjectDelta<T> computeDiffDelta(
            List<? extends ObjectDeltaOperation<T>> executedDeltas, ObjectDelta<T> objectDelta) {
        if (executedDeltas == null || executedDeltas.isEmpty()) {
            return objectDelta;
        }

        // any delta related to our OID, not ending with fatal error
        ObjectDeltaOperation<T> lastRelated = findLastRelatedDelta(executedDeltas, objectDelta);
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("findLastRelatedDelta returned:\n{}",
                    lastRelated != null ? lastRelated.shorterDebugDump(1) : "  (null)");
        }
        if (lastRelated == null) {
            return objectDelta; // nothing found, let us apply our delta
        }
        if (lastRelated.getExecutionResult().isSuccess() && lastRelated.containsDelta(objectDelta)) {
            return null; // case 1 - exact match found with SUCCESS result,
            // let's skip the processing of our delta
        }
        if (!objectDelta.isAdd()) {
            if (lastRelated.getObjectDelta().isDelete()) {
                return null;    // case 3
            } else {
                return objectDelta; // MODIFY or DELETE delta after ADD or MODIFY delta - we may safely apply it
            }
        }
        // determine if we got case 2
        if (lastRelated.getObjectDelta().isDelete()) {
            return objectDelta; // we can (and should) apply the ADD delta as a
            // whole, because the object was deleted
        }
        // let us treat the most simple case here - meaning we have existing ADD
        // delta and nothing more
        // TODO add more sophistication if needed
        if (!lastRelated.getObjectDelta().isAdd()) {
            return objectDelta; // this will probably fail, but ...
        }
        // at this point we know that ADD was more-or-less successfully
        // executed, so let's compute the difference, creating a MODIFY delta
        PrismObject<T> alreadyAdded = lastRelated.getObjectDelta().getObjectToAdd();
        PrismObject<T> toBeAddedNow = objectDelta.getObjectToAdd();
        return alreadyAdded.diff(toBeAddedNow);
    }

    private <T extends ObjectType> ObjectDeltaOperation<T> findLastRelatedDelta(
            List<? extends ObjectDeltaOperation<T>> executedDeltas, ObjectDelta<T> objectDelta) {
        for (int i = executedDeltas.size() - 1; i >= 0; i--) {
            ObjectDeltaOperation<T> currentOdo = executedDeltas.get(i);
            if (currentOdo.getExecutionResult().isFatalError()) {
                continue;
            }
            ObjectDelta<T> current = currentOdo.getObjectDelta();

            if (current.equals(objectDelta)) {
                return currentOdo;
            }

            String oid1 = current.isAdd() ? current.getObjectToAdd().getOid() : current.getOid();
            String oid2 = objectDelta.isAdd() ? objectDelta.getObjectToAdd().getOid()
                    : objectDelta.getOid();
            if (oid1 != null && oid2 != null) {
                if (oid1.equals(oid2)) {
                    return currentOdo;
                }
                continue;
            }
            // ADD-MODIFY and ADD-DELETE combinations lead to applying whole
            // delta (as a result of computeDiffDelta)
            // so we can be lazy and check only ADD-ADD combinations here...
            if (!current.isAdd() || !objectDelta.isAdd()) {
                continue;
            }
            // we simply check the type (for focus objects) and
            // resource+kind+intent (for shadows)
            PrismObject<T> currentObject = current.getObjectToAdd();
            PrismObject<T> objectTypeToAdd = objectDelta.getObjectToAdd();
            Class currentObjectClass = currentObject.getCompileTimeClass();
            Class objectTypeToAddClass = objectTypeToAdd.getCompileTimeClass();
            if (currentObjectClass == null || !currentObjectClass.equals(objectTypeToAddClass)) {
                continue;
            }
            if (FocusType.class.isAssignableFrom(currentObjectClass)) {
                return currentOdo; // we suppose there is only one delta of
                // Focus class
            }
        }
        return null;
    }

    private ProvisioningOperationOptions copyFromModelOptions(ModelExecuteOptions options) {
        ProvisioningOperationOptions provisioningOptions = new ProvisioningOperationOptions();
        if (options == null) {
            return provisioningOptions;
        }

        provisioningOptions.setForce(options.getForce());
        provisioningOptions.setOverwrite(options.getOverwrite());
        return provisioningOptions;
    }

    private <F extends ObjectType> ProvisioningOperationOptions getProvisioningOptions(LensContext<F> context,
            ModelExecuteOptions modelOptions, PrismObject<ShadowType> existingShadow, ObjectDelta<ShadowType> delta) throws SecurityViolationException {
        if (modelOptions == null && context != null) {
            modelOptions = context.getOptions();
        }
        ProvisioningOperationOptions provisioningOptions = copyFromModelOptions(modelOptions);

        if (executeAsSelf(context, modelOptions, existingShadow, delta)) {
            LOGGER.trace("Setting 'execute as self' provisioning option for {}", existingShadow);
            provisioningOptions.setRunAsAccountOid(existingShadow.getOid());
        }

        if (context != null && context.getChannel() != null) {

            if (context.getChannel().equals(QNameUtil.qNameToUri(SchemaConstants.CHANGE_CHANNEL_RECON))) {
                // TODO: this is probably wrong. We should not have special case
                // for recon channel! This should be handled by the provisioning task
                // setting the right options there.
                provisioningOptions.setCompletePostponed(false);
            }

            if (context.getChannel().equals(SchemaConstants.CHANGE_CHANNEL_DISCOVERY_URI)) {
                // We want to avoid endless loops in error handling.
                provisioningOptions.setDoNotDiscovery(true);
            }
        }

        return provisioningOptions;
    }

    // This is a bit of black magic. We only want to execute as self if there a user is changing its own password
    // and we also have old password value.
    // Later, this should be improved. Maybe we need special model operation option for this? Or maybe it should be somehow
    // automatically detected based on resource capabilities? We do not know yet. Therefore let's do the simplest possible
    // thing. Otherwise we might do something that we will later regret.
    private <F extends ObjectType> boolean executeAsSelf(LensContext<F> context,
            ModelExecuteOptions modelOptions, PrismObject<ShadowType> existingShadow, ObjectDelta<ShadowType> delta) throws SecurityViolationException {
        if (existingShadow == null) {
            return false;
        }

        if (!SchemaConstants.CHANNEL_GUI_SELF_SERVICE_URI.equals(context.getChannel())) {
            return false;
        }

        if (delta == null) {
            return false;
        }
        if (!delta.isModify()) {
            return false;
        }
        PropertyDelta<ProtectedStringType> passwordDelta = delta.findPropertyDelta(SchemaConstants.PATH_PASSWORD_VALUE);
        if (passwordDelta == null) {
            return false;
        }
        if (passwordDelta.getEstimatedOldValues() == null || passwordDelta.getEstimatedOldValues().isEmpty()) {
            return false;
        }
        ProtectedStringType oldPassword = passwordDelta.getEstimatedOldValues().iterator().next().getValue();
        if (!oldPassword.canGetCleartext()) {
            return false;
        }

        LensFocusContext<F> focusContext = context.getFocusContext();
        if (focusContext == null) {
            return false;
        }
        if (!focusContext.represents(UserType.class)) {
            return false;
        }

        MidPointPrincipal principal = securityContextManager.getPrincipal();
        if (principal == null) {
            return false;
        }
        FocusType loggedInUser = principal.getFocus();

        if (!loggedInUser.getOid().equals(focusContext.getOid())) {
            return false;
        }
        return true;
    }

    private <T extends ObjectType, F extends ObjectType> void logDeltaExecution(ObjectDelta<T> objectDelta,
            LensContext<F> context, ResourceType resource, OperationResult result, Task task) {
        StringBuilder sb = new StringBuilder();
        sb.append("---[ ");
        if (result == null) {
            sb.append("Going to EXECUTE");
        } else {
            sb.append("EXECUTED");
        }
        sb.append(" delta of ").append(objectDelta.getObjectTypeClass().getSimpleName());
        sb.append(" ]---------------------\n");
        DebugUtil.debugDumpLabel(sb, "Channel", 0);
        sb.append(" ").append(LensUtil.getChannel(context, task)).append("\n");
        if (context != null) {
            DebugUtil.debugDumpLabel(sb, "Wave", 0);
            sb.append(" ").append(context.getExecutionWave()).append("\n");
        }
        if (resource != null) {
            sb.append("Resource: ").append(resource.toString()).append("\n");
        }
        sb.append(objectDelta.debugDump());
        sb.append("\n");
        if (result != null) {
            DebugUtil.debugDumpLabel(sb, "Result", 0);
            sb.append(" ").append(result.getStatus()).append(": ").append(result.getMessage());
        }
        sb.append("\n--------------------------------------------------");

        LOGGER.debug("\n{}", sb);
    }

    private <F extends ObjectType> OwnerResolver createOwnerResolver(final LensContext<F> context, Task task,
            OperationResult result) {
        return new LensOwnerResolver<>(context, objectResolver, task, result);
    }

    private <T extends ObjectType, F extends ObjectType> PrismObject<T> executeAddition(ObjectDelta<T> change,
            LensContext<F> context, LensElementContext<T> objectContext, ModelExecuteOptions options,
            ResourceType resource, Task task, OperationResult result) throws ObjectAlreadyExistsException,
            ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            SecurityViolationException, PolicyViolationException, ExpressionEvaluationException, PreconditionViolationException {

        PrismObject<T> objectToAdd = change.getObjectToAdd();

        for (ItemDelta delta : change.getModifications()) {
            delta.applyTo(objectToAdd);
        }
        change.getModifications().clear();

        OwnerResolver ownerResolver = createOwnerResolver(context, task, result);
        T objectTypeToAdd = objectToAdd.asObjectable();
        try {
            securityEnforcer.authorize(ModelAuthorizationAction.ADD.getUrl(),
                    AuthorizationPhaseType.EXECUTION, AuthorizationParameters.Builder.buildObjectAdd(objectToAdd), ownerResolver, task, result);

            metadataManager.applyMetadataAdd(context, objectToAdd, clock.currentTimeXMLGregorianCalendar(), task, result);

            if (options == null) {
                options = context.getOptions();
            }

            RepoAddOptions addOpt = new RepoAddOptions();
            if (ModelExecuteOptions.isOverwrite(options)) {
                addOpt.setOverwrite(true);
            }
            if (ModelExecuteOptions.isNoCrypt(options)) {
                addOpt.setAllowUnencryptedValues(true);
            }

            String oid;
            if (objectTypeToAdd instanceof TaskType) {
                oid = addTask((TaskType) objectTypeToAdd, addOpt, result);
            } else if (objectTypeToAdd instanceof NodeType) {
                throw new UnsupportedOperationException("NodeType cannot be added using model interface");
            } else if (ObjectTypes.isManagedByProvisioning(objectTypeToAdd)) {

                ProvisioningOperationOptions provisioningOptions = getProvisioningOptions(context, options,
                        (PrismObject<ShadowType>) objectContext.getObjectCurrent(), (ObjectDelta<ShadowType>) change);

                oid = addProvisioningObject(objectToAdd, context, objectContext, provisioningOptions,
                        resource, task, result);
                if (oid == null) {
                    throw new SystemException(
                            "Provisioning addObject returned null OID while adding " + objectToAdd);
                }
                result.addReturn("createdAccountOid", oid);
            } else {
                FocusConstraintsChecker.clearCacheFor(objectToAdd.asObjectable().getName());

                oid = cacheRepositoryService.addObject(objectToAdd, addOpt, result);
                if (oid == null) {
                    throw new SystemException("Repository addObject returned null OID while adding " + objectToAdd);
                }
            }
            if (!change.isImmutable()) {
                change.setOid(oid);
            }
            objectToAdd.setOid(oid);
            task.recordObjectActionExecuted(objectToAdd, objectToAdd.getCompileTimeClass(), oid,
                    ChangeType.ADD, context.getChannel(), null);
            return objectToAdd;
        } catch (Throwable t) {
            task.recordObjectActionExecuted(objectToAdd, objectToAdd.getCompileTimeClass(), null,
                    ChangeType.ADD, context.getChannel(), t);
            if (objectTypeToAdd instanceof ShadowType) {
                handleProvisioningError(resource, t, task, result);
                ((LensProjectionContext) objectContext).setSynchronizationPolicyDecision(SynchronizationPolicyDecision.BROKEN);
                return null;
            }
            throw t;

        }
    }

    private void handleProvisioningError(ResourceType resource, Throwable t, Task task, OperationResult result) throws ObjectNotFoundException, ConfigurationException, SecurityViolationException, PolicyViolationException, ExpressionEvaluationException, ObjectAlreadyExistsException, PreconditionViolationException, CommunicationException, SchemaException {
        ErrorSelectorType errorSelectorType = ResourceTypeUtil.getConnectorErrorCriticality(resource);
        CriticalityType criticality = ExceptionUtil.getCriticality(errorSelectorType, t, CriticalityType.FATAL);
        RepoCommonUtils.processErrorCriticality(task, criticality, t, result);
        if (CriticalityType.IGNORE == criticality) {
            result.muteLastSubresultError();
        }
    }

    private <T extends ObjectType, F extends ObjectType> PrismObject<T> executeDeletion(ObjectDelta<T> change,
            LensContext<F> context, LensElementContext<T> objectContext, ModelExecuteOptions options,
            ResourceType resource, Task task, OperationResult result) throws ObjectNotFoundException,
            ObjectAlreadyExistsException, SchemaException, CommunicationException,
            ConfigurationException, SecurityViolationException, PolicyViolationException, ExpressionEvaluationException, PreconditionViolationException {

        String oid = change.getOid();
        Class<T> objectTypeClass = change.getObjectTypeClass();

        PrismObject<T> objectOld = objectContext.getObjectOld();
        OwnerResolver ownerResolver = createOwnerResolver(context, task, result);
        PrismObject<T> objectAfterModification = null;
        try {
            securityEnforcer.authorize(ModelAuthorizationAction.DELETE.getUrl(),
                    AuthorizationPhaseType.EXECUTION, AuthorizationParameters.Builder.buildObjectDelete(objectOld), ownerResolver, task, result);

            if (TaskType.class.isAssignableFrom(objectTypeClass)) {
                taskManager.deleteTask(oid, result);
            } else if (NodeType.class.isAssignableFrom(objectTypeClass)) {
                taskManager.deleteNode(oid, result);
            } else if (workflowManager != null && CaseType.class.isAssignableFrom(objectTypeClass)) {
                workflowManager.deleteCase(oid, task, result);
            } else if (ObjectTypes.isClassManagedByProvisioning(objectTypeClass)) {
                ProvisioningOperationOptions provisioningOptions = getProvisioningOptions(context, options,
                        (PrismObject<ShadowType>) objectContext.getObjectCurrent(), (ObjectDelta<ShadowType>) change);
                try {
                    objectAfterModification = deleteProvisioningObject(objectTypeClass, oid, context, objectContext,
                            provisioningOptions, resource, task, result);
                } catch (ObjectNotFoundException e) {
                    // Object that we wanted to delete is already gone. This can
                    // happen in some race conditions.
                    // As the resulting state is the same as we wanted it to be
                    // we will not complain and we will go on.
                    LOGGER.trace("Attempt to delete object {} ({}) that is already gone", oid,
                            objectTypeClass);
                    result.muteLastSubresultError();
                }
            } else {
                try {
                    cacheRepositoryService.deleteObject(objectTypeClass, oid, result);
                } catch (ObjectNotFoundException e) {
                    // Object that we wanted to delete is already gone. This can
                    // happen in some race conditions.
                    // As the resulting state is the same as we wanted it to be
                    // we will not complain and we will go on.
                    LOGGER.trace("Attempt to delete object {} ({}) that is already gone", oid,
                            objectTypeClass);
                    result.muteLastSubresultError();
                }
            }
            task.recordObjectActionExecuted(objectOld, objectTypeClass, oid, ChangeType.DELETE,
                    context.getChannel(), null);
        } catch (Throwable t) {
            task.recordObjectActionExecuted(objectOld, objectTypeClass, oid, ChangeType.DELETE,
                    context.getChannel(), t);

            if (ShadowType.class.isAssignableFrom(objectTypeClass)) {
                handleProvisioningError(resource, t, task, result);
                return objectContext.getObjectCurrent();
            }

            throw t;
        }

        return objectAfterModification;
    }

    private <T extends ObjectType, F extends ObjectType> void executeModification(ObjectDelta<T> delta,
            LensContext<F> context, LensElementContext<T> objectContext, ModelExecuteOptions options,
            ConflictResolutionType conflictResolution, ResourceType resource, Task task, OperationResult result)
            throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException, CommunicationException,
            ConfigurationException, SecurityViolationException, PolicyViolationException, ExpressionEvaluationException, PreconditionViolationException {
        Class<T> objectTypeClass = delta.getObjectTypeClass();

        // We need current object here. The current object is used to get data for id-only container delete deltas,
        // replace deltas and so on. The authorization code can figure out new object if needed, but it needs
        // current object to start from.
        // We cannot use old object here. That would fail in multi-wave executions. We want object that has all the previous
        // wave changes already applied.
        PrismObject<T> baseObject = objectContext.getObjectCurrent();
        OwnerResolver ownerResolver = createOwnerResolver(context, task, result);
        try {
            securityEnforcer.authorize(ModelAuthorizationAction.MODIFY.getUrl(),
                    AuthorizationPhaseType.EXECUTION, AuthorizationParameters.Builder.buildObjectDelta(baseObject, delta), ownerResolver, task, result);

            if (shouldApplyModifyMetadata(objectTypeClass, context.getSystemConfigurationType())) {
                metadataManager.applyMetadataModify(delta, objectContext, objectTypeClass,
                        clock.currentTimeXMLGregorianCalendar(), task, context, result);
            }

            if (delta.isEmpty()) {
                // Nothing to do
                return;
            }

            if (TaskType.class.isAssignableFrom(objectTypeClass)) {
                taskManager.modifyTask(delta.getOid(), delta.getModifications(), result);
            } else if (NodeType.class.isAssignableFrom(objectTypeClass)) {
                throw new UnsupportedOperationException("NodeType is not modifiable using model interface");
            } else if (ObjectTypes.isClassManagedByProvisioning(objectTypeClass)) {
                ProvisioningOperationOptions provisioningOptions = getProvisioningOptions(context, options,
                        (PrismObject<ShadowType>) objectContext.getObjectCurrent(), (ObjectDelta<ShadowType>) delta);
                String oid = modifyProvisioningObject(objectTypeClass, delta.getOid(),
                        delta.getModifications(), context, objectContext, provisioningOptions, resource,
                        task, result);
                if (!oid.equals(delta.getOid())) {
                    delta.setOid(oid);
                }
            } else {
                FocusConstraintsChecker.clearCacheForDelta(delta.getModifications());
                ModificationPrecondition<T> precondition = null;
                if (conflictResolution != null) {
                    String readVersion = objectContext.getObjectReadVersion();
                    if (readVersion != null) {
                        LOGGER.trace("Modification with precondition, readVersion={}", readVersion);
                        precondition = new VersionPrecondition<>(readVersion);
                    } else {
                        LOGGER.warn("Requested careful modification of {}, but there is no read version", objectContext.getHumanReadableName());
                    }
                }
                cacheRepositoryService.modifyObject(objectTypeClass, delta.getOid(),
                        delta.getModifications(), precondition, null, result);
            }
            task.recordObjectActionExecuted(baseObject, objectTypeClass, delta.getOid(), ChangeType.MODIFY,
                    context.getChannel(), null);
        } catch (Throwable t) {
            task.recordObjectActionExecuted(baseObject, objectTypeClass, delta.getOid(), ChangeType.MODIFY,
                    context.getChannel(), t);
            throw t;
        }
    }

    private <T extends ObjectType> boolean shouldApplyModifyMetadata(Class<T> objectTypeClass, SystemConfigurationType config) {
        if (!ShadowType.class.equals(objectTypeClass)) {
            return true;
        } else if (config == null || config.getInternals() == null || config.getInternals().getShadowMetadataRecording() == null) {
            return true;
        } else {
            MetadataRecordingStrategyType recording = config.getInternals().getShadowMetadataRecording();
            return !Boolean.TRUE.equals(recording.isSkipOnModify());
        }
    }

    private String addTask(TaskType task, RepoAddOptions addOpt, OperationResult result)
            throws ObjectAlreadyExistsException {
        try {
            return taskManager.addTask(task.asPrismObject(), addOpt, result);
        } catch (ObjectAlreadyExistsException ex) {
            throw ex;
        } catch (Exception ex) {
            LoggingUtils.logException(LOGGER, "Couldn't add object {} to task manager", ex, task.getName());
            throw new SystemException(ex.getMessage(), ex);
        }
    }

    private <F extends ObjectType, T extends ObjectType> String addProvisioningObject(PrismObject<T> object,
            LensContext<F> context, LensElementContext<T> objectContext, ProvisioningOperationOptions options,
            ResourceType resource, Task task, OperationResult result) throws ObjectNotFoundException,
            ObjectAlreadyExistsException, SchemaException, CommunicationException,
            ConfigurationException, SecurityViolationException, ExpressionEvaluationException, PolicyViolationException {

        if (object.canRepresent(ShadowType.class)) {
            ShadowType shadow = (ShadowType) object.asObjectable();
            String resourceOid = ShadowUtil.getResourceOid(shadow);
            if (resourceOid == null) {
                throw new IllegalArgumentException("Resource OID is null in shadow");
            }
        }

        OperationProvisioningScriptsType scripts = null;
        if (object.canRepresent(ShadowType.class)) {
            scripts = prepareScripts(object, context, objectContext, ProvisioningOperationTypeType.ADD,
                    resource, task, result);
        }
        ModelImplUtils.setRequestee(task, context);
        String oid = provisioning.addObject(object, scripts, options, task, result);
        ModelImplUtils.clearRequestee(task);
        return oid;
    }

    private <F extends ObjectType, T extends ObjectType> PrismObject<T> deleteProvisioningObject(
            Class<T> objectTypeClass, String oid, LensContext<F> context, LensElementContext<T> objectContext,
            ProvisioningOperationOptions options, ResourceType resource, Task task, OperationResult result)
            throws ObjectNotFoundException, ObjectAlreadyExistsException, SchemaException,
            CommunicationException, ConfigurationException, SecurityViolationException,
            ExpressionEvaluationException, PolicyViolationException {

        PrismObject<T> shadowToModify = null;
        OperationProvisioningScriptsType scripts = null;
        try {
            GetOperationOptions rootOpts = GetOperationOptions.createNoFetch();
            rootOpts.setPointInTimeType(PointInTimeType.FUTURE);
            shadowToModify = provisioning.getObject(objectTypeClass, oid,
                    SelectorOptions.createCollection(rootOpts), task, result);
        } catch (ObjectNotFoundException ex) {
            // this is almost OK, mute the error and try to delete account (it
            // will fail if something is wrong)
            result.muteLastSubresultError();
        }
        if (ShadowType.class.isAssignableFrom(objectTypeClass)) {
            scripts = prepareScripts(shadowToModify, context, objectContext,
                    ProvisioningOperationTypeType.DELETE, resource, task, result);
        }
        ModelImplUtils.setRequestee(task, context);
        PrismObject<T> objectAfterModification = provisioning.deleteObject(objectTypeClass, oid, options, scripts, task, result);
        ModelImplUtils.clearRequestee(task);
        return objectAfterModification;
    }

    private <F extends ObjectType, T extends ObjectType> String modifyProvisioningObject(
            Class<T> objectTypeClass, String oid, Collection<? extends ItemDelta> modifications,
            LensContext<F> context, LensElementContext<T> objectContext, ProvisioningOperationOptions options,
            ResourceType resource, Task task, OperationResult result) throws ObjectNotFoundException,
            CommunicationException, SchemaException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException, ObjectAlreadyExistsException, PolicyViolationException {

        PrismObject<T> shadowToModify = null;
        OperationProvisioningScriptsType scripts = null;
        try {
            GetOperationOptions rootOpts = GetOperationOptions.createNoFetch();
            rootOpts.setPointInTimeType(PointInTimeType.FUTURE);
            shadowToModify = provisioning.getObject(objectTypeClass, oid,
                    SelectorOptions.createCollection(rootOpts), task, result);
        } catch (ObjectNotFoundException e) {
            // We do not want the operation to fail here. The object might have
            // been re-created on the resource
            // or discovery might re-create it. So simply ignore this error and
            // give provisioning a chance to fail
            // properly.
            result.muteLastSubresultError();
            LOGGER.warn("Repository object {}: {} is gone. But trying to modify resource object anyway",
                    objectTypeClass, oid);
        }
        if (ShadowType.class.isAssignableFrom(objectTypeClass)) {
            scripts = prepareScripts(shadowToModify, context, objectContext,
                    ProvisioningOperationTypeType.MODIFY, resource, task, result);
        }
        ModelImplUtils.setRequestee(task, context);
        String changedOid = provisioning.modifyObject(objectTypeClass, oid, modifications, scripts, options,
                task, result);
        ModelImplUtils.clearRequestee(task);
        return changedOid;
    }

    private <F extends ObjectType, T extends ObjectType> OperationProvisioningScriptsType prepareScripts(
            PrismObject<T> changedObject, LensContext<F> context, LensElementContext<T> objectContext,
            ProvisioningOperationTypeType operation, ResourceType resource, Task task, OperationResult result)
            throws ObjectNotFoundException, SchemaException, CommunicationException,
            ConfigurationException, SecurityViolationException, ExpressionEvaluationException {

        if (resource == null) {
            LOGGER.warn("Resource does not exist. Skipping processing scripts.");
            return null;
        }
        OperationProvisioningScriptsType resourceScripts = resource.getScripts();
        PrismObject<ShadowType> resourceObject = (PrismObject<ShadowType>) changedObject;

        PrismObject<F> user = null;
        if (context.getFocusContext() != null) {
            if (context.getFocusContext().getObjectNew() != null) {
                user = context.getFocusContext().getObjectNew();
            } else if (context.getFocusContext().getObjectCurrent() != null) {
                user = context.getFocusContext().getObjectCurrent();
            } else if (context.getFocusContext().getObjectOld() != null) {
                user = context.getFocusContext().getObjectOld();
            }
        }

        LensProjectionContext projectionCtx = (LensProjectionContext) objectContext;
        PrismObject<ShadowType> shadow = null;
        if (projectionCtx.getObjectNew() != null) {
            shadow = projectionCtx.getObjectNew();
        } else if (projectionCtx.getObjectCurrent() != null) {
            shadow = projectionCtx.getObjectCurrent();
        } else {
            shadow = projectionCtx.getObjectOld();
        }

        if (shadow == null) {
            //put at least something
            shadow = resourceObject.clone();
        }

        ResourceShadowDiscriminator discr = ((LensProjectionContext) objectContext)
                .getResourceShadowDiscriminator();

        ExpressionVariables variables = ModelImplUtils.getDefaultExpressionVariables(user, shadow, discr,
                resource.asPrismObject(), context.getSystemConfiguration(), objectContext, prismContext);
        // Having delta in provisioning scripts may be very useful. E.g. the script can optimize execution of expensive operations.
        variables.put(ExpressionConstants.VAR_DELTA, projectionCtx.getDelta(), ObjectDelta.class);
        ExpressionProfile expressionProfile = MiscSchemaUtil.getExpressionProfile();
        ModelExpressionThreadLocalHolder.pushExpressionEnvironment(new ExpressionEnvironment<>(context, (LensProjectionContext) objectContext, task, result));
        try {
            return evaluateScript(resourceScripts, discr, operation, null, variables, expressionProfile, context, objectContext, task, result);
        } finally {
            ModelExpressionThreadLocalHolder.popExpressionEnvironment();
        }

    }

    private OperationProvisioningScriptsType evaluateScript(OperationProvisioningScriptsType resourceScripts,
            ResourceShadowDiscriminator discr, ProvisioningOperationTypeType operation, BeforeAfterType order,
            ExpressionVariables variables, ExpressionProfile expressionProfile, LensContext<?> context,
            LensElementContext<?> objectContext, Task task,
            OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
        OperationProvisioningScriptsType outScripts = new OperationProvisioningScriptsType();

        if (resourceScripts != null) {
            OperationProvisioningScriptsType scripts = resourceScripts.clone();
            for (OperationProvisioningScriptType script : scripts.getScript()) {
                if (discr != null) {
                    if (script.getKind() != null && !script.getKind().isEmpty()
                            && !script.getKind().contains(discr.getKind())) {
                        continue;
                    }
                    if (script.getIntent() != null && !script.getIntent().isEmpty()
                            && !script.getIntent().contains(discr.getIntent()) && discr.getIntent() != null) {
                        continue;
                    }
                }
                if (operation != null) {
                    if (!script.getOperation().contains(operation)) {
                        continue;
                    }
                }
                if (order != null) {
                    if (order != null && order != script.getOrder()) {
                        continue;
                    }
                }
                // Let's do the most expensive evaluation last
                if (!evaluateScriptCondition(script, variables, expressionProfile, task, result)) {
                    continue;
                }
                for (ProvisioningScriptArgumentType argument : script.getArgument()) {
                    evaluateScriptArgument(argument, variables, context, objectContext, task, result);
                }
                outScripts.getScript().add(script);
            }
        }

        return outScripts;
    }

    private boolean evaluateScriptCondition(OperationProvisioningScriptType script,
            ExpressionVariables variables, ExpressionProfile expressionProfile, Task task, OperationResult result) throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {
        ExpressionType condition = script.getCondition();
        if (condition == null) {
            return true;
        }

        PrismPropertyValue<Boolean> conditionOutput = ExpressionUtil.evaluateCondition(variables, condition, expressionProfile, expressionFactory, " condition for provisioning script ", task, result);
        if (conditionOutput == null) {
            return true;
        }

        Boolean conditionOutputValue = conditionOutput.getValue();

        return BooleanUtils.isNotFalse(conditionOutputValue);

    }

    private void evaluateScriptArgument(ProvisioningScriptArgumentType argument,
            ExpressionVariables variables, LensContext<?> context,
            LensElementContext<?> objectContext, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, SecurityViolationException {

        final QName fakeScriptArgumentName = new QName(SchemaConstants.NS_C, "arg");

        PrismPropertyDefinition<String> scriptArgumentDefinition = prismContext.definitionFactory().createPropertyDefinition(
                fakeScriptArgumentName, DOMUtil.XSD_STRING);

        String shortDesc = "Provisioning script argument expression";
        Expression<PrismPropertyValue<String>, PrismPropertyDefinition<String>> expression = expressionFactory
                .makeExpression(argument, scriptArgumentDefinition, MiscSchemaUtil.getExpressionProfile(), shortDesc, task, result);

        ExpressionEvaluationContext params = new ExpressionEvaluationContext(null, variables, shortDesc, task);
        ExpressionEnvironment<?, ?, ?> env = new ExpressionEnvironment<>(context,
                objectContext instanceof LensProjectionContext ? (LensProjectionContext) objectContext : null, task, result);
        PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = ModelExpressionThreadLocalHolder
                .evaluateExpressionInContext(expression, params, env, result);

        Collection<PrismPropertyValue<String>> nonNegativeValues = null;
        if (outputTriple != null) {
            nonNegativeValues = outputTriple.getNonNegativeValues();
        }

        // replace dynamic script with static value..
        XNodeFactory factory = prismContext.xnodeFactory();

        argument.getExpressionEvaluator().clear();
        if (nonNegativeValues == null || nonNegativeValues.isEmpty()) {
            // We need to create at least one evaluator. Otherwise the
            // expression code will complain
            JAXBElement<RawType> el = new JAXBElement<>(SchemaConstants.C_VALUE, RawType.class, new RawType(prismContext));
            argument.getExpressionEvaluator().add(el);

        } else {
            for (PrismPropertyValue<String> val : nonNegativeValues) {
                PrimitiveXNode<String> prim = factory.primitive(val.getValue(), DOMUtil.XSD_STRING);
                JAXBElement<RawType> el = new JAXBElement<>(SchemaConstants.C_VALUE, RawType.class, new RawType(prim, prismContext));
                argument.getExpressionEvaluator().add(el);
            }
        }
    }

    private <T extends ObjectType, F extends ObjectType> void executeReconciliationScript(
            LensProjectionContext projContext, LensContext<F> context, BeforeAfterType order, Task task,
            OperationResult parentResult) throws SchemaException, ObjectNotFoundException,
            ExpressionEvaluationException, CommunicationException, ConfigurationException,
            SecurityViolationException, ObjectAlreadyExistsException {

        if (!projContext.isDoReconciliation()) {
            return;
        }

        ResourceType resource = projContext.getResource();
        if (resource == null) {
            LOGGER.warn("Resource does not exist. Skipping processing reconciliation scripts.");
            return;
        }

        OperationProvisioningScriptsType resourceScripts = resource.getScripts();
        if (resourceScripts == null) {
            return;
        }
        ExpressionProfile expressionProfile = MiscSchemaUtil.getExpressionProfile();
        executeProvisioningScripts(context, projContext, resourceScripts, ProvisioningOperationTypeType.RECONCILE, order, expressionProfile, task, parentResult);
    }

    private <T extends ObjectType, F extends ObjectType> Object executeProvisioningScripts(LensContext<F> context, LensProjectionContext projContext,
            OperationProvisioningScriptsType scripts, ProvisioningOperationTypeType operation, BeforeAfterType order, ExpressionProfile expressionProfile, Task task, OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, SecurityViolationException, ObjectAlreadyExistsException {

        ResourceType resource = projContext.getResource();
        if (resource == null) {
            LOGGER.warn("Resource does not exist. Skipping processing reconciliation scripts.");
            return null;
        }

        PrismObject<F> user = null;
        PrismObject<ShadowType> shadow = null;

        if (context.getFocusContext() != null) {
            if (context.getFocusContext().getObjectNew() != null) {
                user = context.getFocusContext().getObjectNew();
            } else if (context.getFocusContext().getObjectOld() != null) {
                user = context.getFocusContext().getObjectOld();
            }
            // if (order == ProvisioningScriptOrderType.BEFORE) {
            // user = context.getFocusContext().getObjectOld();
            // } else if (order == ProvisioningScriptOrderType.AFTER) {
            // user = context.getFocusContext().getObjectNew();
            // } else {
            // throw new IllegalArgumentException("Unknown order "+order);
            // }
        }

        if (order == BeforeAfterType.BEFORE) {
            shadow = (PrismObject<ShadowType>) projContext.getObjectOld();
        } else if (order == BeforeAfterType.AFTER) {
            shadow = (PrismObject<ShadowType>) projContext.getObjectNew();
        } else {
            shadow = (PrismObject<ShadowType>) projContext.getObjectCurrent();
        }

        ExpressionVariables variables = ModelImplUtils.getDefaultExpressionVariables(user, shadow,
                projContext.getResourceShadowDiscriminator(), resource.asPrismObject(),
                context.getSystemConfiguration(), projContext, prismContext);
        Object scriptResult = null;
        ModelExpressionThreadLocalHolder.pushExpressionEnvironment(new ExpressionEnvironment<>(context, projContext, task, parentResult));
        try {
            OperationProvisioningScriptsType evaluatedScript = evaluateScript(scripts,
                    projContext.getResourceShadowDiscriminator(), operation, order,
                    variables, expressionProfile, context, projContext, task, parentResult);
            for (OperationProvisioningScriptType script : evaluatedScript.getScript()) {
                ModelImplUtils.setRequestee(task, context);
                scriptResult = provisioning.executeScript(resource.getOid(), script, task, parentResult);
                ModelImplUtils.clearRequestee(task);
            }
        } finally {
            ModelExpressionThreadLocalHolder.popExpressionEnvironment();
        }

        return scriptResult;
    }

}
