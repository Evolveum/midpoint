/*
 * Copyright (c) 2017-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens.projector.focus;

import java.util.Collection;
import java.util.stream.Collectors;

import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.midpoint.common.ActivationComputer;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.context.SynchronizationPolicyDecision;
import com.evolveum.midpoint.model.impl.lens.AssignmentEvaluator;
import com.evolveum.midpoint.model.impl.lens.EvaluatedAssignmentImpl;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.model.impl.lens.LensUtil;
import com.evolveum.midpoint.model.impl.lens.projector.SmartAssignmentCollection;
import com.evolveum.midpoint.model.impl.lens.projector.SmartAssignmentElement;
import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.*;
import com.evolveum.midpoint.prism.delta.builder.S_ValuesEntry;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.ItemDeltaItem;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.FocusTypeUtil;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * Evaluates all assignments and sorts them to triple: added, removed and untouched assignments.
 *
 * @author semancik
 *
 */
public class AssignmentTripleEvaluator<AH extends AssignmentHolderType> {

    private static final Trace LOGGER = TraceManager.getTrace(AssignmentTripleEvaluator.class);

    private static final String OP_EVALUATE_ASSIGNMENT = AssignmentTripleEvaluator.class.getName()+".evaluateAssignment";

    private LensContext<AH> context;
    private AssignmentHolderType source;
    private AssignmentEvaluator<AH> assignmentEvaluator;
    private ActivationComputer activationComputer;
    private PrismContext prismContext;
    private XMLGregorianCalendar now;
    private Task task;
    private OperationResult result;

    private LifecycleStateModelType focusStateModel;

    public LensContext<AH> getContext() {
        return context;
    }

    public void setContext(LensContext<AH> context) {
        this.context = context;
        LensFocusContext<AH> focusContext = context.getFocusContext();
        if (focusContext != null) {
            focusStateModel = focusContext.getLifecycleModel();
        } else {
            focusStateModel = null;
        }
    }

    public AssignmentHolderType getSource() {
        return source;
    }

    public void setSource(AssignmentHolderType source) {
        this.source = source;
    }

    public AssignmentEvaluator<AH> getAssignmentEvaluator() {
        return assignmentEvaluator;
    }

    public void setAssignmentEvaluator(AssignmentEvaluator<AH> assignmentEvaluator) {
        this.assignmentEvaluator = assignmentEvaluator;
    }

    public ActivationComputer getActivationComputer() {
        return activationComputer;
    }

    public void setActivationComputer(ActivationComputer activationComputer) {
        this.activationComputer = activationComputer;
    }

    public PrismContext getPrismContext() {
        return prismContext;
    }

    public void setPrismContext(PrismContext prismContext) {
        this.prismContext = prismContext;
    }

    public XMLGregorianCalendar getNow() {
        return now;
    }

    public void setNow(XMLGregorianCalendar now) {
        this.now = now;
    }

    public Task getTask() {
        return task;
    }

    public void setTask(Task task) {
        this.task = task;
    }

    public OperationResult getResult() {
        return result;
    }

    public void setResult(OperationResult result) {
        this.result = result;
    }

    public void reset(boolean alsoMemberOfInvocations) {
        assignmentEvaluator.reset(alsoMemberOfInvocations);
    }

//    public DeltaSetTriple<EvaluatedAssignmentImpl<AH>> preProcessAssignments(PrismObject<TaskType> taskType) throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, PolicyViolationException, SecurityViolationException, ConfigurationException, CommunicationException {
//
////        LensFocusContext<AH> focusContext = context.getFocusContext();
//
//        SmartAssignmentCollection<AH> assignmentCollection = new SmartAssignmentCollection<>();
//
////        Collection<AssignmentType> forcedAssignments = LensUtil.getForcedAssignments(focusContext.getLifecycleModel(),
////                getNewObjectLifecycleState(focusContext), assignmentEvaluator.getObjectResolver(),
////                prismContext, task, result);
//
//        assignmentCollection.collectAssignmentsForPreprocessing(taskType.findContainer(TaskType.F_ASSIGNMENT), null);
//
//
//        if (LOGGER.isTraceEnabled()) {
//            LOGGER.trace("Assignment collection:\n{}", assignmentCollection.debugDump(1));
//        }
//
//        // Iterate over all the assignments. I mean really all. This is a union of the existing and changed assignments
//        // therefore it contains all three types of assignments (plus, minus and zero). As it is an union each assignment
//        // will be processed only once. Inside the loop we determine whether it was added, deleted or remains unchanged.
//        // This is a first step of the processing. It takes all the account constructions regardless of the resource and
//        // account type (intent). Therefore several constructions for the same resource and intent may appear in the resulting
//        // sets. This is not good as we want only a single account for each resource/intent combination. But that will be
//        // sorted out later.
//        DeltaSetTriple<EvaluatedAssignmentImpl<AH>> evaluatedAssignmentTriple = prismContext.deltaFactory().createDeltaSetTriple();
//        for (SmartAssignmentElement assignmentElement : assignmentCollection) {
//            processAssignment(evaluatedAssignmentTriple, null, null, assignmentElement);
//        }
//
//        return evaluatedAssignmentTriple;
//    }

