/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.executor;

import static com.evolveum.midpoint.model.api.ProgressInformation.ActivityType.FOCUS_OPERATION;
import static com.evolveum.midpoint.model.api.ProgressInformation.StateType.ENTERING;
import static com.evolveum.midpoint.model.impl.lens.ChangeExecutor.OPERATION_EXECUTE_FOCUS;
import static com.evolveum.midpoint.prism.PrismContainerValue.asContainerables;
import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

import java.util.*;

import org.apache.commons.lang3.BooleanUtils;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.ProgressInformation;
import com.evolveum.midpoint.model.impl.lens.ChangeExecutor;
import com.evolveum.midpoint.model.impl.lens.ConflictDetectedException;
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.model.impl.lens.assignments.AssignmentSpec;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.delta.*;
import com.evolveum.midpoint.prism.equivalence.EquivalenceStrategy;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Executes changes in the focus context.
 *
 * 1. Treats pending policy state modifications
 * 2. Applies archetype policy (item constraints) to object being added
 * 3. Treats credentials deltas
 * 4. Updates last provisioning timestamp
 * 5. Manages conflict resolution
 * 6. Reports progress
 */
public class FocusChangeExecution<O extends ObjectType> extends ElementChangeExecution<O, O> {

    /** For the time being we keep the parent logger name. */
    private static final Trace LOGGER = TraceManager.getTrace(ChangeExecutor.class);

    @NotNull private final LensFocusContext<O> focusContext;

    /**
     * Delta to be executed. It is gradually updated.
     */
    private ObjectDelta<O> focusDelta;

    /** Just to detect leaking of the listeners. */
    private static final int MAX_LISTENERS_PER_THREAD = 100;

    private static final ThreadLocal<Set<ChangeExecutionListener>> CHANGE_EXECUTION_LISTENERS_TL =
            ThreadLocal.withInitial(HashSet::new);

    public FocusChangeExecution(@NotNull LensFocusContext<O> focusContext, @NotNull Task task) {
        super(focusContext, task);
        this.focusContext = focusContext;
    }

    public void execute(OperationResult parentResult) throws SchemaException,
            ObjectAlreadyExistsException, CommunicationException, ObjectNotFoundException, ConfigurationException,
            SecurityViolationException, PolicyViolationException, ExpressionEvaluationException, ConflictDetectedException {

        focusDelta = applyPendingPolicyStateModifications();

        if (ObjectDelta.isEmpty(focusDelta) && !context.hasProjectionChange()) {
            LOGGER.trace("Skipping focus change execute, because focus delta is empty and there are no projections changes");
            return;
        }

        if (focusDelta == null) {
            focusDelta = focusContext.getObjectAny().createModifyDelta();
        }

        if (focusDelta.isAdd()) {
            applyArchetypePolicyToAddedObject();
        }

        OperationResult result = parentResult.createSubresult(
                OPERATION_EXECUTE_FOCUS + "." + focusContext.getObjectTypeClass().getSimpleName());

        try {
            context.reportProgress(new ProgressInformation(FOCUS_OPERATION, ENTERING));

            removeOrHashCredentialsDeltas();

            executeDeltaWithConflictResolution(result);

        } catch (ConflictDetectedException e) {
            LOGGER.debug("Modification precondition failed for {}: {}", focusContext.getHumanReadableName(), e.getMessage());
            // TODO: fatal error if the conflict resolution is "error" (later)
            result.recordHandledError(e);
            throw e;
        } catch (ObjectAlreadyExistsException e) {
            result.computeStatus();
            if (!result.isSuccess() && !result.isHandledError()) {
                result.recordFatalError(e);
            }
            throw e;
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
            context.reportProgress(new ProgressInformation(FOCUS_OPERATION, result));
            notifyChangeExecutionListeners();
        }
    }

    private ObjectDelta<O> applyPendingPolicyStateModifications() throws SchemaException {
        applyPendingObjectPolicyStateModifications();
        applyPendingAssignmentPolicyStateModificationsSwallowable();
        ObjectDelta<O> resultingDelta = applyPendingAssignmentPolicyStateModificationsNotSwallowable();

        focusContext.clearPendingPolicyStateModifications();
        return resultingDelta;
    }

