/*
 * Copyright (c) 2017-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens.projector.focus;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.context.SynchronizationPolicyDecision;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.model.impl.lens.LensUtil;
import com.evolveum.midpoint.model.impl.lens.assignments.AssignmentEvaluator;
import com.evolveum.midpoint.model.impl.lens.assignments.EvaluatedAssignmentImpl;
import com.evolveum.midpoint.model.impl.lens.projector.SmartAssignmentCollection;
import com.evolveum.midpoint.model.impl.lens.projector.SmartAssignmentElement;
import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.*;
import com.evolveum.midpoint.prism.delta.builder.S_ValuesEntry;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.ItemDeltaItem;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.FocusTypeUtil;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LifecycleStateModelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

import org.jetbrains.annotations.NotNull;

/**
 * Evaluates all assignments and sorts them to triple: added, removed and "kept" assignments.
 *
 * @author semancik
 */
public class AssignmentTripleEvaluator<AH extends AssignmentHolderType> {

    private static final Trace LOGGER = TraceManager.getTrace(AssignmentTripleEvaluator.class);

    private static final String OP_EVALUATE_ASSIGNMENT = AssignmentTripleEvaluator.class.getName()+".evaluateAssignment";

    private final LensContext<AH> context;
    private final LensFocusContext<AH> focusContext;

    /**
     * TODO explain this
     */
    private final AssignmentHolderType source;

    private final AssignmentEvaluator<AH> assignmentEvaluator;
    private final ModelBeans beans;
    private final XMLGregorianCalendar now;
    private final Task task;
    private final OperationResult result;

    private final LifecycleStateModelType focusStateModel;

    private final DeltaSetTriple<EvaluatedAssignmentImpl<AH>> evaluatedAssignmentTriple;

    private ContainerDelta<AssignmentType> currentAssignmentDelta;

    private AssignmentTripleEvaluator(Builder<AH> builder) throws SchemaException {
        context = builder.context;
        focusContext = context.getFocusContext();
        if (focusContext != null) {
            focusStateModel = focusContext.getLifecycleModel();
        } else {
            focusStateModel = null;
        }

        source = builder.source;
        assignmentEvaluator = builder.assignmentEvaluator;
        beans = builder.beans;
        now = builder.now;
        task = builder.task;
        result = builder.result;

        evaluatedAssignmentTriple = beans.prismContext.deltaFactory().createDeltaSetTriple();
        computeCurrentAssignmentDelta();
    }

    public void reset(boolean alsoMemberOfInvocations) throws SchemaException {
        assignmentEvaluator.reset(alsoMemberOfInvocations);
        evaluatedAssignmentTriple.clear();
        computeCurrentAssignmentDelta();
    }

    DeltaSetTriple<EvaluatedAssignmentImpl<AH>> processAllAssignments() throws ObjectNotFoundException, SchemaException,
            ExpressionEvaluationException, PolicyViolationException, SecurityViolationException, ConfigurationException,
            CommunicationException {

        LOGGER.trace("Assignment current delta (i.e. from current to new object):\n{}", currentAssignmentDelta.debugDumpLazily());

        Collection<AssignmentType> virtualAssignments = getVirtualAssignments();

        SmartAssignmentCollection<AH> assignmentCollection = new SmartAssignmentCollection<>();
        assignmentCollection.collectAndFreeze(focusContext.getObjectCurrent(), focusContext.getObjectOld(), currentAssignmentDelta,
                virtualAssignments, beans.prismContext);

        LOGGER.trace("Assignment collection:\n{}", assignmentCollection.debugDumpLazily(1));

        // Iterate over all the assignments. I mean really all. This is a union of the existing and changed assignments
        // therefore it contains all three types of assignments (plus, minus and zero). As it is an union each assignment
        // will be processed only once. Inside the loop we determine whether it was added, deleted or remains unchanged.
        // This is a first step of the processing. It takes all the account constructions regardless of the resource and
        // account type (intent). Therefore several constructions for the same resource and intent may appear in the resulting
        // sets. This is not good as we want only a single account for each resource/intent combination. But that will be
        // sorted out later.
        for (SmartAssignmentElement assignmentElement : assignmentCollection) {
            processAssignment(assignmentElement);
        }

        return evaluatedAssignmentTriple;
    }