    public DeltaSetTriple<EvaluatedAssignmentImpl<AH>> processAllAssignments() throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, PolicyViolationException, SecurityViolationException, ConfigurationException, CommunicationException {

        LensFocusContext<AH> focusContext = context.getFocusContext();

        ObjectDelta<AH> focusDelta = focusContext.getDelta();

        ContainerDelta<AssignmentType> assignmentDelta = getExecutionWaveAssignmentDelta(focusContext);
        assignmentDelta.expand(focusContext.getObjectCurrent(), LOGGER);

        LOGGER.trace("Assignment delta:\n{}", assignmentDelta.debugDump());

        SmartAssignmentCollection<AH> assignmentCollection = new SmartAssignmentCollection<>();

        Collection<AssignmentType> forcedAssignments = LensUtil.getForcedAssignments(focusContext.getLifecycleModel(),
                getNewObjectLifecycleState(focusContext), assignmentEvaluator.getObjectResolver(),
                prismContext, task, result);

        LOGGER.trace("Task for process: {}", task.debugDumpLazily());

        Collection<Task> allTasksToRoot = task.getPathToRootTask(result);
        Collection<AssignmentType> taskAssignments = allTasksToRoot.stream()
                .filter(taskPath -> taskPath.hasAssignments())
                .map(taskPath -> createTaskAssignment(taskPath))
                .collect(Collectors.toList());

        LOGGER.trace("Task assignment: {}", taskAssignments);

        assignmentCollection.collect(focusContext.getObjectCurrent(), focusContext.getObjectOld(), assignmentDelta, forcedAssignments, taskAssignments);

        LOGGER.trace("Assignment collection:\n{}", assignmentCollection.debugDumpLazily(1));

        // Iterate over all the assignments. I mean really all. This is a union of the existing and changed assignments
        // therefore it contains all three types of assignments (plus, minus and zero). As it is an union each assignment
        // will be processed only once. Inside the loop we determine whether it was added, deleted or remains unchanged.
        // This is a first step of the processing. It takes all the account constructions regardless of the resource and
        // account type (intent). Therefore several constructions for the same resource and intent may appear in the resulting
        // sets. This is not good as we want only a single account for each resource/intent combination. But that will be
        // sorted out later.
        DeltaSetTriple<EvaluatedAssignmentImpl<AH>> evaluatedAssignmentTriple = prismContext.deltaFactory().createDeltaSetTriple();
        for (SmartAssignmentElement assignmentElement : assignmentCollection) {
            processAssignment(evaluatedAssignmentTriple, focusDelta, assignmentDelta, assignmentElement);
        }

        return evaluatedAssignmentTriple;
    }

