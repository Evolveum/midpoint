/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.executor;

import com.evolveum.midpoint.model.api.ModelAuthorizationAction;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.context.ProjectionContextKey;
import com.evolveum.midpoint.model.common.expression.ModelExpressionEnvironment;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.lens.*;
import com.evolveum.midpoint.model.impl.lens.projector.focus.FocusConstraintsChecker;
import com.evolveum.midpoint.model.impl.lens.projector.focus.ProjectionMappingSetEvaluator;
import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.*;
import com.evolveum.midpoint.prism.equivalence.EquivalenceStrategy;
import com.evolveum.midpoint.provisioning.api.ProvisioningOperationContext;
import com.evolveum.midpoint.provisioning.api.ProvisioningOperationOptions;
import com.evolveum.midpoint.provisioning.api.ShadowLivenessState;
import com.evolveum.midpoint.repo.api.ModificationPrecondition;
import com.evolveum.midpoint.repo.api.PreconditionViolationException;
import com.evolveum.midpoint.repo.api.RepoAddOptions;
import com.evolveum.midpoint.repo.api.VersionPrecondition;
import com.evolveum.midpoint.repo.common.expression.ExpressionEnvironmentThreadLocalHolder;
import com.evolveum.midpoint.repo.common.util.RepoCommonUtils;
import com.evolveum.midpoint.schema.*;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.expression.ExpressionProfile;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ExceptionUtil;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.enforcer.api.AuthorizationParameters;
import com.evolveum.midpoint.security.enforcer.api.SecurityEnforcer;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static com.evolveum.midpoint.model.impl.lens.ChangeExecutor.OPERATION_EXECUTE_DELTA;
import static com.evolveum.midpoint.prism.PrismObject.asObjectable;
import static com.evolveum.midpoint.prism.PrismObject.cast;
import static com.evolveum.midpoint.schema.internals.InternalsConfig.consistencyChecks;
import static com.evolveum.midpoint.util.DebugUtil.lazy;
import static com.evolveum.midpoint.util.MiscUtil.*;

/**
 * Executes specified delta. Chooses appropriate component (repo, provisioning, task manager, and so on).
 *
 * Main responsibilities:
 *
 * 1. Checks for redundancy (w.r.t. executed deltas and current focus object)
 * 2. Authorizes the execution
 * 3. Applies metadata
 * 4. Calls appropriate component (provisioning, task manager, repository)
 * 5. Executes simulated deltas right on `objectCurrent` in {@link LensElementContext}.
 *
 * @param <O> type of the lens context
 * @param <E> type of the element context (whose delta is being executed)
 */
class DeltaExecution<O extends ObjectType, E extends ObjectType> {

    /** For the time being we keep the parent logger name. */
    private static final Trace LOGGER = TraceManager.getTrace(ChangeExecutor.class);

    /** Main context. */
    @NotNull private final LensContext<O> context;

    /** Context whose delta is to be executed. */
    @NotNull private final LensElementContext<E> elementContext;

    /** Delta to execute. Can be different from the elementContext.delta. Refined during processing. */
    @NotNull private ObjectDelta<E> delta;

    /**
     * Delta that was really executed (or attempted to be executed). It is the {@link #delta} minus items
     * whose changes should not be applied.
     */
    private ObjectDelta<E> deltaForExecution;

    /** How should we resolve conflicts? */
    private final ConflictResolutionType conflictResolution;

    /** Resource related to element context, if any. */
    private final ResourceType resource;

    @NotNull private final Task task;
    private final ModelBeans b = ModelBeans.get();

    /**
     * Estimate of the object after modification. Or null if the object was deleted.
     *
     * NOTE: This is only partially implemented. For modifications it is always null.
     */
    @Experimental
    private PrismObject<E> objectAfterModification;

    /**
     * Liveness state of the shadow after operation.
     * Null if the operation was not executed or object is not a shadow.
     */
    private ShadowLivenessState shadowLivenessState;

    /**
     * True if the object was successfully deleted.
     * Currently supported for repository-managed objects (e.g. focus).
     * The meaning for projections is undefined.
     */
    private boolean deleted;

    /** The same object as is put into {@link LensElementContext#lastChangeExecutionResult} */
    private final ChangeExecutionResult<E> changeExecutionResult;

    DeltaExecution(
            @NotNull LensElementContext<E> elementContext,
            ObjectDelta<E> delta,
            ConflictResolutionType conflictResolution,
            @NotNull Task task,
            @NotNull ChangeExecutionResult<E> changeExecutionResult) {

        //noinspection unchecked
        this.context = (LensContext<O>) elementContext.getLensContext();
        this.elementContext = elementContext;
        this.delta = java.util.Objects.requireNonNull(delta, "null delta");
        this.conflictResolution = conflictResolution;
        this.resource = elementContext instanceof LensProjectionContext ?
                ((LensProjectionContext) elementContext).getResource() : null;
        this.task = task;
        this.changeExecutionResult = changeExecutionResult;
    }

