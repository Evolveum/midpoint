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
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.model.impl.lens.LensUtil;
import com.evolveum.midpoint.model.impl.lens.assignments.AssignmentEvaluator;
import com.evolveum.midpoint.model.impl.lens.assignments.EvaluatedAssignmentImpl;
import com.evolveum.midpoint.model.impl.lens.assignments.EvaluationContext;
import com.evolveum.midpoint.model.impl.lens.projector.SmartAssignmentCollection;
import com.evolveum.midpoint.model.impl.lens.projector.SmartAssignmentElement;
import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.*;
import com.evolveum.midpoint.prism.delta.builder.S_ValuesEntry;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.ItemDeltaItem;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ConstructionTypeUtil;
import com.evolveum.midpoint.schema.util.FocusTypeUtil;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

import static com.evolveum.midpoint.prism.util.CloneUtil.cloneCollectionMembers;
import static com.evolveum.midpoint.schema.util.SchemaDebugUtil.prettyPrintLazily;
import static com.evolveum.midpoint.util.DebugUtil.lazy;
import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

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
     * Looks like the source of the evaluated assignments. It is initialized to focus object new (if exists),
     * or object current (otherwise).
     *
     * TODO research + explain this better
     */
    private final AssignmentHolderType source;

    private final AssignmentEvaluator<AH> assignmentEvaluator;
    private final ModelBeans beans;
    private final XMLGregorianCalendar now;
    private final Task task;
    private final OperationResult result;

    private final LifecycleStateModelType focusStateModel;

    @NotNull private final DeltaSetTriple<EvaluatedAssignmentImpl<AH>> evaluatedAssignmentTriple;

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

    @NotNull DeltaSetTriple<EvaluatedAssignmentImpl<AH>> processAllAssignments() throws ObjectNotFoundException, SchemaException,
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
    private Collection<AssignmentType> getVirtualAssignments() throws SchemaException, ObjectNotFoundException,
            CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        Collection<AssignmentType> forcedAssignments =
                focusContext.isDelete() ?
                        List.of() :
                        LensUtil.getForcedAssignments(
                                focusContext.getLifecycleModel(),
                                getNewObjectLifecycleState(focusContext),
                                beans.modelObjectResolver, beans.prismContext, task, result);
        LOGGER.trace("Forced assignments: {}", forcedAssignments);

        LOGGER.trace("Task for process (operation result is not updated): {}",
                lazy(() -> task.getRawTaskObjectClonedIfNecessary().debugDump()));
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
        AssignmentType taskAssignment = new AssignmentType();
        ObjectReferenceType targetRef = new ObjectReferenceType();
        targetRef.asReferenceValue().setObject(fromTask.getRawTaskObjectClonedIfNecessary());
        taskAssignment.setTargetRef(targetRef);
        return taskAssignment;
    }

    private String getNewObjectLifecycleState(LensFocusContext<AH> focusContext) {
        PrismObject<AH> focusNew = focusContext.getObjectNew();
        AH focusTypeNew = focusNew.asObjectable();
        return focusTypeNew.getLifecycleState();
    }

    private void processAssignment(SmartAssignmentElement assignmentElement)
            throws SchemaException, ExpressionEvaluationException, PolicyViolationException, SecurityViolationException,
            ConfigurationException, CommunicationException {

        // Whether the assignment was changed (either as a whole, or only in its content).
        boolean assignmentChanged;

        // Should we set forceRecon flag on the resulting evaluated assignment structure?
        boolean forceRecon;

        // Deltas that modify the content of the assignment.
        Collection<? extends ItemDelta<?,?>> innerAssignmentDeltas;

        // Human-readable description of the assignment "placement" (not quite concise name).
        String assignmentPlacementDesc;

        if (assignmentElement.isCurrent() != assignmentElement.isNew()) {
            // Whole assignment added or deleted
            assignmentChanged = true;
            forceRecon = false;
            innerAssignmentDeltas = List.of();
            assignmentPlacementDesc = "delta for "+source;
        } else {
            innerAssignmentDeltas = getInnerAssignmentDeltas(focusContext, assignmentElement);
            if (!innerAssignmentDeltas.isEmpty()) {
                // There are small changes inside assignment, but otherwise the assignment stays as it is (not added or deleted).
                // The further processing of innerAssignmentDeltas will handle some changes. But not all of them.
                // E.g. a replace of the whole construction will not be handled properly.
                // Therefore we force recon to sort it out.
                assignmentChanged = true;
                forceRecon = true;
            } else {
                assignmentChanged = false;
                forceRecon = false;
            }
            assignmentPlacementDesc = source.toString();
        }

        if (focusContext.isDelete()) {
            // Special case 1: focus is being deleted
            processAssignmentOnFocusDelete(assignmentElement, forceRecon, assignmentPlacementDesc);
        } else if (currentAssignmentDelta.isReplace()) {
            // Special case 2: assignments are being replaced
            processAssignmentReplace(assignmentElement, forceRecon, assignmentPlacementDesc);
        } else if (assignmentChanged) {
            // Standard case 1: the assignment is added, deleted, or modified
            processAddedOrDeletedOrChangedAssignment(assignmentElement, forceRecon, assignmentPlacementDesc,
                    innerAssignmentDeltas);
        } else {
            // Standard case 2: the assignment is unchanged in this wave (still present in current object or already gone)
            processReallyUnchangedAssignment(assignmentElement, assignmentPlacementDesc);
        }
    }

    /**
     * Processes an assignment in when the whole focus is deleted. In such situation all the assignments are to be gone.
     * Including those that were not changed explicitly.
     */
    private void processAssignmentOnFocusDelete(SmartAssignmentElement assignmentElement, boolean forceRecon,
            String assignmentPlacementDesc)
            throws SchemaException, ExpressionEvaluationException, PolicyViolationException, SecurityViolationException,
            ConfigurationException, CommunicationException {
        LOGGER.trace("Processing focus delete for: {}", printLazily(assignmentElement));
        evaluateAsDeleted(assignmentElement, forceRecon, assignmentPlacementDesc);
    }

    /**
     * Processes an assignment when the whole assignment container is being replaced.
     *
     * This needs to be handled specially as all the "old" assignments should be considered deleted - except those
     * that are part of the new value set (remain after replace). As account delete and add are costly operations
     * (and potentially dangerous) we optimize here are consider the assignments that were there before replace
     * and still are there after it as unchanged.
     */
    private void processAssignmentReplace(SmartAssignmentElement assignmentElement, boolean forceRecon,
            String assignmentPlacementDesc)
            throws SchemaException, ExpressionEvaluationException, PolicyViolationException, SecurityViolationException,
            ConfigurationException, CommunicationException {
        LOGGER.trace("Processing replace of all assignments for: {}", printLazily(assignmentElement));
        boolean existed = assignmentElement.isCurrent();
        boolean willExist = assignmentElement.isNew();
        if (existed && willExist) {
            evaluateAsUnchanged(assignmentElement, forceRecon, assignmentPlacementDesc);
        } else if (willExist) {
            evaluateAsAdded(assignmentElement, forceRecon, assignmentPlacementDesc);
        } else if (existed) {
            evaluateAsDeleted(assignmentElement, forceRecon, assignmentPlacementDesc);
        } else if (assignmentElement.isOld()) {
            // This is OK, safe to skip. This is just an relic of earlier processing.
        } else {
            String msg = "Whoops. Unexpected things happen. Assignment is neither current, "
                    + "old nor new - while processing replace delta.";
            LOGGER.error("{}\n{}", msg, assignmentElement.debugDump(1));
            throw new SystemException(msg);
        }
    }

    /**
     * Add/delete of entire assignment or some changes inside existing assignments.
     */
    private void processAddedOrDeletedOrChangedAssignment(SmartAssignmentElement assignmentElement, boolean forceRecon,
            String assignmentPlacementDesc, Collection<? extends ItemDelta<?, ?>> innerAssignmentDeltas)
            throws SchemaException, ExpressionEvaluationException, PolicyViolationException, SecurityViolationException,
            ConfigurationException, CommunicationException {
        boolean added = assignmentElement.getOrigin().isInDeltaAdd();
        boolean deleted = assignmentElement.getOrigin().isInDeltaDelete();
        if (added && !deleted) {
            if (assignmentElement.isCurrent()) {
                LOGGER.trace("Processing phantom assignment add (adding assignment that is already there): {}",
                        printLazily(assignmentElement));
                evaluateAsUnchanged(assignmentElement, forceRecon, assignmentPlacementDesc);
            } else {
                LOGGER.trace("Processing assignment add: {}", printLazily(assignmentElement));
                evaluateAsAdded(assignmentElement, forceRecon, assignmentPlacementDesc);
            }
        } else if (deleted && !added) {
            LOGGER.trace("Processing assignment deletion: {}", printLazily(assignmentElement));
            evaluateAsDeleted(assignmentElement, forceRecon, assignmentPlacementDesc);
            // TODO what about phantom deletes?
        } else {
            processInternallyChangedAssignment(assignmentElement, assignmentPlacementDesc, innerAssignmentDeltas,
                    added, deleted);
        }
    }

    /**
     * Process an assignment with an inside change.
     *
     * The only thing that we need to worry about is assignment validity change. That is a cause
     * of provisioning/de-provisioning of the projections. So check that explicitly. Other changes are
     * not significant, i.e. reconciliation can handle them.
     *
     * TODO We expect that added == deleted == false. But can they both be true?
     */
    private void processInternallyChangedAssignment(SmartAssignmentElement assignmentElement, String assignmentPlacementDesc,
            Collection<? extends ItemDelta<?, ?>> innerAssignmentDeltas, boolean added, boolean deleted)
            throws SchemaException, ExpressionEvaluationException, PolicyViolationException, SecurityViolationException,
            ConfigurationException, CommunicationException {

        PrismContainerValue<AssignmentType> assignmentValueBefore = assignmentElement.getAssignmentCVal();
        PrismContainerValue<AssignmentType> assignmentValueAfter = innerAssignmentDeltas.isEmpty() ?
                assignmentValueBefore : getAssignmentNewValue(assignmentElement);

        PrismObject<AH> focusBefore = assignmentElement.isOld() && !assignmentElement.isCurrent() ?
                focusContext.getObjectOld() : focusContext.getObjectCurrent();
        boolean wasActive = focusBefore != null &&
                LensUtil.isAssignmentValid(
                        focusBefore.asObjectable(), assignmentValueBefore.asContainerable(),
                        now, beans.activationComputer, focusStateModel, task.getExecutionMode());
        boolean isActive = focusContext.getObjectNew() != null &&
                LensUtil.isAssignmentValid(
                        focusContext.getObjectNew().asObjectable(), assignmentValueAfter.asContainerable(),
                        now, beans.activationComputer, focusStateModel, task.getExecutionMode());

        ItemDeltaItem<PrismContainerValue<AssignmentType>, PrismContainerDefinition<AssignmentType>> assignmentIdi =
                createAssignmentIdiInternalChange(assignmentValueBefore, innerAssignmentDeltas);

        if (isActive == wasActive) {
            // No change in activation -> right to the zero set
            // The change is not significant for assignment applicability. Recon will sort out the details.
            LOGGER.trace("Processing changed assignment, minor change (add={}, delete={}, valid={}): {}",
                    added, deleted, isActive, prettyPrintLazily(assignmentValueBefore));
            EvaluatedAssignmentImpl<AH> evaluatedAssignment = evaluateAssignment(assignmentIdi, PlusMinusZero.ZERO,
                    false, assignmentPlacementDesc, assignmentElement);
            if (evaluatedAssignment != null) { // TODO what about the condition state change?! MID-6404
                evaluatedAssignment.setWasValid(evaluatedAssignment.isValid());
                collectToZero(evaluatedAssignmentTriple, evaluatedAssignment, true);
            }
        } else if (isActive) {
            // Assignment became active. We need to place it in plus set to initiate provisioning
            // TODO review this: MID-6403
            LOGGER.trace("Processing changed assignment, assignment becomes valid (add={}, delete={}): {}",
                    added, deleted, prettyPrintLazily(assignmentValueBefore));
            EvaluatedAssignmentImpl<AH> evaluatedAssignment = evaluateAssignment(assignmentIdi, PlusMinusZero.PLUS,
                    false, assignmentPlacementDesc, assignmentElement);
            if (evaluatedAssignment != null) {
                evaluatedAssignment.setWasValid(false);
                collectToPlus(evaluatedAssignmentTriple, evaluatedAssignment, true);
            }
        } else {
            // Assignment became invalid. We need to place is in minus set to initiate de-provisioning
            // TODO review this: MID-6403
            LOGGER.trace("Processing changed assignment, assignment becomes invalid (add={}, delete={}): {}",
                    added, deleted, prettyPrintLazily(assignmentValueBefore));
            EvaluatedAssignmentImpl<AH> evaluatedAssignment = evaluateAssignment(assignmentIdi, PlusMinusZero.MINUS,
                    false, assignmentPlacementDesc, assignmentElement);
            if (evaluatedAssignment != null) {
                evaluatedAssignment.setWasValid(true);
                collectToMinus(evaluatedAssignmentTriple, evaluatedAssignment, true);
            }
        }
    }

    /** Evaluate the whole assignment as being added. */
    private void evaluateAsAdded(SmartAssignmentElement assignmentElement, boolean forceRecon, String assignmentPlacementDesc)
            throws SchemaException, ExpressionEvaluationException, PolicyViolationException, SecurityViolationException,
            ConfigurationException, CommunicationException {
        EvaluatedAssignmentImpl<AH> evaluatedAssignment = evaluateAssignment(
                createAssignmentIdiAdd(assignmentElement), PlusMinusZero.PLUS, false,
                assignmentPlacementDesc, assignmentElement);
        if (evaluatedAssignment != null) {
            evaluatedAssignment.setWasValid(false);
            collectToPlus(evaluatedAssignmentTriple, evaluatedAssignment, forceRecon);
        }
    }

    /** Evaluate the whole assignment as being deleted. */
    private void evaluateAsDeleted(SmartAssignmentElement assignmentElement, boolean forceRecon, String assignmentPlacementDesc)
            throws SchemaException, ExpressionEvaluationException, PolicyViolationException, SecurityViolationException,
            ConfigurationException, CommunicationException {
        EvaluatedAssignmentImpl<AH> evaluatedAssignment = evaluateAssignment(
                createAssignmentIdiDelete(assignmentElement), PlusMinusZero.MINUS, true,
                assignmentPlacementDesc, assignmentElement);
        if (evaluatedAssignment != null) {
            evaluatedAssignment.setWasValid(evaluatedAssignment.isValid());
            collectToMinus(evaluatedAssignmentTriple, evaluatedAssignment, forceRecon);
        }
    }

    /**
     * Evaluate the whole assignment as being unchanged:
     *
     * 1. either present in replace delta (but also present in the current object),
     * 2. or being added but also present in the current object - i.e. phantom add.
     *
     * TODO:
     *  The assignment ID matches. But the content may or may not match.
     *  So, shouldn't we check whether to force recon?
     *  Also, shouldn't we evaluate validity to place it in plus/minus set, if needed?
     */
    private void evaluateAsUnchanged(SmartAssignmentElement assignmentElement, boolean forceRecon, String assignmentPlacementDesc)
            throws SchemaException, ExpressionEvaluationException, PolicyViolationException, SecurityViolationException,
            ConfigurationException, CommunicationException {
        EvaluatedAssignmentImpl<AH> evaluatedAssignment = evaluateAssignment(
                createAssignmentIdiNoChange(assignmentElement), PlusMinusZero.ZERO, false,
                assignmentPlacementDesc, assignmentElement);
        if (evaluatedAssignment != null) {
            evaluatedAssignment.setWasValid(evaluatedAssignment.isValid());
            collectToZero(evaluatedAssignmentTriple, evaluatedAssignment, forceRecon);
        }
    }

    /**
     * Process an assignment that was really not touched by the current delta.
     *
     * There are two cases here:
     *
     * 1. Assignment is present in the current object. This is the usual case.
     * 2. Assignment is no longer there. It was present in the old object but has been deleted in some of the previous waves.
     *
     * We will evaluate the assignment even in the second case. It is because there may be resource object constructions
     * targeted at the current wave that need the information that the assignment is gone. On the other hand, focus-related
     * artifacts (like focus mappings) present in the assignment should be ignored.
     *
     * What should be the primary mode and the target triple set?
     *
     * - for projections (old->new scope) the mode and set should be MINUS;
     * - for focus (current->new scope) the assignment should be ignored.
     *
     * So we'll put the assignment into minus set (as before), with the mode of MINUS (was zero before MID-7382).
     * Fortunately, the primary mode is not used much, see {@link EvaluationContext#primaryAssignmentMode}. And the
     * plus/minus/zero set placement should be reviewed anyway, as it is ambiguous: MID-6403.
     *
     * TODO We should somehow ensure that the focus-related artifacts are ignored from these no-longer-present assignments.
     */
    private void processReallyUnchangedAssignment(SmartAssignmentElement assignmentElement, String assignmentPlacementDesc)
            throws SchemaException, ExpressionEvaluationException, PolicyViolationException, SecurityViolationException,
            ConfigurationException, CommunicationException {

        LOGGER.trace("Processing unchanged assignment (origin: {}): {}",
                assignmentElement.getOrigin(), printLazily(assignmentElement));

        PlusMinusZero primaryAssignmentMode;
        if (assignmentElement.isCurrent()) {
            primaryAssignmentMode = PlusMinusZero.ZERO;
        } else {
            stateCheck(assignmentElement.isOld(),
                    "Unchanged assignment not present in current nor old object? %s", assignmentElement);
            primaryAssignmentMode = PlusMinusZero.MINUS;
        }

        EvaluatedAssignmentImpl<AH> evaluatedAssignment = evaluateAssignment(
                createAssignmentIdiNoChange(assignmentElement), primaryAssignmentMode, false,
                assignmentPlacementDesc, assignmentElement);

        if (evaluatedAssignment != null) {
            evaluatedAssignment.setWasValid(evaluatedAssignment.isValid());
            if (primaryAssignmentMode == PlusMinusZero.ZERO) {
                collectToZero(evaluatedAssignmentTriple, evaluatedAssignment, false);
            } else {
                collectToMinus(evaluatedAssignmentTriple, evaluatedAssignment, false);
            }
        }
    }

    private ItemDeltaItem<PrismContainerValue<AssignmentType>, PrismContainerDefinition<AssignmentType>> createAssignmentIdiNoChange(
            SmartAssignmentElement element) throws SchemaException {
        PrismContainerValue<AssignmentType> value = element.getAssignmentCVal();
        PrismContainerDefinition<AssignmentType> definition = value.getDefinition();
        if (definition == null) {
            // TODO: optimize
            definition = beans.prismContext.getSchemaRegistry()
                    .findObjectDefinitionByCompileTimeClass(AssignmentHolderType.class)
                    .findContainerDefinition(AssignmentHolderType.F_ASSIGNMENT);
        }
        return new ItemDeltaItem<>(LensUtil.createAssignmentSingleValueContainer(value.asContainerable()), definition);
    }

    private ItemDeltaItem<PrismContainerValue<AssignmentType>, PrismContainerDefinition<AssignmentType>> createAssignmentIdiAdd(
            SmartAssignmentElement element) throws SchemaException {
        PrismContainerValue<AssignmentType> value = element.getAssignmentCVal();
        @SuppressWarnings({"unchecked", "raw"})
        ItemDelta<PrismContainerValue<AssignmentType>, PrismContainerDefinition<AssignmentType>> itemDelta =
                (ItemDelta<PrismContainerValue<AssignmentType>, PrismContainerDefinition<AssignmentType>>)
                        getDeltaItemFragment(value)
                                .add(value.asContainerable().clone())
                                .asItemDelta();
        ItemDeltaItem<PrismContainerValue<AssignmentType>, PrismContainerDefinition<AssignmentType>> idi =
                new ItemDeltaItem<>(null, itemDelta, null, value.getDefinition());
        idi.recompute();
        return idi;
    }

    private S_ValuesEntry getDeltaItemFragment(PrismContainerValue<AssignmentType> value) throws SchemaException {
        PrismContainerDefinition<AssignmentType> definition = value.getParent() != null ? value.getParent().getDefinition() : null;
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
            SmartAssignmentElement element) throws SchemaException {
        PrismContainerValue<AssignmentType> value = element.getAssignmentCVal();
        @SuppressWarnings({"unchecked", "raw"})
        ItemDelta<PrismContainerValue<AssignmentType>, PrismContainerDefinition<AssignmentType>> itemDelta =
                (ItemDelta<PrismContainerValue<AssignmentType>, PrismContainerDefinition<AssignmentType>>)
                        getDeltaItemFragment(value)
                                .delete(value.asContainerable().clone())
                                .asItemDelta();

        PrismContainer<AssignmentType> container = LensUtil.createAssignmentSingleValueContainer(value.asContainerable());
        ItemDeltaItem<PrismContainerValue<AssignmentType>, PrismContainerDefinition<AssignmentType>> idi =
                new ItemDeltaItem<>(container, itemDelta, null, value.getDefinition());
        idi.recompute();
        return idi;
    }

    private ItemDeltaItem<PrismContainerValue<AssignmentType>, PrismContainerDefinition<AssignmentType>> createAssignmentIdiInternalChange(
            PrismContainerValue<AssignmentType> value, Collection<? extends ItemDelta<?, ?>> subItemDeltas)
            throws SchemaException {
        ItemDeltaItem<PrismContainerValue<AssignmentType>, PrismContainerDefinition<AssignmentType>> idi =
                new ItemDeltaItem<>(LensUtil.createAssignmentSingleValueContainer(value.asContainerable()), value.getDefinition());
        idi.setResolvePath(AssignmentHolderType.F_ASSIGNMENT);
        idi.setSubItemDeltas(subItemDeltas);
        idi.recompute();
        return idi;
    }

    /** Returns deltas related to given assignment element. */
    private @NotNull Collection<? extends ItemDelta<?,?>> getInnerAssignmentDeltas(LensFocusContext<AH> focusContext,
            SmartAssignmentElement assignmentElement) {
        ObjectDelta<AH> focusDelta = focusContext.getCurrentDelta(); // TODO is this correct?
        if (focusDelta == null) {
            return List.of();
        }
        return focusDelta.findItemDeltasSubPath(
                ItemPath.create(AssignmentHolderType.F_ASSIGNMENT, assignmentElement.getAssignmentId()));
    }

    /** Returns new value of assignment corresponding to given assignment element. */
    private PrismContainerValue<AssignmentType> getAssignmentNewValue(SmartAssignmentElement assignmentElement) {
        return focusContext.getObjectNew().<AssignmentType>findContainer(AssignmentHolderType.F_ASSIGNMENT)
                .getValue(assignmentElement.getAssignmentId());
    }

    private void collectToZero(
            DeltaSetTriple<EvaluatedAssignmentImpl<AH>> evaluatedAssignmentTriple,
            EvaluatedAssignmentImpl<AH> evaluatedAssignment,
            boolean forceRecon) {
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

    /**
     * Evaluates the assignment.
     *
     * @param primaryAssignmentMode primary primaryAssignmentMode of the assignment. Beware, it's not quite well defined.
     * Please see {@link EvaluationContext#primaryAssignmentMode}.
     *
     * Returns null in exceptional situations.
     */
    private EvaluatedAssignmentImpl<AH> evaluateAssignment(
            ItemDeltaItem<PrismContainerValue<AssignmentType>,PrismContainerDefinition<AssignmentType>> assignmentIdi,
            PlusMinusZero primaryAssignmentMode, boolean evaluateOld, String assignmentPlacementDesc,
            SmartAssignmentElement smartAssignment)
            throws SchemaException, ExpressionEvaluationException, PolicyViolationException, SecurityViolationException,
            ConfigurationException, CommunicationException {
        OperationResult subResult = result.createMinorSubresult(OP_EVALUATE_ASSIGNMENT);
        PrismContainerValue<AssignmentType> assignment = assignmentIdi.getSingleValue(evaluateOld);
        subResult.addParam("assignment", assignment != null ? FocusTypeUtil.dumpAssignment(assignment.asContainerable()) : null);
        subResult.addArbitraryObjectAsParam("primaryAssignmentMode", primaryAssignmentMode);
        subResult.addParam("assignmentPlacementDescription", assignmentPlacementDesc);
        try {
            // Evaluate assignment. This follows to the assignment targets, follows to the inducements,
            // evaluates all the expressions, etc.
            EvaluatedAssignmentImpl<AH> evaluatedAssignment =
                    assignmentEvaluator.evaluate(
                            assignmentIdi,
                            primaryAssignmentMode,
                            evaluateOld,
                            source,
                            assignmentPlacementDesc,
                            smartAssignment.getOrigin(),
                            task,
                            subResult);
            subResult.recordSuccess();
            LOGGER.trace("Evaluated assignment:\n{}", evaluatedAssignment.debugDumpLazily(1));
            if (evaluatedAssignment.getTarget() != null) {
                subResult.addContext("assignmentTargetName", PolyString.getOrig(evaluatedAssignment.getTarget().getName()));
            }
            return evaluatedAssignment;
        } catch (ObjectNotFoundException ex) {
            // Actually, "not found" exceptions during targetRef resolution are not propagated here,
            // so this code maybe should be adapted.
            LOGGER.trace("Processing of assignment resulted in error {}: {}", ex,
                    lazy(() -> SchemaDebugUtil.prettyPrint(LensUtil.getAssignmentType(assignmentIdi, evaluateOld))));
            if (ModelExecuteOptions.isForce(context.getOptions())) {
                subResult.recordHandledError(ex);
            } else {
                ModelImplUtils.recordFatalError(subResult, ex);
            }
            return null;
        } catch (SchemaException ex) {
            AssignmentType assignmentBean = LensUtil.getAssignmentType(assignmentIdi, evaluateOld);
            LOGGER.trace("Processing of assignment resulted in error {}: {}", ex, prettyPrintLazily(assignmentBean));
            ModelImplUtils.recordFatalError(subResult, ex);
            ConstructionType construction = assignmentBean.getConstruction();
            String resourceOid = ConstructionTypeUtil.getResourceOid(construction);
            if (resourceOid != null) {
                context.markMatchingProjectionsBroken(construction, resourceOid);
                return null;
            }
            // This is a role assignment or something like that. Or we cannot get resource OID.
            // Just throw the original exception for now.
            throw ex;
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
    private ContainerDelta<AssignmentType> getCurrentAssignmentDelta(LensFocusContext<AH> focusContext) throws SchemaException {
        ObjectDelta<AH> focusDelta = focusContext.getCurrentDelta();
        if (ObjectDelta.isDelete(focusDelta)) {
            return getAssignmentDeltaOnDelete(focusContext);
        }

        ContainerDelta<AssignmentType> assignmentDelta = focusDelta != null ?
                focusDelta.findContainerDelta(AssignmentHolderType.F_ASSIGNMENT) : null;
        if (assignmentDelta != null) {
            return assignmentDelta;
        } else {
            return createEmptyAssignmentDelta(focusContext);
        }
    }

    private ContainerDelta<AssignmentType> getAssignmentDeltaOnDelete(LensFocusContext<AH> focusContext) throws SchemaException {
        PrismObject<AH> currentObject = focusContext.getObjectCurrent();
        if (currentObject != null) {
            AH currentObjectable = currentObject.asObjectable();
            //noinspection unchecked
            return (ContainerDelta<AssignmentType>) PrismContext.get().deltaFor(currentObjectable.getClass())
                    .item(AssignmentHolderType.F_ASSIGNMENT)
                        .deleteRealValues(cloneCollectionMembers(currentObjectable.getAssignment()))
                    .asItemDelta();
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
        return !focusContext.isDelete() && // we don't want to bother with isMemberOf when focus is being deleted
                assignmentEvaluator.isMemberOfInvocationResultChanged(evaluatedAssignmentTriple);
    }

    private Object printLazily(SmartAssignmentElement assignmentElement) {
        return prettyPrintLazily(assignmentElement.getAssignmentCVal());
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