    private AssignmentType createTaskAssignment(Task fromTask) {
        AssignmentType taskAssignment = new AssignmentType(prismContext);
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

    private void processAssignment(DeltaSetTriple<EvaluatedAssignmentImpl<AH>> evaluatedAssignmentTriple,
            ObjectDelta<AH> focusDelta, ContainerDelta<AssignmentType> assignmentDelta, SmartAssignmentElement assignmentElement)
                    throws SchemaException, ExpressionEvaluationException, PolicyViolationException, SecurityViolationException, ConfigurationException, CommunicationException {

        final LensFocusContext<AH> focusContext = context.getFocusContext();
        final PrismContainerValue<AssignmentType> assignmentCVal = assignmentElement.getAssignmentCVal();
        final PrismContainerValue<AssignmentType> assignmentCValOld = assignmentCVal;
        PrismContainerValue<AssignmentType> assignmentCValNew = assignmentCVal;        // refined later

//        final boolean presentInCurrent = assignmentElement.isCurrent();
//        final boolean presentInOld = assignmentElement.isOld();
        // This really means whether the WHOLE assignment was changed (e.g. added/deleted/replaced). It tells nothing
        // about "micro-changes" inside assignment, these will be processed later.
        boolean isAssignmentChanged = assignmentElement.isChanged();            // refined later
        // TODO what about assignments that are present in old or current objects, and also changed?
        // TODO They seem to have "old"/"current" flag, not "changed" one. Is that OK?
        boolean forceRecon = false;
        final String assignmentPlacementDesc;

        Collection<? extends ItemDelta<?,?>> subItemDeltas = null;

        if (isAssignmentChanged) {
            // Whole assignment added or deleted
            assignmentPlacementDesc = "delta for "+source;
        } else {
            assignmentPlacementDesc = source.toString();
            Collection<? extends ItemDelta<?,?>> assignmentItemDeltas = getExecutionWaveAssignmentItemDeltas(focusContext, assignmentCVal.getId());
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

        if (focusDelta != null && focusDelta.isDelete()) {

            // USER DELETE
            // If focus (user) is being deleted that all the assignments are to be gone. Including those that
            // were not changed explicitly.
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Processing focus delete for: {}", SchemaDebugUtil.prettyPrint(assignmentCVal));
            }
            EvaluatedAssignmentImpl<AH> evaluatedAssignment = evaluateAssignment(createAssignmentIdiDelete(assignmentCVal), PlusMinusZero.MINUS, false, assignmentPlacementDesc, assignmentElement);
            if (evaluatedAssignment == null) {
                return;
            }
            evaluatedAssignment.setWasValid(evaluatedAssignment.isValid());
            collectToMinus(evaluatedAssignmentTriple, evaluatedAssignment, forceRecon);

        } else {
            if (assignmentDelta.isReplace()) {

                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Processing replace of all assignments for: {}", SchemaDebugUtil.prettyPrint(assignmentCVal));
                }
                // ASSIGNMENT REPLACE
                // Handling assignment replace delta. This needs to be handled specially as all the "old"
                // assignments should be considered deleted - except those that are part of the new value set
                // (remain after replace). As account delete and add are costly operations (and potentially dangerous)
                // we optimize here are consider the assignments that were there before replace and still are there
                // after it as unchanged.
                boolean hadValue = assignmentElement.isCurrent();
                boolean willHaveValue = assignmentDelta.isValueToReplace(assignmentCVal, true);
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

                    boolean isAdd = assignmentDelta.isValueToAdd(assignmentCVal, true);
                    boolean isDelete = assignmentDelta.isValueToDelete(assignmentCVal, true);
                    if (isAdd & !isDelete) {
                        // Entirely new assignment is added
                        if (assignmentElement.isCurrent() && assignmentElement.isOld()) {
                            // Phantom add: adding assignment that is already there
                            if (LOGGER.isTraceEnabled()) {
                                LOGGER.trace("Processing changed assignment, phantom add: {}", SchemaDebugUtil.prettyPrint(assignmentCVal));
                            }
                            EvaluatedAssignmentImpl<AH> evaluatedAssignment = evaluateAssignment(createAssignmentIdiNoChange(assignmentCVal), PlusMinusZero.ZERO, false, assignmentPlacementDesc, assignmentElement);
                            if (evaluatedAssignment == null) {
                                return;
                            }
                            evaluatedAssignment.setWasValid(evaluatedAssignment.isValid());
                            collectToZero(evaluatedAssignmentTriple, evaluatedAssignment, forceRecon);
                        } else {
                            if (LOGGER.isTraceEnabled()) {
                                LOGGER.trace("Processing changed assignment, add: {}", SchemaDebugUtil.prettyPrint(assignmentCVal));
                            }
                            EvaluatedAssignmentImpl<AH> evaluatedAssignment = evaluateAssignment(createAssignmentIdiAdd(assignmentCVal), PlusMinusZero.PLUS, false, assignmentPlacementDesc, assignmentElement);
                            if (evaluatedAssignment == null) {
                                return;
                            }
                            evaluatedAssignment.setWasValid(false);
                            collectToPlus(evaluatedAssignmentTriple, evaluatedAssignment, forceRecon);
                        }

                    } else if (isDelete && !isAdd) {
                        // Existing assignment is removed
                        if (LOGGER.isTraceEnabled()) {
                            LOGGER.trace("Processing changed assignment, delete: {}", SchemaDebugUtil.prettyPrint(assignmentCVal));
                        }
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
                        boolean isValidOld = focusContext.getObjectOld() != null &&
                                LensUtil.isAssignmentValid(focusContext.getObjectOld().asObjectable(),
                                        assignmentCValOld.asContainerable(), now, activationComputer, focusStateModel);
                        boolean isValid = focusContext.getObjectNew() != null &&
                                LensUtil.isAssignmentValid(focusContext.getObjectNew().asObjectable(),
                                        assignmentCValNew.asContainerable(), now, activationComputer, focusStateModel);
                        ItemDeltaItem<PrismContainerValue<AssignmentType>, PrismContainerDefinition<AssignmentType>> assignmentIdi =
                                createAssignmentIdiInternalChange(assignmentCVal, subItemDeltas);
                        if (isValid == isValidOld) {
                            // No change in validity -> right to the zero set
                            // The change is not significant for assignment applicability. Recon will sort out the details.
                            if (LOGGER.isTraceEnabled()) {
                                LOGGER.trace("Processing changed assignment, minor change (add={}, delete={}, valid={}): {}",
                                        isAdd, isDelete, isValid, SchemaDebugUtil.prettyPrint(assignmentCVal));
                            }
                            EvaluatedAssignmentImpl<AH> evaluatedAssignment = evaluateAssignment(assignmentIdi, PlusMinusZero.ZERO, false, assignmentPlacementDesc, assignmentElement);
                            if (evaluatedAssignment == null) {
                                return;
                            }
                            evaluatedAssignment.setWasValid(evaluatedAssignment.isValid());
                            collectToZero(evaluatedAssignmentTriple, evaluatedAssignment, true);
                        } else if (isValid) {
                            // Assignment became valid. We need to place it in plus set to initiate provisioning
                            if (LOGGER.isTraceEnabled()) {
                                LOGGER.trace("Processing changed assignment, assignment becomes valid (add={}, delete={}): {}",
                                        isAdd, isDelete, SchemaDebugUtil.prettyPrint(assignmentCVal));
                            }
                            EvaluatedAssignmentImpl<AH> evaluatedAssignment = evaluateAssignment(assignmentIdi, PlusMinusZero.PLUS, false, assignmentPlacementDesc, assignmentElement);
                            if (evaluatedAssignment == null) {
                                return;
                            }
                            evaluatedAssignment.setWasValid(false);
                            collectToPlus(evaluatedAssignmentTriple, evaluatedAssignment, true);
                        } else {
                            // Assignment became invalid. We need to place is in minus set to initiate deprovisioning
                            if (LOGGER.isTraceEnabled()) {
                                LOGGER.trace("Processing changed assignment, assignment becomes invalid (add={}, delete={}): {}",
                                        isAdd, isDelete, SchemaDebugUtil.prettyPrint(assignmentCVal));
                            }
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
            definition = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(AssignmentHolderType.class).findContainerDefinition(AssignmentHolderType.F_ASSIGNMENT);
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
            definition = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(AssignmentHolderType.class)
                    .findItemDefinition(AssignmentHolderType.F_ASSIGNMENT);
        }
        definition = definition.clone();
        definition.toMutable().setMaxOccurs(1);
        return prismContext.deltaFor(AssignmentHolderType.class)
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

    private Collection<? extends ItemDelta<?,?>> getExecutionWaveAssignmentItemDeltas(LensFocusContext<AH> focusContext, Long id) throws SchemaException {
        ObjectDelta<AH> focusDelta = focusContext.getWaveDelta(focusContext.getLensContext().getExecutionWave());
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

    /**
     * Returns delta of user assignments, both primary and secondary (merged together).
     * The returned object is (kind of) immutable. Changing it may do strange things (but most likely the changes will be lost).
     *
     * Originally we took only the delta related to current execution wave, to avoid re-processing of already executed assignments.
     * But MID-2422 shows that we need to take deltas from waves 0..N (N=current execution wave) [that effectively means all the secondary deltas]
     */
    private ContainerDelta<AssignmentType> getExecutionWaveAssignmentDelta(LensFocusContext<AH> focusContext) throws SchemaException {
        ObjectDelta<AH> focusDelta = focusContext.getAggregatedWaveDelta(focusContext.getLensContext().getExecutionWave());
        if (focusDelta == null) {
            return createEmptyAssignmentDelta(focusContext);
        }
        ContainerDelta<AssignmentType> assignmentDelta = focusDelta.findContainerDelta(AssignmentHolderType.F_ASSIGNMENT);
        if (assignmentDelta == null) {
            return createEmptyAssignmentDelta(focusContext);
        }
        return assignmentDelta;
    }

    private ContainerDelta<AssignmentType> createEmptyAssignmentDelta(LensFocusContext<AH> focusContext) {
        return prismContext.deltaFactory().container().create(getAssignmentContainerDefinition(focusContext));
    }

    private PrismContainerDefinition<AssignmentType> getAssignmentContainerDefinition(LensFocusContext<AH> focusContext) {
        return focusContext.getObjectDefinition().findContainerDefinition(AssignmentHolderType.F_ASSIGNMENT);
    }

    boolean isMemberOfInvocationResultChanged(DeltaSetTriple<EvaluatedAssignmentImpl<AH>> evaluatedAssignmentTriple) {
        return assignmentEvaluator.isMemberOfInvocationResultChanged(evaluatedAssignmentTriple);
    }
}