    private void applyPendingObjectPolicyStateModifications() throws SchemaException {
        focusContext.swallowToSecondaryDelta(focusContext.getPendingObjectPolicyStateModifications());
    }

    private void applyPendingAssignmentPolicyStateModificationsSwallowable() throws SchemaException {
        for (Iterator<Map.Entry<AssignmentSpec, List<ItemDelta<?, ?>>>> iterator = focusContext
                .getPendingAssignmentPolicyStateModifications().entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry<AssignmentSpec, List<ItemDelta<?, ?>>> entry = iterator.next();
            PlusMinusZero mode = entry.getKey().mode;
            AssignmentType assignmentToFind = entry.getKey().assignment;
            List<ItemDelta<?, ?>> modifications = entry.getValue();
            LOGGER.trace("Applying policy state modifications for {} ({}): {} mods", assignmentToFind, mode, modifications.size());
            if (modifications.isEmpty()) {
                iterator.remove();
                continue;
            }
            if (mode == PlusMinusZero.MINUS) {
                LOGGER.trace("This assignment is being thrown out anyway, so let's ignore it. Mods:\n{}",
                        DebugUtil.debugDumpLazily(modifications));
                iterator.remove();
                continue;
            }
            if (mode == PlusMinusZero.ZERO) {
                if (assignmentToFind.getId() == null) {
                    throw new IllegalStateException("Existing assignment with null id: " + assignmentToFind);
                }
                LOGGER.trace("Swallowing mods:\n{}", DebugUtil.debugDumpLazily(modifications));
                focusContext.swallowToSecondaryDelta(modifications);
                iterator.remove();
            } else {
                assert mode == PlusMinusZero.PLUS;
                LOGGER.trace("Cannot apply this one, postponing.");
                // Cannot apply this one, so will not remove it
            }
        }
    }