    //region Main
    public void execute(OperationResult parentResult) throws SchemaException, CommunicationException,
            ObjectAlreadyExistsException, ExpressionEvaluationException, PolicyViolationException,
            SecurityViolationException, ConfigurationException, ObjectNotFoundException, ConflictDetectedException {

        elementContext.resolveTemporaryContainerIds(delta);

        if (delta.getOid() == null) {
            delta.setOid(elementContext.getOid());
        }

        ObjectDelta<E> effectiveDelta = computeEffectiveDelta();
        if (ObjectDelta.isEmpty(effectiveDelta)) {
            LOGGER.debug("Skipping execution of delta because it was already executed: {}", elementContext.getHumanReadableName());
            if (elementContext instanceof LensFocusContext<?> && context.hasProjectionChange()) {
                LOGGER.debug("However, there was a projection change, so we have to record that");
                if (effectiveDelta != null) {
                    delta = effectiveDelta;
                } else {
                    delta = PrismContext.get().deltaFactory().object().createEmptyModifyDelta(
                            elementContext.getObjectTypeClass(), elementContext.getOid());
                }
            } else {
                objectAfterModification = elementContext.getObjectCurrent();
                return;
            }
        } else {
            delta = effectiveDelta;
        }

        checkDeltaConsistence();

        LensUtil.setDeltaOldValue(elementContext, delta);

        logDeltaExecutionStart();

        OperationResult result = parentResult.createSubresult(OPERATION_EXECUTE_DELTA);
        try {
            deltaForExecution = delta; // Overwritten only if needed; present here e.g. because of potential exceptions
            if (delta.getChangeType() == ChangeType.ADD) {
                executeAddition(result);
            } else if (delta.getChangeType() == ChangeType.MODIFY) {
                executeModification(result);
            } else if (delta.getChangeType() == ChangeType.DELETE) {
                executeDeletion(result);
            }

        } finally {
            result.computeStatusIfUnknown();

            LensObjectDeltaOperation<E> objectDeltaOp = addToExecutedDeltas(result);

            changeExecutionResult.setExecutedOperation(objectDeltaOp);
            addTrace(objectDeltaOp, result);

            logDeltaExecutionEnd(result);
        }
    }
    //endregion Main

    //region Delta processing
    private void checkDeltaConsistence() throws SchemaException {
        if (consistencyChecks) {
            delta.checkConsistence(ConsistencyCheckScope.THOROUGH);
        }

        // Other types than focus types may not be definition-complete (e.g.
        // accounts and resources are completed in provisioning)
        if (FocusType.class.isAssignableFrom(delta.getObjectTypeClass())) {
            delta.assertDefinitions();
        }
    }