    @NotNull
    private Collection<AssignmentType> getVirtualAssignments() throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        Collection<AssignmentType> forcedAssignments = LensUtil.getForcedAssignments(focusContext.getLifecycleModel(),
                getNewObjectLifecycleState(focusContext), beans.modelObjectResolver,
                beans.prismContext, task, result);
        LOGGER.trace("Forced assignments: {}", forcedAssignments);

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Task for process: {}", task.getUpdatedOrClonedTaskObject().debugDump());
        }
        Collection<Task> allTasksToRoot = task.getPathToRootTask(result);
        Collection<AssignmentType> taskAssignments = allTasksToRoot.stream()
                .filter(Task::hasAssignments)
                .map(this::createTaskAssignment)
                .collect(Collectors.toList());
        LOGGER.trace("Task assignment: {}", taskAssignments);

        List<AssignmentType> virtualAssignments = new ArrayList<>(forcedAssignments);
        virtualAssignments.addAll(taskAssignments);
        return virtualAssignments;
    }

    private AssignmentType createTaskAssignment(Task fromTask) {
        AssignmentType taskAssignment = new AssignmentType(beans.prismContext);
        ObjectReferenceType targetRef = new ObjectReferenceType();
        targetRef.asReferenceValue().setObject(fromTask.getUpdatedOrClonedTaskObject());
        taskAssignment.setTargetRef(targetRef);
        return taskAssignment;
    }

    private String getNewObjectLifecycleState(LensFocusContext<AH> focusContext) {
        PrismObject<AH> focusNew = focusContext.getObjectNew();
        AH focusTypeNew = focusNew.asObjectable();
        return focusTypeNew.getLifecycleState();
    }

    private void processAssignment(SmartAssignmentElement assignmentElement) throws SchemaException, ExpressionEvaluationException,
            PolicyViolationException, SecurityViolationException, ConfigurationException, CommunicationException {

        PrismContainerValue<AssignmentType> assignmentCVal = assignmentElement.getAssignmentCVal();
        PrismContainerValue<AssignmentType> assignmentCValOld = assignmentCVal;
        PrismContainerValue<AssignmentType> assignmentCValNew = assignmentCVal; // refined later

        // This really means whether the WHOLE assignment was changed (e.g. added/deleted/replaced). It tells nothing
        // about "micro-changes" inside assignment, these will be processed later.
        boolean isAssignmentChanged = assignmentElement.isCurrent() != assignmentElement.isNew();
        // TODO what about assignments that are present in old or current objects, and also changed?
        // TODO They seem to have "old"/"current" flag, not "changed" one. Is that OK?
        boolean forceRecon = false;
        String assignmentPlacementDesc;

        Collection<? extends ItemDelta<?,?>> subItemDeltas = null;

        if (isAssignmentChanged) {
            // Whole assignment added or deleted
            assignmentPlacementDesc = "delta for "+source;
        } else {
            assignmentPlacementDesc = source.toString();
            Collection<? extends ItemDelta<?,?>> assignmentItemDeltas = getAssignmentItemDeltas(focusContext, assignmentCVal.getId());
            if (assignmentItemDeltas != null && !assignmentItemDeltas.isEmpty()) {
                // Small changes inside assignment, but otherwise the assignment stays as it is (not added or deleted)
                subItemDeltas = assignmentItemDeltas;

                // The subItemDeltas above will handle some changes. But not other.
                // E.g. a replace of the whole construction will not be handled properly.
                // Therefore we force recon to sort it out.
                forceRecon = true;

                isAssignmentChanged = true;
                PrismContainer<AssignmentType> assContNew = focusContext.getObjectNew().findContainer(AssignmentHolderType.F_ASSIGNMENT);
                assignmentCValNew = assContNew.getValue(assignmentCVal.getId());
            }
        }

        // The following code is using collectToAccountMap() to collect the account constructions to one of the three "delta"
        // sets (zero, plus, minus). It is handling several situations that needs to be handled specially.
        // It is also collecting assignments to evaluatedAssignmentTriple.

        if (focusContext.isDelete()) {

            // USER DELETE
            // If focus (user) is being deleted that all the assignments are to be gone. Including those that
            // were not changed explicitly.
            LOGGER.trace("Processing focus delete for: {}", SchemaDebugUtil.prettyPrintLazily(assignmentCVal));
            EvaluatedAssignmentImpl<AH> evaluatedAssignment = evaluateAssignment(createAssignmentIdiDelete(assignmentCVal), PlusMinusZero.MINUS, false, assignmentPlacementDesc, assignmentElement);
            if (evaluatedAssignment == null) {
                return;
            }
            evaluatedAssignment.setWasValid(evaluatedAssignment.isValid());
            collectToMinus(evaluatedAssignmentTriple, evaluatedAssignment, forceRecon);

        } else {
            if (currentAssignmentDelta.isReplace()) {

                LOGGER.trace("Processing replace of all assignments for: {}", SchemaDebugUtil.prettyPrintLazily(assignmentCVal));
                // ASSIGNMENT REPLACE
                // Handling assignment replace delta. This needs to be handled specially as all the "old"
                // assignments should be considered deleted - except those that are part of the new value set
                // (remain after replace). As account delete and add are costly operations (and potentially dangerous)
                // we optimize here are consider the assignments that were there before replace and still are there
                // after it as unchanged.
                //
                // TODO consider using augmented origin here (isOld / isCurrent / isNew values) -- MID-6404
                boolean hadValue = assignmentElement.isCurrent();
                boolean willHaveValue = currentAssignmentDelta.isValueToReplace(assignmentCVal, true);
                if (hadValue && willHaveValue) {
                    // No change
                    EvaluatedAssignmentImpl<AH> evaluatedAssignment = evaluateAssignment(createAssignmentIdiNoChange(assignmentCVal), PlusMinusZero.ZERO, false, assignmentPlacementDesc, assignmentElement);
                    if (evaluatedAssignment == null) {
                        return;
                    }
                    evaluatedAssignment.setWasValid(evaluatedAssignment.isValid());
                    collectToZero(evaluatedAssignmentTriple, evaluatedAssignment, forceRecon);
                } else if (willHaveValue) {
                    // add
                    EvaluatedAssignmentImpl<AH> evaluatedAssignment = evaluateAssignment(createAssignmentIdiAdd(assignmentCVal), PlusMinusZero.PLUS, false, assignmentPlacementDesc, assignmentElement);
                    if (evaluatedAssignment == null) {
                        return;
                    }
                    evaluatedAssignment.setWasValid(false);
                    collectToPlus(evaluatedAssignmentTriple, evaluatedAssignment, forceRecon);
                } else if (hadValue) {
                    // delete
                    EvaluatedAssignmentImpl<AH> evaluatedAssignment = evaluateAssignment(createAssignmentIdiDelete(assignmentCVal), PlusMinusZero.MINUS, true, assignmentPlacementDesc, assignmentElement);
                    if (evaluatedAssignment == null) {
                        return;
                    }
                    evaluatedAssignment.setWasValid(evaluatedAssignment.isValid());
                    collectToMinus(evaluatedAssignmentTriple, evaluatedAssignment, forceRecon);
                } else if (assignmentElement.isOld()) {
                    // This is OK, safe to skip. This is just an relic of earlier processing.
                    return;
                } else {
                    LOGGER.error("Whoops. Unexpected things happen. Assignment is neither current, old nor new (replace delta)\n{}", assignmentElement.debugDump(1));
                    throw new SystemException("Whoops. Unexpected things happen. Assignment is neither current, old nor new (replace delta).");
                }

            } else {

                // ADD/DELETE of entire assignment or small changes inside existing assignments
                // This is the usual situation.
                // Just sort assignments to sets: unchanged (zero), added (plus), removed (minus)
                if (isAssignmentChanged) {
                    // There was some change

                    boolean isAdd = currentAssignmentDelta.isValueToAdd(assignmentCVal, true);
                    boolean isDelete = currentAssignmentDelta.isValueToDelete(assignmentCVal, true);
                    if (isAdd & !isDelete) {
                        // Entirely new assignment is added
                        if (assignmentElement.isCurrent() && assignmentElement.isOld()) {
                            // Phantom add: adding assignment that is already there
                            LOGGER.trace("Processing changed assignment, phantom add: {}", SchemaDebugUtil.prettyPrintLazily(assignmentCVal));
                            EvaluatedAssignmentImpl<AH> evaluatedAssignment = evaluateAssignment(createAssignmentIdiNoChange(assignmentCVal), PlusMinusZero.ZERO, false, assignmentPlacementDesc, assignmentElement);
                            if (evaluatedAssignment == null) {
                                return;
                            }
                            evaluatedAssignment.setWasValid(evaluatedAssignment.isValid());
                            collectToZero(evaluatedAssignmentTriple, evaluatedAssignment, forceRecon);
                        } else {
                            LOGGER.trace("Processing changed assignment, add: {}", SchemaDebugUtil.prettyPrintLazily(assignmentCVal));
                            EvaluatedAssignmentImpl<AH> evaluatedAssignment = evaluateAssignment(createAssignmentIdiAdd(assignmentCVal), PlusMinusZero.PLUS, false, assignmentPlacementDesc, assignmentElement);
                            if (evaluatedAssignment == null) {
                                return;
                            }
                            evaluatedAssignment.setWasValid(false);
                            collectToPlus(evaluatedAssignmentTriple, evaluatedAssignment, forceRecon);
                        }

                    } else if (isDelete && !isAdd) {
                        // Existing assignment is removed
                        LOGGER.trace("Processing changed assignment, delete: {}", SchemaDebugUtil.prettyPrintLazily(assignmentCVal));
                        EvaluatedAssignmentImpl<AH> evaluatedAssignment = evaluateAssignment(createAssignmentIdiDelete(assignmentCVal), PlusMinusZero.MINUS, true, assignmentPlacementDesc, assignmentElement);
                        if (evaluatedAssignment == null) {
                            return;
                        }
                        evaluatedAssignment.setWasValid(evaluatedAssignment.isValid());
                        collectToMinus(evaluatedAssignmentTriple, evaluatedAssignment, forceRecon);

                    } else {
                        // Small change inside an assignment
                        // The only thing that we need to worry about is assignment validity change. That is a cause
                        // of provisioning/deprovisioning of the projections. So check that explicitly. Other changes are
                        // not significant, i.e. reconciliation can handle them.
                        boolean wasActive = focusContext.getObjectOld() != null &&
                                LensUtil.isAssignmentValid(focusContext.getObjectOld().asObjectable(),
                                        assignmentCValOld.asContainerable(), now, beans.activationComputer, focusStateModel);
                        boolean isActive = focusContext.getObjectNew() != null &&
                                LensUtil.isAssignmentValid(focusContext.getObjectNew().asObjectable(),
                                        assignmentCValNew.asContainerable(), now, beans.activationComputer, focusStateModel);
                        ItemDeltaItem<PrismContainerValue<AssignmentType>, PrismContainerDefinition<AssignmentType>> assignmentIdi =
                                createAssignmentIdiInternalChange(assignmentCVal, subItemDeltas);
                        if (isActive == wasActive) {
                            // No change in activation -> right to the zero set
                            // The change is not significant for assignment applicability. Recon will sort out the details.
                            LOGGER.trace("Processing changed assignment, minor change (add={}, delete={}, valid={}): {}",
                                    isAdd, isDelete, isActive, SchemaDebugUtil.prettyPrintLazily(assignmentCVal));
                            EvaluatedAssignmentImpl<AH> evaluatedAssignment = evaluateAssignment(assignmentIdi, PlusMinusZero.ZERO, false, assignmentPlacementDesc, assignmentElement);
                            if (evaluatedAssignment == null) {
                                return;
                            }
                            // TODO what about the condition state change?! MID-6404
                            evaluatedAssignment.setWasValid(evaluatedAssignment.isValid());
                            collectToZero(evaluatedAssignmentTriple, evaluatedAssignment, true);
                        } else if (isActive) {
                            // Assignment became active. We need to place it in plus set to initiate provisioning
                            // TODO change this! We should keep it in the zero set, and determine plus/minus according to validity change. MID-6403
                            LOGGER.trace("Processing changed assignment, assignment becomes valid (add={}, delete={}): {}",
                                    isAdd, isDelete, SchemaDebugUtil.prettyPrintLazily(assignmentCVal));
                            EvaluatedAssignmentImpl<AH> evaluatedAssignment = evaluateAssignment(assignmentIdi, PlusMinusZero.PLUS, false, assignmentPlacementDesc, assignmentElement);
                            if (evaluatedAssignment == null) {
                                return;
                            }
                            evaluatedAssignment.setWasValid(false);
                            collectToPlus(evaluatedAssignmentTriple, evaluatedAssignment, true);
                        } else {
                            // Assignment became invalid. We need to place is in minus set to initiate deprovisioning
                            // TODO change this! We should keep it in the zero set, and determine plus/minus according to validity change. MID-6403
                            LOGGER.trace("Processing changed assignment, assignment becomes invalid (add={}, delete={}): {}",
                                    isAdd, isDelete, SchemaDebugUtil.prettyPrintLazily(assignmentCVal));
                            EvaluatedAssignmentImpl<AH> evaluatedAssignment = evaluateAssignment(assignmentIdi, PlusMinusZero.MINUS, false, assignmentPlacementDesc, assignmentElement);
                            if (evaluatedAssignment == null) {
                                return;
                            }
                            evaluatedAssignment.setWasValid(true);
                            collectToMinus(evaluatedAssignmentTriple, evaluatedAssignment, true);
                        }
                    }

                } else {
                    // No change in assignment
                    if (LOGGER.isTraceEnabled()) {
                        LOGGER.trace("Processing unchanged assignment ({}) {}",
                                assignmentElement.isCurrent() ? "present" : "not present",
                                SchemaDebugUtil.prettyPrint(assignmentCVal));
                    }
                    EvaluatedAssignmentImpl<AH> evaluatedAssignment = evaluateAssignment(createAssignmentIdiNoChange(assignmentCVal), PlusMinusZero.ZERO, false, assignmentPlacementDesc, assignmentElement);
                    if (evaluatedAssignment == null) {
                        return;
                    }
                    // NOTE: unchanged may mean both:
                    //   * was there before, is there now
                    //   * was not there before, is not there now
                    evaluatedAssignment.setWasValid(evaluatedAssignment.isValid());
                    if (assignmentElement.isCurrent()) {
                        collectToZero(evaluatedAssignmentTriple, evaluatedAssignment, forceRecon);
                    } else {
                        collectToMinus(evaluatedAssignmentTriple, evaluatedAssignment, forceRecon);
                    }
                }
            }
        }
    }

    private ItemDeltaItem<PrismContainerValue<AssignmentType>, PrismContainerDefinition<AssignmentType>> createAssignmentIdiNoChange(PrismContainerValue<AssignmentType> cval) throws SchemaException {
        PrismContainerDefinition<AssignmentType> definition = cval.getDefinition();
        if (definition == null) {
            // TODO: optimize
            definition = beans.prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(AssignmentHolderType.class).findContainerDefinition(AssignmentHolderType.F_ASSIGNMENT);
        }
        return new ItemDeltaItem<>(LensUtil.createAssignmentSingleValueContainer(cval.asContainerable()), definition);
    }

    private ItemDeltaItem<PrismContainerValue<AssignmentType>, PrismContainerDefinition<AssignmentType>> createAssignmentIdiAdd(
            PrismContainerValue<AssignmentType> cval) throws SchemaException {
        @SuppressWarnings({"unchecked", "raw"})
        ItemDelta<PrismContainerValue<AssignmentType>, PrismContainerDefinition<AssignmentType>> itemDelta = (ItemDelta<PrismContainerValue<AssignmentType>, PrismContainerDefinition<AssignmentType>>)
                getDeltaItemFragment(cval)
                        .add(cval.asContainerable().clone())
                        .asItemDelta();
        ItemDeltaItem<PrismContainerValue<AssignmentType>, PrismContainerDefinition<AssignmentType>> idi = new ItemDeltaItem<>(
                null, itemDelta, null, cval.getDefinition());
        idi.recompute();
        return idi;
    }

    private S_ValuesEntry getDeltaItemFragment(PrismContainerValue<AssignmentType> cval) throws SchemaException {
        PrismContainerDefinition<AssignmentType> definition = cval.getParent() != null ? cval.getParent().getDefinition() : null;
        if (definition == null) {
            // we use custom definition, if available; if not, we find the standard one
            definition = beans.prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(AssignmentHolderType.class)
                    .findItemDefinition(AssignmentHolderType.F_ASSIGNMENT);
        }
        definition = definition.clone();
        definition.toMutable().setMaxOccurs(1);
        return beans.prismContext.deltaFor(AssignmentHolderType.class)
                .item(AssignmentHolderType.F_ASSIGNMENT, definition);
    }

    private ItemDeltaItem<PrismContainerValue<AssignmentType>, PrismContainerDefinition<AssignmentType>> createAssignmentIdiDelete(
            PrismContainerValue<AssignmentType> cval) throws SchemaException {
        @SuppressWarnings({"unchecked", "raw"})
        ItemDelta<PrismContainerValue<AssignmentType>, PrismContainerDefinition<AssignmentType>> itemDelta = (ItemDelta<PrismContainerValue<AssignmentType>, PrismContainerDefinition<AssignmentType>>)
                getDeltaItemFragment(cval)
                        .delete(cval.asContainerable().clone())
                        .asItemDelta();

        ItemDeltaItem<PrismContainerValue<AssignmentType>, PrismContainerDefinition<AssignmentType>> idi = new ItemDeltaItem<>(
                LensUtil.createAssignmentSingleValueContainer(cval.asContainerable()), itemDelta, null, cval.getDefinition());
        idi.recompute();
        return idi;
    }

    private ItemDeltaItem<PrismContainerValue<AssignmentType>, PrismContainerDefinition<AssignmentType>> createAssignmentIdiInternalChange(
            PrismContainerValue<AssignmentType> cval, Collection<? extends ItemDelta<?, ?>> subItemDeltas)
            throws SchemaException {
        ItemDeltaItem<PrismContainerValue<AssignmentType>, PrismContainerDefinition<AssignmentType>> idi =
                new ItemDeltaItem<>(LensUtil.createAssignmentSingleValueContainer(cval.asContainerable()), cval.getDefinition());
        idi.setResolvePath(AssignmentHolderType.F_ASSIGNMENT);
        idi.setSubItemDeltas(subItemDeltas);
        idi.recompute();
        return idi;
    }

    private Collection<? extends ItemDelta<?,?>> getAssignmentItemDeltas(LensFocusContext<AH> focusContext, Long id) {
        ObjectDelta<AH> focusDelta = focusContext.getCurrentDelta();  // TODO is this correct?
        if (focusDelta == null) {
            return null;
        }
        return focusDelta.findItemDeltasSubPath(ItemPath.create(AssignmentHolderType.F_ASSIGNMENT, id));
    }

    private void collectToZero(DeltaSetTriple<EvaluatedAssignmentImpl<AH>> evaluatedAssignmentTriple,
            EvaluatedAssignmentImpl<AH> evaluatedAssignment, boolean forceRecon) {
        if (forceRecon) {
            evaluatedAssignment.setForceRecon(true);
        }
        evaluatedAssignmentTriple.addToZeroSet(evaluatedAssignment);
    }

    private void collectToPlus(DeltaSetTriple<EvaluatedAssignmentImpl<AH>> evaluatedAssignmentTriple,
            EvaluatedAssignmentImpl<AH> evaluatedAssignment, boolean forceRecon) {
        if (forceRecon) {
            evaluatedAssignment.setForceRecon(true);
        }
        evaluatedAssignmentTriple.addToPlusSet(evaluatedAssignment);
    }

    private void collectToMinus(DeltaSetTriple<EvaluatedAssignmentImpl<AH>> evaluatedAssignmentTriple,
            EvaluatedAssignmentImpl<AH> evaluatedAssignment, boolean forceRecon) {
        if (forceRecon) {
            evaluatedAssignment.setForceRecon(true);
        }
        evaluatedAssignmentTriple.addToMinusSet(evaluatedAssignment);
    }

    private EvaluatedAssignmentImpl<AH> evaluateAssignment(ItemDeltaItem<PrismContainerValue<AssignmentType>,PrismContainerDefinition<AssignmentType>> assignmentIdi,
            PlusMinusZero mode, boolean evaluateOld, String assignmentPlacementDesc, SmartAssignmentElement smartAssignment) throws SchemaException, ExpressionEvaluationException, PolicyViolationException, SecurityViolationException, ConfigurationException, CommunicationException {
        OperationResult subResult = result.createMinorSubresult(OP_EVALUATE_ASSIGNMENT);
        PrismContainerValue<AssignmentType> assignment = assignmentIdi.getSingleValue(evaluateOld);
        subResult.addParam("assignment", assignment != null ? FocusTypeUtil.dumpAssignment(assignment.asContainerable()) : null);
        subResult.addArbitraryObjectAsParam("mode", mode);
        subResult.addParam("assignmentPlacementDescription", assignmentPlacementDesc);
        try {
            // Evaluate assignment. This follows to the assignment targets, follows to the inducements,
            // evaluates all the expressions, etc.
            EvaluatedAssignmentImpl<AH> evaluatedAssignment = assignmentEvaluator.evaluate(assignmentIdi, mode, evaluateOld,
                    source, assignmentPlacementDesc, smartAssignment.getOrigin(), task, subResult);
            subResult.recordSuccess();
            LOGGER.trace("Evaluated assignment:\n{}", evaluatedAssignment.debugDumpLazily(1));
            if (evaluatedAssignment.getTarget() != null) {
                subResult.addContext("assignmentTargetName", PolyString.getOrig(evaluatedAssignment.getTarget().getName()));
            }
            return evaluatedAssignment;
        } catch (ObjectNotFoundException ex) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Processing of assignment resulted in error {}: {}", ex, SchemaDebugUtil.prettyPrint(LensUtil.getAssignmentType(assignmentIdi, evaluateOld)));
            }
            if (ModelExecuteOptions.isForce(context.getOptions())) {
                subResult.recordHandledError(ex);
                return null;
            }
            ModelImplUtils.recordFatalError(subResult, ex);
            return null;
        } catch (SchemaException ex) {
            AssignmentType assignmentType = LensUtil.getAssignmentType(assignmentIdi, evaluateOld);
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Processing of assignment resulted in error {}: {}", ex, SchemaDebugUtil.prettyPrint(assignmentType));
            }
            ModelImplUtils.recordFatalError(subResult, ex);
            String resourceOid = FocusTypeUtil.determineConstructionResource(assignmentType);
            if (resourceOid == null) {
                // This is a role assignment or something like that. Just throw the original exception for now.
                throw ex;
            }
            ResourceShadowDiscriminator rad = new ResourceShadowDiscriminator(resourceOid,
                    FocusTypeUtil.determineConstructionKind(assignmentType),
                    FocusTypeUtil.determineConstructionIntent(assignmentType),
                    null, false);
            LensProjectionContext projCtx = context.findProjectionContext(rad);
            if (projCtx != null) {
                projCtx.setSynchronizationPolicyDecision(SynchronizationPolicyDecision.BROKEN);
            }
            return null;
        } catch (ExpressionEvaluationException | PolicyViolationException | SecurityViolationException | ConfigurationException |
                CommunicationException | RuntimeException | Error e) {
            AssignmentType assignmentType = LensUtil.getAssignmentType(assignmentIdi, evaluateOld);
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Processing of assignment resulted in error {}: {}", e, SchemaDebugUtil.prettyPrint(assignmentType));
            }
            subResult.recordFatalError(e);
            throw e;
        }
    }

    private void computeCurrentAssignmentDelta() throws SchemaException {
        currentAssignmentDelta = getCurrentAssignmentDelta(focusContext);
        currentAssignmentDelta.expand(focusContext.getObjectCurrent(), LOGGER);
    }

    /**
     * Returns delta of user assignments - for the current wave i.e. the one that transforms current to new object.
     */
    private ContainerDelta<AssignmentType> getCurrentAssignmentDelta(LensFocusContext<AH> focusContext) {
        ObjectDelta<AH> focusDelta = focusContext.getCurrentDelta();
        ContainerDelta<AssignmentType> assignmentDelta = focusDelta != null ?
                focusDelta.findContainerDelta(AssignmentHolderType.F_ASSIGNMENT) : null;
        if (assignmentDelta != null) {
            return assignmentDelta;
        } else {
            return createEmptyAssignmentDelta(focusContext);
        }
    }

    private ContainerDelta<AssignmentType> createEmptyAssignmentDelta(LensFocusContext<AH> focusContext) {
        return beans.prismContext.deltaFactory().container().create(getAssignmentContainerDefinition(focusContext));
    }

    private PrismContainerDefinition<AssignmentType> getAssignmentContainerDefinition(LensFocusContext<AH> focusContext) {
        return focusContext.getObjectDefinition().findContainerDefinition(AssignmentHolderType.F_ASSIGNMENT);
    }

    boolean isMemberOfInvocationResultChanged(DeltaSetTriple<EvaluatedAssignmentImpl<AH>> evaluatedAssignmentTriple) {
        return assignmentEvaluator.isMemberOfInvocationResultChanged(evaluatedAssignmentTriple);
    }

    public static final class Builder<AH extends AssignmentHolderType> {
        private LensContext<AH> context;
        private AssignmentHolderType source;
        private AssignmentEvaluator<AH> assignmentEvaluator;
        private ModelBeans beans;
        private XMLGregorianCalendar now;
        private Task task;
        private OperationResult result;

        public Builder<AH> context(LensContext<AH> val) {
            context = val;
            return this;
        }

        public Builder<AH> source(AssignmentHolderType val) {
            source = val;
            return this;
        }

        public Builder<AH> assignmentEvaluator(AssignmentEvaluator<AH> val) {
            assignmentEvaluator = val;
            return this;
        }

        public Builder<AH> beans(ModelBeans val) {
            beans = val;
            return this;
        }

        public Builder<AH> now(XMLGregorianCalendar val) {
            now = val;
            return this;
        }

        public Builder<AH> task(Task val) {
            task = val;
            return this;
        }

        public Builder<AH> result(OperationResult val) {
            result = val;
            return this;
        }

        public AssignmentTripleEvaluator<AH> build() throws SchemaException {
            return new AssignmentTripleEvaluator<>(this);
        }
    }
}