    /**
     * The following modifications cannot be (generally) applied by simply swallowing them to secondary deltas.
     * They require modifying primary ADD delta or modifying values in specific assignment-related item deltas.
     */
    private ObjectDelta<O> applyPendingAssignmentPolicyStateModificationsNotSwallowable()
            throws SchemaException {
        ObjectDelta<O> focusDelta = focusContext.getCurrentDelta();
        Map<AssignmentSpec, List<ItemDelta<?, ?>>> pendingModifications = focusContext.getPendingAssignmentPolicyStateModifications();
        for (Map.Entry<AssignmentSpec, List<ItemDelta<?, ?>>> entry : pendingModifications.entrySet()) {
            PlusMinusZero mode = entry.getKey().mode;
            AssignmentType assignmentToFind = entry.getKey().assignment;
            List<ItemDelta<?, ?>> modifications = entry.getValue();
            LOGGER.trace("Applying postponed policy state modifications for {} ({}):\n{}", assignmentToFind, mode,
                    DebugUtil.debugDumpLazily(modifications));

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
                    swallowIntoValues(asContainerables(assignmentDelta.getValuesToReplace()), assignmentToFind, modifications);
                } else if (assignmentDelta.isAdd()) {
                    swallowIntoValues(asContainerables(assignmentDelta.getValuesToAdd()), assignmentToFind, modifications);
                } else {
                    throw new IllegalStateException(
                            "We have 'plus' assignment to modify but there are no values to add or replace in assignment delta. "
                                    + "Assignment=" + assignmentToFind + ", objectDelta=" + focusDelta);
                }
            }
        }
        return focusDelta;
    }

    private void swallowIntoValues(Collection<AssignmentType> assignments, AssignmentType assignmentToFind, List<ItemDelta<?, ?>> modifications)
            throws SchemaException {
        for (AssignmentType assignment : assignments) {
            PrismContainerValue<?> pcv = assignment.asPrismContainerValue();
            PrismContainerValue<?> pcvToFind = assignmentToFind.asPrismContainerValue();
            if (pcv.representsSameValue(pcvToFind, EquivalenceStrategy.REAL_VALUE_CONSIDER_DIFFERENT_IDS, false)
                    || pcv.equals(pcvToFind, EquivalenceStrategy.REAL_VALUE_CONSIDER_DIFFERENT_IDS)) {

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
        throw new IllegalStateException("We have 'plus' assignment to modify but it couldn't be found in assignment delta. "
                + "Assignment=" + assignmentToFind + ", new assignments=" + assignments);
    }

    private void removeOrHashCredentialsDeltas() throws SchemaException {
        try {
            focusDelta = b.credentialsStorageManager.transformFocusExecutionDelta(
                    context.getFocusContext().getCredentialsPolicy(),
                    focusDelta);
        } catch (EncryptionException e) {
            throw new SystemException(e.getMessage(), e);
        }
    }

    private void executeDeltaWithConflictResolution(OperationResult result) throws SchemaException,
            CommunicationException, ObjectAlreadyExistsException, ExpressionEvaluationException,
            PolicyViolationException, SecurityViolationException, ConfigurationException, ObjectNotFoundException,
            ConflictDetectedException {
        ConflictResolutionType conflictResolution = ModelExecuteOptions.getFocusConflictResolution(context.getOptions());
        DeltaExecution<O, O> deltaExecution =
                new DeltaExecution<>(focusContext, focusDelta, conflictResolution, task, changeExecutionResult);
        deltaExecution.execute(result);
        if (focusDelta.isAdd() && focusDelta.getOid() != null) {
            b.clockworkConflictResolver.createConflictWatcherAfterFocusAddition(context, focusDelta.getOid(),
                    focusDelta.getObjectToAdd().getVersion());
        }
        if (deltaExecution.isDeleted()) {
            focusContext.setDeleted();
        }
    }

    private void applyArchetypePolicyToAddedObject() {
        ArchetypePolicyType archetypePolicy = focusContext.getArchetypePolicy();
        if (archetypePolicy == null) {
            return;
        }
        PrismObject<O> objectNew = focusContext.getObjectNew();
        if (objectNew.getOid() == null) {
            for (ItemConstraintType itemConstraint : archetypePolicy.getItemConstraint()) {
                processItemConstraint(objectNew, itemConstraint);
            }
        }
    }

    private void processItemConstraint(PrismObject<O> objectNew, ItemConstraintType itemConstraint) {
        if (BooleanUtils.isTrue(itemConstraint.isOidBound())) {
            ItemPath itemPath = itemConstraint.getPath().getItemPath();
            PrismProperty<Object> prop = objectNew.findProperty(itemPath);
            focusContext.setOid(String.valueOf(prop.getRealValue()));
        }
    }

    private void notifyChangeExecutionListeners() {
        String oid = focusContext.getOid(); // Even for ADD operations the new OID should be here.
        if (oid == null) {
            return; // Something must have gone wrong
        }

        for (ChangeExecutionListener listener : CHANGE_EXECUTION_LISTENERS_TL.get()) {
            // We don't expect any exceptions to be thrown
            listener.onFocusChange(oid);
        }
    }

    /** Must be accompanied by respective unregister call! */
    public static void registerChangeExecutionListener(@NotNull ChangeExecutionListener listener) {
        Set<ChangeExecutionListener> listeners = CHANGE_EXECUTION_LISTENERS_TL.get();
        listeners.add(listener);
        stateCheck(listeners.size() <= MAX_LISTENERS_PER_THREAD,
                "Leaking change execution listeners in %s: %s", Thread.currentThread(), listeners);
    }

    public static void unregisterChangeExecutionListener(@NotNull ChangeExecutionListener listener) {
        CHANGE_EXECUTION_LISTENERS_TL.get().remove(listener);
    }

    /**
     * Receives notifications when focus object is modified (or added, or deleted).
     * Should be fast and should throw no exceptions.
     */
    @Experimental
    public interface ChangeExecutionListener {
        void onFocusChange(@NotNull String oid);
    }
}