    /**
     * Computes delta to execute, given a list of already executes deltas and current object (for focus).
     */
    private ObjectDelta<E> computeEffectiveDelta() throws SchemaException {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Computing effective delta from delta:\n{}\nGiven these executed deltas:\n{}",
                    delta.debugDump(1),
                    LensObjectDeltaOperation.shorterDebugDump(elementContext.getExecutedDeltas(), 1));
        }
        ObjectDelta<E> consideringExecutedDeltas = treatExecutedDeltas();
        ObjectDelta<E> effectiveDelta = treatFocusObjectCurrent(consideringExecutedDeltas);
        LOGGER.trace("Delta after executed deltas and current focus are considered:\n{}",
                DebugUtil.debugDumpLazily(effectiveDelta, 1));
        return effectiveDelta;
    }

    /**
     * Compute a "difference delta" - given that executedDeltas were executed,
     * and objectDelta is about to be executed; eliminates parts that have
     * already been done. It is meant as a kind of optimization (for MODIFY
     * deltas) and error avoidance (for ADD deltas).
     *
     * Explanation for ADD deltas: there are situations where an execution wave
     * is restarted - when unexpected AlreadyExistsException is reported from
     * provisioning. However, in such cases, duplicate ADD Focus deltas were
     * generated. So we (TEMPORARILY!) decided to filter them out here.
     *
     * Unfortunately, this mechanism is not well-defined, and seems to work more
     * "by accident" than "by design". It should be replaced with something more
     * serious. Perhaps by re-reading current focus state when repeating a wave?
     *
     * Anyway, currently it treats only three cases:
     *
     * 1. if the objectDelta is present in the list of executed deltas
     * 2. if the objectDelta is ADD, and another ADD delta is there (then the difference is computed)
     * 3. if objectDelta is MODIFY or DELETE and previous delta was MODIFY
     */
    private ObjectDelta<E> treatExecutedDeltas() {
        List<? extends ObjectDeltaOperation<E>> executedDeltas = elementContext.getExecutedDeltas();
        if (executedDeltas.isEmpty()) {
            return delta;
        }

        // any delta related to our OID, not ending with fatal error
        ObjectDeltaOperation<E> lastRelated = findLastRelatedDelta(executedDeltas);
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("findLastRelatedDelta returned:\n{}",
                    lastRelated != null ? lastRelated.shorterDebugDump(1) : "  (null)");
        }
        if (lastRelated == null) {
            // nothing found, let us apply our delta
            return delta;
        }
        OperationResult lastRelatedResult = lastRelated.getExecutionResult();
        if (lastRelatedResult != null &&
                lastRelatedResult.isSuccess() &&
                lastRelated.containsDelta(delta, EquivalenceStrategy.DATA)) {
            // Case 1 - exact match found with SUCCESS result, let's skip the processing of our delta.
            // Equivalence strategy DATA is used to consider the metadata as well.
            return null;
        }
        if (!delta.isAdd()) {
            if (lastRelated.getObjectDelta().isDelete()) {
                // case 3
                return null;
            } else {
                // MODIFY or DELETE delta after ADD or MODIFY delta - we may safely apply it
                return delta;
            }
        }
        // determine if we got case 2
        if (lastRelated.getObjectDelta().isDelete()) {
            // we can (and should) apply the ADD delta as a whole, because the object was deleted
            return delta;
        }
        // let us treat the most simple case here - meaning we have existing ADD delta and nothing more
        // TODO add more sophistication if needed
        if (!lastRelated.getObjectDelta().isAdd()) {
            // this will probably fail, but ...
            return delta;
        }
        // at this point we know that ADD was more-or-less successfully
        // executed, so let's compute the difference, creating a MODIFY delta
        PrismObject<E> alreadyAdded = lastRelated.getObjectDelta().getObjectToAdd();
        PrismObject<E> toBeAddedNow = delta.getObjectToAdd();
        return alreadyAdded.diff(toBeAddedNow);
    }

    private ObjectDeltaOperation<E> findLastRelatedDelta(List<? extends ObjectDeltaOperation<E>> executedDeltas) {
        for (int i = executedDeltas.size() - 1; i >= 0; i--) {
            ObjectDeltaOperation<E> currentOdo = executedDeltas.get(i);
            OperationResult currentOdoResult = currentOdo.getExecutionResult();
            if (currentOdoResult != null && currentOdoResult.isFatalError()) {
                continue;
            }
            ObjectDelta<E> currentDelta = currentOdo.getObjectDelta();

            if (currentDelta.equals(delta)) {
                return currentOdo;
            }

            String oid1 = currentDelta.isAdd() ? currentDelta.getObjectToAdd().getOid() : currentDelta.getOid();
            String oid2 = delta.isAdd() ? delta.getObjectToAdd().getOid() : delta.getOid();
            if (oid1 != null && oid2 != null) {
                if (oid1.equals(oid2)) {
                    return currentOdo;
                } else {
                    continue;
                }
            }
            // ADD-MODIFY and ADD-DELETE combinations lead to applying whole
            // delta (as a result of computeDiffDelta)
            // so we can be lazy and check only ADD-ADD combinations here...
            if (!currentDelta.isAdd() || !delta.isAdd()) {
                continue;
            }
            // we simply check the type (for focus objects) and
            // resource+kind+intent (for shadows)
            PrismObject<E> objectAdded = currentDelta.getObjectToAdd();
            PrismObject<E> objectToAdd = delta.getObjectToAdd();
            Class<E> objectAddedClass = objectAdded.getCompileTimeClass();
            Class<E> objectToAddClass = objectToAdd.getCompileTimeClass();
            if (objectAddedClass == null || !objectAddedClass.equals(objectToAddClass)) {
                continue;
            }
            if (FocusType.class.isAssignableFrom(objectAddedClass)) {
                return currentOdo; // we suppose there is only one delta of Focus class (shouldn't this be AssignmentHolderType?)
            }
        }
        return null;
    }

    /**
     * Is the computed delta idempotent related to objectCurrent?
     *
     * Currently we deal only with focusContext because of safety; and also because this check is a reaction
     * in change to focus context secondary delta swallowing code (MID-5207).
     *
     * LookupTableType operation optimization is not available here, because it looks like that isRedundant
     * does not work reliably for key-based row deletions (MID-5276).
     *
     * Comparing using DATA: we consider value metadata as making a difference
     */
    @Nullable
    private ObjectDelta<E> treatFocusObjectCurrent(ObjectDelta<E> delta) throws SchemaException {
        if (delta == null) {
            return null;
        }
        if (!(elementContext instanceof LensFocusContext<?>)) {
            return delta;
        }
        if (elementContext.isOfType(LookupTableType.class)) {
            return delta;
        }
        if (!delta.isRedundant(elementContext.getObjectCurrent(), EquivalenceStrategy.DATA,
                EquivalenceStrategy.REAL_VALUE_CONSIDER_DIFFERENT_IDS, false)) {
            return delta;
        }
        LOGGER.trace("delta is redundant related to {}", elementContext.getObjectCurrent());
        return null;
    }

    @NotNull
    private LensObjectDeltaOperation<E> addToExecutedDeltas(OperationResult result) throws SchemaException {
        if (!deltaForExecution.hasCompleteDefinition()) { // TODO reconsider this
            throw new SchemaException("object delta does not have complete definition");
        }
        var objectDeltaOp = LensUtil.createObjectDeltaOperation(deltaForExecution.clone(), result, elementContext, resource);
        LOGGER.trace("Recording executed delta:\n{}", lazy(() -> objectDeltaOp.shorterDebugDump(1)));
        elementContext.addToExecutedDeltas(objectDeltaOp);
        return objectDeltaOp;
    }

    private void addTrace(LensObjectDeltaOperation<E> objectDeltaOp, OperationResult result) throws SchemaException {
        if (result.isTracingNormal(ModelExecuteDeltaTraceType.class)) {
            TraceType trace = new ModelExecuteDeltaTraceType()
                    .delta(objectDeltaOp.clone().toLensObjectDeltaOperationBean()); // todo kill operation result?
            result.addTrace(trace);
        }
    }

    private void logDeltaExecutionStart() {
        if (LOGGER.isTraceEnabled()) {
            // We log the execution start only on trace level
            logDeltaExecution(null);
        }
    }

    private void logDeltaExecutionEnd(OperationResult result) {
        if (LOGGER.isDebugEnabled()) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("EXECUTION result {}", result.getLastSubresult());
            } else {
                // Execution of deltas was not logged yet
                logDeltaExecution(result.getLastSubresult());
            }
        }
    }

    private void logDeltaExecution(OperationResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append("---[ ");
        if (result == null) {
            sb.append("Going to EXECUTE");
        } else {
            sb.append("EXECUTED");
        }
        sb.append(" delta of ").append(delta.getObjectTypeClass().getSimpleName());
        sb.append(" ]---------------------\n");
        DebugUtil.debugDumpLabel(sb, "Channel", 0);
        sb.append(" ").append(LensUtil.getChannel(context, task)).append("\n");
        DebugUtil.debugDumpLabel(sb, "Wave", 0);
        sb.append(" ").append(context.getExecutionWave()).append("\n");
        if (resource != null) {
            sb.append("Resource: ").append(resource).append("\n");
        }
        sb.append(delta.debugDump());
        sb.append("\n");
        if (result != null) {
            DebugUtil.debugDumpLabel(sb, "Result", 0);
            sb.append(" ").append(result.getStatus()).append(": ").append(result.getMessage());
        }
        sb.append("\n--------------------------------------------------");

        LOGGER.debug("\n{}", sb);
    }
    //endregion

    //region Addition
    private void executeAddition(OperationResult result)
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, CommunicationException,
            ConfigurationException, SecurityViolationException, PolicyViolationException, ExpressionEvaluationException {

        stateCheck(!delta.isImmutable(), "Immutable delta? In %s", elementContext);

        // Note that the "object to add" is a live object in the delta
        PrismObject<E> objectToAdd = delta.getObjectToAdd();

        E objectBeanToAdd = objectToAdd.asObjectable();
        try {
            b.securityEnforcer.authorize(ModelAuthorizationAction.ADD.getUrl(),
                    AuthorizationPhaseType.EXECUTION, AuthorizationParameters.Builder.buildObjectAdd(objectToAdd),
                    enforcerOptionsWithLensOwnerResolver(result), task, result);

            if (elementContext instanceof LensFocusContext<E> focusContext && focusContext.isOfType(AssignmentHolderType.class)) {
                resolveAssignmentIdentifiersOnAdd((AssignmentHolderType) objectBeanToAdd, focusContext.getAssignmentIdStore());
            }

            b.metadataManager.applyMetadataOnObjectAddOp(context, objectToAdd, b.clock.currentTimeXMLGregorianCalendar(), task);
            b.indexingManager.updateIndexDataOnElementAdd(objectBeanToAdd, elementContext, task, result);
            b.taskOperationalDataManager.updateOnElementAdd(objectBeanToAdd, result);

            deltaForExecution = processChangeApplicationMode(result);
            objectToAdd = deltaForExecution.getObjectToAdd();
            String oid;
            if (task.isExecutionFullyPersistent()) {
                oid = executeRealAddition(objectToAdd, result);
            } else {
                oid = executeSimulatedAddition(objectToAdd);
            }
            delta.setOid(oid);
            if (!objectToAdd.isImmutable()) {
                objectToAdd.setOid(oid);
            }
            LensUtil.setContextOid(context, elementContext, oid);

            task.recordObjectActionExecuted(
                    objectToAdd, objectToAdd.getCompileTimeClass(), oid, ChangeType.ADD, context.getChannel(), null);
            objectAfterModification = objectToAdd;
        } catch (Throwable t) {
            task.recordObjectActionExecuted(
                    objectToAdd, objectToAdd.getCompileTimeClass(), null, ChangeType.ADD, context.getChannel(), t);
            if (objectBeanToAdd instanceof ShadowType) {
                handleProvisioningError(resource, t, result);
                assert elementContext instanceof LensProjectionContext;
                ((LensProjectionContext) elementContext).setBroken();
                objectAfterModification = null;
            }
            throw t;
        }
    }

    private void resolveAssignmentIdentifiersOnAdd(AssignmentHolderType objectToAdd, AssignmentIdStore assignmentIdStore) {
        if (!assignmentIdStore.isEmpty()) {
            objectToAdd.getAssignment().forEach(assignment -> {
                if (assignment.getId() == null) {
                    Long externalId = assignmentIdStore.getKnownExternalId(assignment);
                    if (externalId != null) {
                        assignment.setId(externalId);
                    }
                }
            });
        }
    }

    private String executeRealAddition(PrismObject<E> objectToAdd, OperationResult result)
            throws ObjectAlreadyExistsException, SchemaException, ObjectNotFoundException, CommunicationException,
            ConfigurationException, SecurityViolationException, ExpressionEvaluationException, PolicyViolationException {
        E objectBeanToAdd = objectToAdd.asObjectable();
        String oid;
        if (objectBeanToAdd instanceof TaskType) {
            oid = b.taskManager.addTask(cast(objectToAdd, TaskType.class), createRepoAddOptions(), result);
        } else if (objectBeanToAdd instanceof NodeType) {
            throw new UnsupportedOperationException("NodeType cannot be added using model interface");
        } else if (ObjectTypes.isManagedByProvisioning(objectBeanToAdd)) {
            oid = addProvisioningObject(objectToAdd, result);
            if (oid == null) {
                throw new SystemException("Provisioning addObject returned null OID while adding " + objectToAdd);
            }
            if (objectBeanToAdd instanceof ShadowType) {
                // Even if the resource object is not created on the resource (e.g. because of error or because the
                // resource is manual), we know the shadow exists. And we assume the shadow is live.
                // TODO reconsider if this assumption is valid.
                shadowLivenessState = ShadowLivenessState.LIVE;
            }
            result.addReturn("createdAccountOid", oid);
        } else {
            FocusConstraintsChecker.clearCacheFor(objectToAdd.asObjectable().getName());
            oid = b.cacheRepositoryService.addObject(objectToAdd, createRepoAddOptions(), result);
        }
        return oid;
    }

    private String executeSimulatedAddition(PrismObject<E> objectToAdd) throws SchemaException {
        if (objectToAdd.getOid() == null) {
            objectToAdd.setOid(
                    UUID.randomUUID().toString());
        }
        elementContext.simulateDeltaExecution(objectToAdd.createAddDelta());
        return objectToAdd.getOid();
    }

    private String addProvisioningObject(PrismObject<E> object, OperationResult result)
            throws ObjectNotFoundException, ObjectAlreadyExistsException, SchemaException, CommunicationException,
            ConfigurationException, SecurityViolationException, ExpressionEvaluationException, PolicyViolationException {

        OperationProvisioningScriptsType scripts;
        if (object.canRepresent(ShadowType.class)) {
            ShadowType shadow = (ShadowType) object.asObjectable();
            argCheck(ShadowUtil.getResourceOid(shadow) != null, "Resource OID is null in shadow");
            scripts = prepareScripts(object, ProvisioningOperationTypeType.ADD, result);
        } else {
            scripts = null;
        }

        ModelImplUtils.setRequestee(task, context);
        try {
            ProvisioningOperationOptions options = getProvisioningOptions();
            ProvisioningOperationContext ctx = context.createProvisioningOperationContext();

            return b.provisioningService.addObject(object, scripts, options, ctx, task, result);
        } finally {
            ModelImplUtils.clearRequestee(task);
        }
    }

    @NotNull
    private RepoAddOptions createRepoAddOptions() {
        ModelExecuteOptions options = context.getOptions();
        RepoAddOptions addOpt = new RepoAddOptions();
        if (ModelExecuteOptions.isOverwrite(options)) {
            addOpt.setOverwrite(true);
        }
        if (ModelExecuteOptions.isNoCrypt(options)) {
            addOpt.setAllowUnencryptedValues(true);
        }
        return addOpt;
    }
    //endregion

    //region Modification
    private void executeModification(OperationResult result)
            throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException, CommunicationException,
            ConfigurationException, SecurityViolationException, PolicyViolationException, ExpressionEvaluationException,
            ConflictDetectedException {

        Class<E> objectClass = delta.getObjectTypeClass();

        // We need current object here. The current object is used to get data for id-only container delete deltas,
        // replace deltas and so on. The authorization code can figure out new object if needed, but it needs
        // current object to start from.
        // We cannot use old object here. That would fail in multi-wave executions. We want object that has all the previous
        // wave changes already applied.
        PrismObject<E> baseObject = elementContext.getObjectCurrent();
        try {
            if (!delta.isEmpty()) {
                b.securityEnforcer.authorize(
                        ModelAuthorizationAction.MODIFY.getUrl(),
                        AuthorizationPhaseType.EXECUTION,
                        AuthorizationParameters.Builder.buildObjectDelta(baseObject, delta, true),
                        enforcerOptionsWithLensOwnerResolver(result),
                        task,
                        result);
            } else {
                // No need to authorize empty delta, because it means that we will update only the metadata
                // (which is explicitly allowed in execution phase anyway).
            }

            if (elementContext instanceof LensFocusContext<E> focusContext && focusContext.isOfType(AssignmentHolderType.class)) {
                resolveAssignmentIdentifiersOnModify(focusContext.getAssignmentIdStore());
            }

            if (shouldApplyModifyMetadata(objectClass)) {
                b.metadataManager.applyMetadataOnObjectModifyOp(
                        delta, elementContext, b.clock.currentTimeXMLGregorianCalendar(), task, context);
            }
            b.indexingManager.updateIndexDataOnElementModify(
                    asObjectable(baseObject), delta, objectClass, elementContext, task, result);
            b.taskOperationalDataManager.updateOnElementModify(
                    asObjectable(baseObject), delta, objectClass, elementContext, result);

            if (delta.isEmpty()) {
                // Nothing to do
                return;
            }

            deltaForExecution = processChangeApplicationMode(result);
            if (task.isExecutionFullyPersistent()) {
                executeRealModification(objectClass, result);
            } else {
                elementContext.simulateDeltaExecution(deltaForExecution);
            }
            task.recordObjectActionExecuted(
                    baseObject, objectClass, delta.getOid(), ChangeType.MODIFY, context.getChannel(), null);
        } catch (Throwable t) {
            task.recordObjectActionExecuted(
                    baseObject, objectClass, delta.getOid(), ChangeType.MODIFY, context.getChannel(), t);
            throw t;
        }
    }

    private void resolveAssignmentIdentifiersOnModify(AssignmentIdStore assignmentIdStore) {
        if (assignmentIdStore.isEmpty()) {
            return;
        }
        for (ItemDelta<?, ?> modification : delta.getModifications()) {
            if (modification.getPath().equivalent(AssignmentHolderType.F_ASSIGNMENT)) {
                resolveAssignmentIdentifiersInPcvs(modification.getValuesToAdd(), assignmentIdStore);
                resolveAssignmentIdentifiersInPcvs(modification.getValuesToReplace(), assignmentIdStore);
            }
        }
    }

    private void resolveAssignmentIdentifiersInPcvs(
            Collection<? extends PrismValue> values, AssignmentIdStore assignmentIdStore) {
        for (PrismValue value : emptyIfNull(values)) {
            PrismContainerValue<?> pcv = (PrismContainerValue<?>) value;
            if (pcv.getId() == null) {
                var externalId = assignmentIdStore.getKnownExternalId((AssignmentType) pcv.asContainerable());
                if (externalId != null) {
                    pcv.setId(externalId);
                }
            }
        }
    }

    private @NotNull ObjectDelta<E> processChangeApplicationMode(OperationResult result)
            throws SchemaException, ConfigurationException {
        return new ChangeModeApplication<>(elementContext, delta, task)
                .execute(result);
    }

    private void executeRealModification(Class<E> objectClass, OperationResult result)
            throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException, CommunicationException,
            ConfigurationException, SecurityViolationException, ExpressionEvaluationException, PolicyViolationException,
            ConflictDetectedException {
        if (TaskType.class.isAssignableFrom(objectClass)) {
            b.taskManager.modifyTask(
                    deltaForExecution.getOid(), deltaForExecution.getModifications(), result);
        } else if (NodeType.class.isAssignableFrom(objectClass)) {
            b.cacheRepositoryService.modifyObject(
                    NodeType.class, deltaForExecution.getOid(), deltaForExecution.getModifications(), result);
        } else if (ObjectTypes.isClassManagedByProvisioning(objectClass)) {
            String oid = modifyProvisioningObject(result);
            if (!oid.equals(deltaForExecution.getOid())) {
                deltaForExecution.setOid(oid);
                delta.setOid(oid);
                LensUtil.setContextOid(context, elementContext, oid);
            }
        } else {
            FocusConstraintsChecker.clearCacheForDelta(deltaForExecution.getModifications());
            ModificationPrecondition<E> precondition = createRepoModificationPrecondition();
            try {
                b.cacheRepositoryService.modifyObject(
                        objectClass, deltaForExecution.getOid(), deltaForExecution.getModifications(),
                        precondition, null, result);
            } catch (PreconditionViolationException e) {
                throw new ConflictDetectedException(e);
            }
        }
    }

    private String modifyProvisioningObject(OperationResult result) throws ObjectNotFoundException, CommunicationException,
            SchemaException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException,
            ObjectAlreadyExistsException, PolicyViolationException {

        Class<E> objectClass = deltaForExecution.getObjectTypeClass();
        String oid = deltaForExecution.getOid();
        PrismObject<E> objectToModify = null;
        try {
            Collection<SelectorOptions<GetOperationOptions>> getOptions = b.schemaService.getOperationOptionsBuilder()
                    .readOnly()
                    .noFetch()
                    .futurePointInTime()
                    .build();
            objectToModify = b.provisioningService.getObject(objectClass, oid, getOptions, task, result);
        } catch (ObjectNotFoundException e) {
            // We do not want the operation to fail here. The object might have been re-created on the resource
            // or discovery might re-create it. So simply ignore this error and give provisioning a chance to fail properly.
            // TODO This is maybe a false hope. In fact, if OID is not in repo, the modifyObject call fails immediately.
            result.muteLastSubresultError();
            LOGGER.warn("Repository object {}: {} is gone. But trying to modify resource object anyway", objectClass, oid);
        }
        OperationProvisioningScriptsType scripts;
        if (ShadowType.class.isAssignableFrom(objectClass)) {
            scripts = prepareScripts(objectToModify, ProvisioningOperationTypeType.MODIFY, result);
        } else {
            scripts = null;
        }
        ModelImplUtils.setRequestee(task, context);
        try {
            ProvisioningOperationOptions options = getProvisioningOptions();
            ProvisioningOperationContext ctx = context.createProvisioningOperationContext();
            String updatedOid =
                    b.provisioningService.modifyObject(
                            objectClass, oid, deltaForExecution.getModifications(), scripts, options, ctx, task, result);
            determineLivenessFromObject(objectToModify);
            return updatedOid;
        } catch (ObjectNotFoundException e) {
            // rough attempt at guessing if the exception is related to the shadow (and not e.g. to the resource)
            if (e.getOid() == null || e.getOid().equals(oid)) {
                shadowLivenessState = ShadowLivenessState.DELETED;
            }
            throw e;
        } finally {
            ModelImplUtils.clearRequestee(task);
        }
    }

    private void determineLivenessFromObject(PrismObject<E> objectToModify) throws SchemaException {
        if (!ShadowType.class.equals(delta.getObjectTypeClass())) {
            return;
        }
        if (objectToModify != null) {
            // Although we can expect that modifications are not connected with the 'dead' property, let us be precise.
            delta.applyTo(objectToModify);
            PrismObject<ShadowType> shadowToModify = cast(objectToModify, ShadowType.class);

            // TODO what about shadows with pending deletion?
            shadowLivenessState = ShadowLivenessState.forShadow(shadowToModify);
            LOGGER.trace("Determined liveness of {} (before modification) to be {} (dead: {})",
                    shadowToModify, shadowLivenessState, ShadowUtil.isDead(shadowToModify));
        } else {
            // If this is so, we are probably not here. But just for completeness.
            shadowLivenessState = ShadowLivenessState.DELETED;
        }
    }

    @Nullable
    private ModificationPrecondition<E> createRepoModificationPrecondition() {
        if (!b.clockworkConflictResolver.shouldCreatePrecondition(context, conflictResolution)) {
            return null;
        }
        String readVersion = elementContext.getObjectReadVersion();
        if (readVersion == null) {
            LOGGER.warn("Requested careful modification of {}, but there is no read version", elementContext.getHumanReadableName());
            return null;
        }
        LOGGER.trace("Modification with precondition, readVersion={}", readVersion);
        return new VersionPrecondition<>(readVersion);
    }

    private <T extends ObjectType> boolean shouldApplyModifyMetadata(Class<T> objectTypeClass) {
        if (!ShadowType.class.equals(objectTypeClass)) {
            return true;
        }
        MetadataRecordingStrategyType recordingStrategy = context.getShadowMetadataRecordingStrategy();
        return recordingStrategy == null || !Boolean.TRUE.equals(recordingStrategy.isSkipOnModify());
    }
    //endregion

    //region Deletion
    private void executeDeletion(OperationResult result)
            throws ObjectNotFoundException, ObjectAlreadyExistsException, SchemaException, CommunicationException,
            ConfigurationException, SecurityViolationException, PolicyViolationException, ExpressionEvaluationException {

        String oid = delta.getOid();
        Class<E> objectTypeClass = delta.getObjectTypeClass();

        PrismObject<E> objectOld = elementContext.getObjectOld();
        try {
            b.securityEnforcer.authorize(
                    ModelAuthorizationAction.DELETE.getUrl(),
                    AuthorizationPhaseType.EXECUTION,
                    AuthorizationParameters.Builder.buildObjectDelete(objectOld),
                    enforcerOptionsWithLensOwnerResolver(result),
                    task,
                    result);

            if (task.isExecutionFullyPersistent()) {
                executeRealDeletion(objectTypeClass, oid, result);
            } else {
                elementContext.simulateDeltaExecution(delta);
                objectAfterModification = null;
            }
            deleted = true;
            task.recordObjectActionExecuted(objectOld, objectTypeClass, oid, ChangeType.DELETE, context.getChannel(), null);
        } catch (Throwable t) {
            task.recordObjectActionExecuted(objectOld, objectTypeClass, oid, ChangeType.DELETE, context.getChannel(), t);

            if (ShadowType.class.isAssignableFrom(objectTypeClass)) {
                handleProvisioningError(resource, t, result);
                objectAfterModification = elementContext.getObjectCurrent(); // TODO ok?
            }

            throw t;
        }
    }

    private void executeRealDeletion(Class<E> objectTypeClass, String oid, OperationResult result)
            throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException, SecurityViolationException,
            CommunicationException, ConfigurationException, ExpressionEvaluationException, PolicyViolationException {
        if (TaskType.class.isAssignableFrom(objectTypeClass)) {
            b.taskManager.deleteTask(oid, result);
        } else if (NodeType.class.isAssignableFrom(objectTypeClass)) {
            b.taskManager.deleteNode(oid, result);
        } else if (b.caseManager != null && CaseType.class.isAssignableFrom(objectTypeClass)) {
            b.caseManager.deleteCase(oid, task, result);
        } else if (ObjectTypes.isClassManagedByProvisioning(objectTypeClass)) {
            try {
                objectAfterModification = deleteProvisioningObject(objectTypeClass, oid, result);
                if (ShadowType.class.equals(objectTypeClass)) {
                    PrismObject<ShadowType> shadowAfterModification = cast(objectAfterModification, ShadowType.class);
                    // TODO what about shadows with pending deletion?
                    shadowLivenessState = ShadowLivenessState.forShadow(shadowAfterModification);
                    LOGGER.trace("Determined liveness of {} (after modification) to be {} (dead: {})",
                            shadowAfterModification, shadowLivenessState,
                            shadowAfterModification != null ? ShadowUtil.isDead(shadowAfterModification) : "(null)");
                }
            } catch (ObjectNotFoundException e) {
                // Object that we wanted to delete is already gone. This can happen in some race conditions.
                // As the resulting state is the same as we wanted it to be we will not complain and we will go on.
                LOGGER.trace("Attempt to delete object {} ({}) that is already gone", oid, objectTypeClass);
                result.muteLastSubresultError();
                objectAfterModification = null;
                shadowLivenessState = ShadowLivenessState.DELETED;
            }
            if (objectAfterModification == null && elementContext instanceof LensProjectionContext) {
                ((LensProjectionContext) elementContext).setShadowExistsInRepo(false);
            }
        } else {
            try {
                b.cacheRepositoryService.deleteObject(objectTypeClass, oid, result);
            } catch (ObjectNotFoundException e) {
                // Object that we wanted to delete is already gone. This can happen in some race conditions.
                // As the resulting state is the same as we wanted it to be we will not complain and we will go on.
                LOGGER.trace("Attempt to delete object {} ({}) that is already gone", oid, objectTypeClass);
                result.muteLastSubresultError();
            }
            objectAfterModification = null;
        }
    }

    private PrismObject<E> deleteProvisioningObject(Class<E> type, String oid, OperationResult result)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException, PolicyViolationException {

        ProvisioningOperationOptions options = getProvisioningOptions();
        ProvisioningOperationContext ctx = context.createProvisioningOperationContext();

        PrismObject<E> objectToDelete = null;
        try {
            Collection<SelectorOptions<GetOperationOptions>> getOptions = b.schemaService.getOperationOptionsBuilder()
                    .readOnly()
                    .noFetch()
                    .futurePointInTime()
                    .build();
            objectToDelete = b.provisioningService.getObject(type, oid, getOptions, task, result);
        } catch (ObjectNotFoundException ex) {
            // this is almost OK, mute the error and try to delete account (it will fail if something is wrong)
            result.muteLastSubresultError();
        }
        OperationProvisioningScriptsType scripts;
        if (ShadowType.class.isAssignableFrom(type)) {
            scripts = prepareScripts(objectToDelete, ProvisioningOperationTypeType.DELETE, result);
        } else {
            scripts = null;
        }
        ModelImplUtils.setRequestee(task, context);
        try {
            return b.provisioningService.deleteObject(type, oid, options, scripts, ctx, task, result);
        } finally {
            ModelImplUtils.clearRequestee(task);
        }
    }
    //endregion

    //region Provisioning options
    private ProvisioningOperationOptions getProvisioningOptions() throws SecurityViolationException {
        ModelExecuteOptions modelOptions = context.getOptions();
        ProvisioningOperationOptions provisioningOptions = copyFromModelOptions(modelOptions);

        E existingObject = asObjectable(elementContext.getObjectCurrent());
        if (existingObject instanceof ShadowType existingShadow) {
            if (isExecuteAsSelf(existingShadow)) {
                LOGGER.trace("Setting 'execute as self' provisioning option for {}", existingShadow);
                provisioningOptions.setRunAsAccountOid(existingShadow.getOid());
            }
        }

        if (context.getChannel() != null) {

            if (context.getChannel().equals(QNameUtil.qNameToUri(SchemaConstants.CHANNEL_RECON))) {
                // TODO: this is probably wrong. We should not have special case
                //  for recon channel! This should be handled by the provisioning task
                //  setting the right options there.
                provisioningOptions.setCompletePostponed(false);
            }

            if (context.getChannel().equals(SchemaConstants.CHANNEL_DISCOVERY_URI)) {
                // We want to avoid endless loops in error handling.
                provisioningOptions.setDoNotDiscovery(true);
            }
        }

        return provisioningOptions;
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

    /**
     * This is a bit of black magic. We only want to execute as self if there a user is changing its own password
     * and we also have old password value.
     *
     * TODO Later, this should be improved. Maybe we need special model operation option for this? Or maybe it should be somehow
     *  automatically detected based on resource capabilities? We do not know yet. Therefore let's do the simplest possible
     *  thing. Otherwise we might do something that we will later regret.
     *
     * See also {@link ProjectionMappingSetEvaluator#applyEstimatedOldValueInReplaceCaseIfCredentials
     * (MappingEvaluatorParams, ItemDelta, PrismValueDeltaSetTriple)}.
     */
    private boolean isExecuteAsSelf(ShadowType existingShadow) throws SecurityViolationException {
        if (existingShadow == null) {
            return false;
        }

        if (!SchemaConstants.CHANNEL_SELF_SERVICE_URI.equals(context.getChannel())) {
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

        LensFocusContext<O> focusContext = context.getFocusContext();
        if (focusContext == null) {
            return false;
        }
        if (!focusContext.represents(UserType.class)) {
            return false;
        }

        MidPointPrincipal principal = b.securityContextManager.getPrincipal();
        if (principal == null) {
            return false;
        }
        FocusType loggedInUser = principal.getFocus();
        return loggedInUser.getOid().equals(focusContext.getOid());
    }
    //endregion

    //region Provisioning scripts
    /**
     * TODO clarify the role of `object` parameter and why it is used only as a second choice (after ctx.objectAny).
     */
    private OperationProvisioningScriptsType prepareScripts(
            PrismObject<E> object, ProvisioningOperationTypeType operation, OperationResult result)
            throws ObjectNotFoundException, SchemaException, CommunicationException,
            ConfigurationException, SecurityViolationException, ExpressionEvaluationException {

        if (resource == null) {
            LOGGER.warn("Resource does not exist. Skipping processing scripts.");
            return null;
        }
        OperationProvisioningScriptsType resourceScripts = resource.getScripts();

        LensProjectionContext projCtx = (LensProjectionContext) elementContext;
        PrismObject<ShadowType> shadow = getShadow(projCtx, object);
        PrismObject<O> focus = getFocus();

        VariablesMap variables = ModelImplUtils.getDefaultVariablesMap(
                focus, shadow, resource.asPrismObject(), context.getSystemConfiguration(), elementContext);
        // Having delta in provisioning scripts may be very useful. E.g. the script can optimize execution of expensive operations.
        variables.put(ExpressionConstants.VAR_DELTA, projCtx.getCurrentDelta(), ObjectDelta.class);
        ExpressionProfile expressionProfile = MiscSchemaUtil.getExpressionProfile();

        ExpressionEnvironmentThreadLocalHolder.pushExpressionEnvironment(
                new ModelExpressionEnvironment<>(context, projCtx, task, result));
        try {
            ScriptExecutor<O> scriptExecutor = new ScriptExecutor<>(context, projCtx, task, b);
            ProjectionContextKey key = projCtx.getKey();
            return scriptExecutor.prepareScripts(
                    resourceScripts, key, operation, null, variables, expressionProfile, result);
        } finally {
            ExpressionEnvironmentThreadLocalHolder.popExpressionEnvironment();
        }
    }

    @Nullable
    private PrismObject<O> getFocus() {
        PrismObject<O> focus;
        if (context.getFocusContext() != null) {
            focus = context.getFocusContext().getObjectAny();
        } else {
            focus = null;
        }
        return focus;
    }

    private PrismObject<ShadowType> getShadow(LensProjectionContext projectionCtx, PrismObject<E> fromProvisioning) {
        PrismObject<ShadowType> fromContext = projectionCtx.getObjectAny();
        if (fromContext != null) {
            return fromContext; // TODO why not cloning here?
        } else if (fromProvisioning != null) {
            return cast(fromProvisioning, ShadowType.class).clone(); // TODO and why cloning here?
        } else {
            return null;
        }
    }
    //endregion

    //region Misc
    private SecurityEnforcer.Options enforcerOptionsWithLensOwnerResolver(OperationResult result) {
        return SecurityEnforcer.Options.create()
                .withCustomOwnerResolver(
                        new LensOwnerResolver<>(context, b.modelObjectResolver, task, result));
    }

    private void handleProvisioningError(ResourceType resource, Throwable t, OperationResult result)
            throws ObjectNotFoundException, ConfigurationException, SecurityViolationException, PolicyViolationException,
            ExpressionEvaluationException, ObjectAlreadyExistsException, CommunicationException,
            SchemaException {
        ErrorSelectorType errorSelectorType = ResourceTypeUtil.getConnectorErrorCriticality(resource);
        CriticalityType criticality = ExceptionUtil.getCriticality(errorSelectorType, t, CriticalityType.FATAL);
        RepoCommonUtils.processErrorCriticality(resource, criticality, t, result);
        if (criticality == CriticalityType.IGNORE) {
            result.muteLastSubresultError();
        }
    }

    PrismObject<E> getObjectAfterModification() {
        return objectAfterModification;
    }

    ShadowLivenessState getShadowLivenessState() {
        return shadowLivenessState;
    }

    public boolean isDeleted() {
        return deleted;
    }
    //endregion
}
