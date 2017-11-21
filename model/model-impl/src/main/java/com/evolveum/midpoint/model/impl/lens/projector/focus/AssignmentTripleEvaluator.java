/**
 * Copyright (c) 2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.model.impl.lens.projector.focus;

import com.evolveum.midpoint.common.ActivationComputer;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.context.SynchronizationPolicyDecision;
import com.evolveum.midpoint.model.impl.controller.ModelUtils;
import com.evolveum.midpoint.model.impl.lens.*;
import com.evolveum.midpoint.model.impl.lens.projector.SmartAssignmentCollection;
import com.evolveum.midpoint.model.impl.lens.projector.SmartAssignmentElement;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.delta.*;
import com.evolveum.midpoint.prism.delta.builder.DeltaBuilder;
import com.evolveum.midpoint.prism.delta.builder.S_ValuesEntry;
import com.evolveum.midpoint.prism.path.IdItemPathSegment;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;
import com.evolveum.midpoint.repo.common.expression.ItemDeltaItem;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.FocusTypeUtil;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import javax.xml.datatype.XMLGregorianCalendar;
import java.util.Collection;

/**
 * Evaluates all assignments and sorts them to triple: added, removed and untouched assignments.
 *
 * @author semancik
 *
 */
public class AssignmentTripleEvaluator<F extends FocusType> {

	private static final Trace LOGGER = TraceManager.getTrace(AssignmentTripleEvaluator.class);

	private LensContext<F> context;
	private ObjectType source;
	private AssignmentEvaluator<F> assignmentEvaluator;
	private ActivationComputer activationComputer;
	private PrismContext prismContext;
	private XMLGregorianCalendar now;
	private Task task;
	private OperationResult result;

	public LensContext<F> getContext() {
		return context;
	}

	public void setContext(LensContext<F> context) {
		this.context = context;
	}

	public ObjectType getSource() {
		return source;
	}

	public void setSource(ObjectType source) {
		this.source = source;
	}

	public AssignmentEvaluator<F> getAssignmentEvaluator() {
		return assignmentEvaluator;
	}

	public void setAssignmentEvaluator(AssignmentEvaluator<F> assignmentEvaluator) {
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

	public void reset() {
		assignmentEvaluator.reset();
	}

	public DeltaSetTriple<EvaluatedAssignmentImpl<F>> processAllAssignments() throws SchemaException, ExpressionEvaluationException, PolicyViolationException, SecurityViolationException, ConfigurationException, CommunicationException {

		LensFocusContext<F> focusContext = context.getFocusContext();

		ObjectDelta<F> focusDelta = focusContext.getDelta();

		ContainerDelta<AssignmentType> assignmentDelta = getExecutionWaveAssignmentDelta(focusContext);
        assignmentDelta.expand(focusContext.getObjectCurrent(), LOGGER);

        LOGGER.trace("Assignment delta:\n{}", assignmentDelta.debugDump());

        SmartAssignmentCollection<F> assignmentCollection = new SmartAssignmentCollection<>();
        assignmentCollection.collect(focusContext.getObjectCurrent(), focusContext.getObjectOld(), assignmentDelta);

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Assignment collection:\n{}", assignmentCollection.debugDump(1));
		}

		// Iterate over all the assignments. I mean really all. This is a union of the existing and changed assignments
        // therefore it contains all three types of assignments (plus, minus and zero). As it is an union each assignment
        // will be processed only once. Inside the loop we determine whether it was added, deleted or remains unchanged.
        // This is a first step of the processing. It takes all the account constructions regardless of the resource and
        // account type (intent). Therefore several constructions for the same resource and intent may appear in the resulting
        // sets. This is not good as we want only a single account for each resource/intent combination. But that will be
        // sorted out later.
        DeltaSetTriple<EvaluatedAssignmentImpl<F>> evaluatedAssignmentTriple = new DeltaSetTriple<>();
        for (SmartAssignmentElement assignmentElement : assignmentCollection) {
        	processAssignment(evaluatedAssignmentTriple, focusDelta, assignmentDelta, assignmentElement);
        }

        return evaluatedAssignmentTriple;
	}

    private void processAssignment(DeltaSetTriple<EvaluatedAssignmentImpl<F>> evaluatedAssignmentTriple,
    		ObjectDelta<F> focusDelta, ContainerDelta<AssignmentType> assignmentDelta, SmartAssignmentElement assignmentElement)
    				throws SchemaException, ExpressionEvaluationException, PolicyViolationException, SecurityViolationException, ConfigurationException, CommunicationException {

		final LensFocusContext<F> focusContext = context.getFocusContext();
    	final PrismContainerValue<AssignmentType> assignmentCVal = assignmentElement.getAssignmentCVal();
        final PrismContainerValue<AssignmentType> assignmentCValOld = assignmentCVal;
        PrismContainerValue<AssignmentType> assignmentCValNew = assignmentCVal;		// refined later

		final boolean presentInCurrent = assignmentElement.isCurrent();
		final boolean presentInOld = assignmentElement.isOld();
        // This really means whether the WHOLE assignment was changed (e.g. added/deleted/replaced). It tells nothing
        // about "micro-changes" inside assignment, these will be processed later.
        boolean isAssignmentChanged = assignmentElement.isChanged();			// refined later
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
            	PrismContainer<AssignmentType> assContNew = focusContext.getObjectNew().findContainer(FocusType.F_ASSIGNMENT);
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
        	EvaluatedAssignmentImpl<F> evaluatedAssignment = evaluateAssignment(createAssignmentIdiDelete(assignmentCVal), PlusMinusZero.MINUS, false, context, source, assignmentEvaluator, assignmentPlacementDesc, task, result);
            if (evaluatedAssignment == null) {
            	return;
            }
			evaluatedAssignment.setPresentInCurrentObject(presentInCurrent);
			evaluatedAssignment.setPresentInOldObject(presentInOld);
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
        		boolean hadValue = presentInCurrent;
        		boolean willHaveValue = assignmentDelta.isValueToReplace(assignmentCVal, true);
        		if (hadValue && willHaveValue) {
        			// No change
        			EvaluatedAssignmentImpl<F> evaluatedAssignment = evaluateAssignment(createAssignmentIdiNoChange(assignmentCVal), PlusMinusZero.ZERO, false, context, source, assignmentEvaluator, assignmentPlacementDesc, task, result);
                    if (evaluatedAssignment == null) {
                    	return;
                    }
					evaluatedAssignment.setPresentInCurrentObject(presentInCurrent);
					evaluatedAssignment.setPresentInOldObject(presentInOld);
					evaluatedAssignment.setWasValid(evaluatedAssignment.isValid());
					collectToZero(evaluatedAssignmentTriple, evaluatedAssignment, forceRecon);
        		} else if (willHaveValue) {
        			// add
        			EvaluatedAssignmentImpl<F> evaluatedAssignment = evaluateAssignment(createAssignmentIdiAdd(assignmentCVal), PlusMinusZero.PLUS, false, context, source, assignmentEvaluator, assignmentPlacementDesc, task, result);
                    if (evaluatedAssignment == null) {
                    	return;
                    }
					evaluatedAssignment.setPresentInCurrentObject(presentInCurrent);
					evaluatedAssignment.setPresentInOldObject(presentInOld);
					evaluatedAssignment.setWasValid(false);
                    collectToPlus(evaluatedAssignmentTriple, evaluatedAssignment, forceRecon);
        		} else if (hadValue) {
        			// delete
        			EvaluatedAssignmentImpl<F> evaluatedAssignment = evaluateAssignment(createAssignmentIdiDelete(assignmentCVal), PlusMinusZero.MINUS, true, context, source, assignmentEvaluator, assignmentPlacementDesc, task, result);
                    if (evaluatedAssignment == null) {
                    	return;
                    }
					evaluatedAssignment.setPresentInCurrentObject(presentInCurrent);
					evaluatedAssignment.setPresentInOldObject(presentInOld);
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
	                	if (presentInCurrent && presentInOld) {
	                		// Phantom add: adding assignment that is already there
	                		if (LOGGER.isTraceEnabled()) {
			            		LOGGER.trace("Processing changed assignment, phantom add: {}", SchemaDebugUtil.prettyPrint(assignmentCVal));
			            	}
	                		EvaluatedAssignmentImpl<F> evaluatedAssignment = evaluateAssignment(createAssignmentIdiNoChange(assignmentCVal), PlusMinusZero.ZERO, false, context, source, assignmentEvaluator, assignmentPlacementDesc, task, result);
	                        if (evaluatedAssignment == null) {
	                        	return;
	                        }
							evaluatedAssignment.setPresentInCurrentObject(presentInCurrent);
							evaluatedAssignment.setPresentInOldObject(presentInOld);
							evaluatedAssignment.setWasValid(evaluatedAssignment.isValid());
							collectToZero(evaluatedAssignmentTriple, evaluatedAssignment, forceRecon);
	                	} else {
	                		if (LOGGER.isTraceEnabled()) {
			            		LOGGER.trace("Processing changed assignment, add: {}", SchemaDebugUtil.prettyPrint(assignmentCVal));
			            	}
	                		EvaluatedAssignmentImpl<F> evaluatedAssignment = evaluateAssignment(createAssignmentIdiAdd(assignmentCVal), PlusMinusZero.PLUS, false, context, source, assignmentEvaluator, assignmentPlacementDesc, task, result);
	                        if (evaluatedAssignment == null) {
	                        	return;
	                        }
							evaluatedAssignment.setPresentInCurrentObject(presentInCurrent);
							evaluatedAssignment.setPresentInOldObject(presentInOld);
							evaluatedAssignment.setWasValid(false);
		                    collectToPlus(evaluatedAssignmentTriple, evaluatedAssignment, forceRecon);
	                	}

	                } else if (isDelete && !isAdd) {
	                	// Existing assignment is removed
	                	if (LOGGER.isTraceEnabled()) {
		            		LOGGER.trace("Processing changed assignment, delete: {}", SchemaDebugUtil.prettyPrint(assignmentCVal));
		            	}
	                	EvaluatedAssignmentImpl<F> evaluatedAssignment = evaluateAssignment(createAssignmentIdiDelete(assignmentCVal), PlusMinusZero.MINUS, true, context, source, assignmentEvaluator, assignmentPlacementDesc, task, result);
	                    if (evaluatedAssignment == null) {
	                    	return;
	                    }
						evaluatedAssignment.setPresentInCurrentObject(presentInCurrent);
						evaluatedAssignment.setPresentInOldObject(presentInOld);
						evaluatedAssignment.setWasValid(evaluatedAssignment.isValid());
	                    collectToMinus(evaluatedAssignmentTriple, evaluatedAssignment, forceRecon);

	                } else {
	                	// Small change inside an assignment
	                	// The only thing that we need to worry about is assignment validity change. That is a cause
	                	// of provisioning/deprovisioning of the projections. So check that explicitly. Other changes are
	                	// not significant, i.e. reconciliation can handle them.
	                	boolean isValidOld = LensUtil.isAssignmentValid(focusContext.getObjectOld().asObjectable(),
	                			assignmentCValOld.asContainerable(), now, activationComputer);
	                	boolean isValid = LensUtil.isAssignmentValid(focusContext.getObjectNew().asObjectable(),
	                			assignmentCValNew.asContainerable(), now, activationComputer);
						ItemDeltaItem<PrismContainerValue<AssignmentType>, PrismContainerDefinition<AssignmentType>> assignmentIdi =
								createAssignmentIdiInternalChange(assignmentCVal, subItemDeltas);
	                	if (isValid == isValidOld) {
	                		// No change in validity -> right to the zero set
		                	// The change is not significant for assignment applicability. Recon will sort out the details.
	                		if (LOGGER.isTraceEnabled()) {
			            		LOGGER.trace("Processing changed assignment, minor change (add={}, delete={}, valid={}): {}",
										isAdd, isDelete, isValid, SchemaDebugUtil.prettyPrint(assignmentCVal));
			            	}
	                		EvaluatedAssignmentImpl<F> evaluatedAssignment = evaluateAssignment(assignmentIdi, PlusMinusZero.ZERO, false, context, source, assignmentEvaluator, assignmentPlacementDesc, task, result);
	                        if (evaluatedAssignment == null) {
	                        	return;
	                        }
							evaluatedAssignment.setPresentInCurrentObject(presentInCurrent);
							evaluatedAssignment.setPresentInOldObject(presentInOld);
							evaluatedAssignment.setWasValid(evaluatedAssignment.isValid());
			                collectToZero(evaluatedAssignmentTriple, evaluatedAssignment, true);
	                	} else if (isValid) {
	                		// Assignment became valid. We need to place it in plus set to initiate provisioning
	                		if (LOGGER.isTraceEnabled()) {
			            		LOGGER.trace("Processing changed assignment, assignment becomes valid (add={}, delete={}): {}",
										isAdd, isDelete, SchemaDebugUtil.prettyPrint(assignmentCVal));
			            	}
	                		EvaluatedAssignmentImpl<F> evaluatedAssignment = evaluateAssignment(assignmentIdi, PlusMinusZero.PLUS, false, context, source, assignmentEvaluator, assignmentPlacementDesc, task, result);
	                        if (evaluatedAssignment == null) {
	                        	return;
	                        }
							evaluatedAssignment.setPresentInCurrentObject(presentInCurrent);
							evaluatedAssignment.setPresentInOldObject(presentInOld);
							evaluatedAssignment.setWasValid(false);
		                    collectToPlus(evaluatedAssignmentTriple, evaluatedAssignment, true);
	                	} else {
	                		// Assignment became invalid. We need to place is in minus set to initiate deprovisioning
	                		if (LOGGER.isTraceEnabled()) {
			            		LOGGER.trace("Processing changed assignment, assignment becomes invalid (add={}, delete={}): {}",
										isAdd, isDelete, SchemaDebugUtil.prettyPrint(assignmentCVal));
			            	}
	                		EvaluatedAssignmentImpl<F> evaluatedAssignment = evaluateAssignment(assignmentIdi, PlusMinusZero.MINUS, false, context, source, assignmentEvaluator, assignmentPlacementDesc, task, result);
	                        if (evaluatedAssignment == null) {
	                        	return;
	                        }
							evaluatedAssignment.setPresentInCurrentObject(presentInCurrent);
							evaluatedAssignment.setPresentInOldObject(presentInOld);
							evaluatedAssignment.setWasValid(true);
	                        collectToMinus(evaluatedAssignmentTriple, evaluatedAssignment, true);
	                	}
	                }

	            } else {
	                // No change in assignment
	            	if (LOGGER.isTraceEnabled()) {
	            		LOGGER.trace("Processing unchanged assignment {}", SchemaDebugUtil.prettyPrint(assignmentCVal));
	            	}
	            	EvaluatedAssignmentImpl<F> evaluatedAssignment = evaluateAssignment(createAssignmentIdiNoChange(assignmentCVal), PlusMinusZero.ZERO, false, context, source, assignmentEvaluator, assignmentPlacementDesc, task, result);
	                if (evaluatedAssignment == null) {
	                	return;
	                }
					evaluatedAssignment.setPresentInCurrentObject(presentInCurrent);
					evaluatedAssignment.setPresentInOldObject(presentInOld);
					evaluatedAssignment.setWasValid(evaluatedAssignment.isValid());
	                collectToZero(evaluatedAssignmentTriple, evaluatedAssignment, forceRecon);
	            }
        	}
        }
    }

	private ItemDeltaItem<PrismContainerValue<AssignmentType>, PrismContainerDefinition<AssignmentType>> createAssignmentIdiNoChange(
			PrismContainerValue<AssignmentType> cval) throws SchemaException {
		ItemDeltaItem<PrismContainerValue<AssignmentType>, PrismContainerDefinition<AssignmentType>> idi = new ItemDeltaItem<>();
		idi.setItemOld(LensUtil.createAssignmentSingleValueContainerClone(cval.asContainerable()));
		idi.recompute();
		return idi;
	}

	private ItemDeltaItem<PrismContainerValue<AssignmentType>, PrismContainerDefinition<AssignmentType>> createAssignmentIdiAdd(
			PrismContainerValue<AssignmentType> cval) throws SchemaException {
		ItemDeltaItem<PrismContainerValue<AssignmentType>, PrismContainerDefinition<AssignmentType>> idi = new ItemDeltaItem<>();
		idi.setItemOld(null);
		@SuppressWarnings({"unchecked", "raw"})
		ItemDelta<PrismContainerValue<AssignmentType>, PrismContainerDefinition<AssignmentType>> itemDelta = (ItemDelta<PrismContainerValue<AssignmentType>, PrismContainerDefinition<AssignmentType>>)
				getDeltaItemFragment(cval)
						.add(cval.asContainerable().clone())
						.asItemDelta();
		idi.setDelta(itemDelta);
		idi.recompute();
		return idi;
	}

	private S_ValuesEntry getDeltaItemFragment(PrismContainerValue<AssignmentType> cval) throws SchemaException {
		PrismContainerDefinition<AssignmentType> definition = cval.getParent() != null ? cval.getParent().getDefinition() : null;
		if (definition == null) {
			// we use custom definition, if available; if not, we find the standard one
			definition = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(FocusType.class)
					.findItemDefinition(FocusType.F_ASSIGNMENT);
		}
		definition = definition.clone();
		definition.setMaxOccurs(1);
		return DeltaBuilder.deltaFor(FocusType.class, prismContext)
				.item(new ItemPath(FocusType.F_ASSIGNMENT), definition);
	}

	private ItemDeltaItem<PrismContainerValue<AssignmentType>, PrismContainerDefinition<AssignmentType>> createAssignmentIdiDelete(
			PrismContainerValue<AssignmentType> cval) throws SchemaException {
		ItemDeltaItem<PrismContainerValue<AssignmentType>, PrismContainerDefinition<AssignmentType>> idi = new ItemDeltaItem<>();
		idi.setItemOld(LensUtil.createAssignmentSingleValueContainerClone(cval.asContainerable()));
		@SuppressWarnings({"unchecked", "raw"})
		ItemDelta<PrismContainerValue<AssignmentType>, PrismContainerDefinition<AssignmentType>> itemDelta = (ItemDelta<PrismContainerValue<AssignmentType>, PrismContainerDefinition<AssignmentType>>)
				getDeltaItemFragment(cval)
						.delete(cval.asContainerable().clone())
						.asItemDelta();
		idi.setDelta(itemDelta);
		idi.recompute();
		return idi;
	}

	private ItemDeltaItem<PrismContainerValue<AssignmentType>, PrismContainerDefinition<AssignmentType>> createAssignmentIdiInternalChange(
			PrismContainerValue<AssignmentType> cval, Collection<? extends ItemDelta<?, ?>> subItemDeltas)
			throws SchemaException {
		ItemDeltaItem<PrismContainerValue<AssignmentType>, PrismContainerDefinition<AssignmentType>> idi = new ItemDeltaItem<>();
		idi.setItemOld(LensUtil.createAssignmentSingleValueContainerClone(cval.asContainerable()));
		idi.setSubItemDeltas(subItemDeltas);
		idi.recompute();
		return idi;
	}

	private <F extends FocusType> Collection<? extends ItemDelta<?,?>> getExecutionWaveAssignmentItemDeltas(LensFocusContext<F> focusContext, Long id) throws SchemaException {
        ObjectDelta<? extends FocusType> focusDelta = focusContext.getWaveDelta(focusContext.getLensContext().getExecutionWave());
        if (focusDelta == null) {
            return null;
        }
        return focusDelta.findItemDeltasSubPath(new ItemPath(new NameItemPathSegment(FocusType.F_ASSIGNMENT),
        									  new IdItemPathSegment(id)));
	}

	private <F extends FocusType> void collectToZero(DeltaSetTriple<EvaluatedAssignmentImpl<F>> evaluatedAssignmentTriple,
    		EvaluatedAssignmentImpl<F> evaluatedAssignment, boolean forceRecon) {
        if (forceRecon) {
            evaluatedAssignment.setForceRecon(true);
        }
    	evaluatedAssignmentTriple.addToZeroSet(evaluatedAssignment);
    }

    private <F extends FocusType> void collectToPlus(DeltaSetTriple<EvaluatedAssignmentImpl<F>> evaluatedAssignmentTriple,
    		EvaluatedAssignmentImpl<F> evaluatedAssignment, boolean forceRecon) {
        if (forceRecon) {
            evaluatedAssignment.setForceRecon(true);
        }
    	evaluatedAssignmentTriple.addToPlusSet(evaluatedAssignment);
    }

    private <F extends FocusType> void collectToMinus(DeltaSetTriple<EvaluatedAssignmentImpl<F>> evaluatedAssignmentTriple,
    		EvaluatedAssignmentImpl<F> evaluatedAssignment, boolean forceRecon) {
        if (forceRecon) {
            evaluatedAssignment.setForceRecon(true);
        }
    	evaluatedAssignmentTriple.addToMinusSet(evaluatedAssignment);
    }

    private <F extends FocusType> EvaluatedAssignmentImpl<F> evaluateAssignment(ItemDeltaItem<PrismContainerValue<AssignmentType>,PrismContainerDefinition<AssignmentType>> assignmentIdi,
    		PlusMinusZero mode, boolean evaluateOld, LensContext<F> context, ObjectType source, AssignmentEvaluator<F> assignmentEvaluator,
			String assignmentPlacementDesc, Task task, OperationResult parentResult) throws SchemaException, ExpressionEvaluationException, PolicyViolationException, SecurityViolationException, ConfigurationException, CommunicationException {
		OperationResult result = parentResult.createMinorSubresult(AssignmentProcessor.class.getSimpleName()+".evaluateAssignment");
		result.addParam("assignmentDescription", assignmentPlacementDesc);
        try {
			// Evaluate assignment. This follows to the assignment targets, follows to the inducements,
        	// evaluates all the expressions, etc.
        	EvaluatedAssignmentImpl<F> evaluatedAssignment = assignmentEvaluator.evaluate(assignmentIdi, mode, evaluateOld, source, assignmentPlacementDesc, task, result);
        	context.rememberResources(evaluatedAssignment.getResources(task, result));
        	result.recordSuccess();
        	if (LOGGER.isTraceEnabled()) {
        		LOGGER.trace("Evaluated assignment:\n{}", evaluatedAssignment == null ? null : evaluatedAssignment.debugDump(1));
        	}
        	return evaluatedAssignment;
        } catch (ObjectNotFoundException ex) {
        	if (LOGGER.isTraceEnabled()) {
        		LOGGER.trace("Processing of assignment resulted in error {}: {}", ex, SchemaDebugUtil.prettyPrint(LensUtil.getAssignmentType(assignmentIdi, evaluateOld)));
            }
        	if (ModelExecuteOptions.isForce(context.getOptions())) {
        		result.recordHandledError(ex);
        		return null;
        	}
        	ModelUtils.recordFatalError(result, ex);
        	return null;
        } catch (SchemaException ex) {
        	AssignmentType assignmentType = LensUtil.getAssignmentType(assignmentIdi, evaluateOld);
        	if (LOGGER.isTraceEnabled()) {
        		LOGGER.trace("Processing of assignment resulted in error {}: {}", ex, SchemaDebugUtil.prettyPrint(assignmentType));
            }
        	ModelUtils.recordFatalError(result, ex);
        	String resourceOid = FocusTypeUtil.determineConstructionResource(assignmentType);
        	if (resourceOid == null) {
        		// This is a role assignment or something like that. Just throw the original exception for now.
        		throw ex;
        	}
        	ResourceShadowDiscriminator rad = new ResourceShadowDiscriminator(resourceOid,
        			FocusTypeUtil.determineConstructionKind(assignmentType),
        			FocusTypeUtil.determineConstructionIntent(assignmentType));
			LensProjectionContext accCtx = context.findProjectionContext(rad);
			if (accCtx != null) {
				accCtx.setSynchronizationPolicyDecision(SynchronizationPolicyDecision.BROKEN);
			}
        	return null;
        } catch (ExpressionEvaluationException | PolicyViolationException | SecurityViolationException | ConfigurationException | CommunicationException  e) {
        	AssignmentType assignmentType = LensUtil.getAssignmentType(assignmentIdi, evaluateOld);
        	if (LOGGER.isTraceEnabled()) {
        		LOGGER.trace("Processing of assignment resulted in error {}: {}", e, SchemaDebugUtil.prettyPrint(assignmentType));
            }
        	result.recordFatalError(e);
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
	private <F extends FocusType> ContainerDelta<AssignmentType> getExecutionWaveAssignmentDelta(LensFocusContext<F> focusContext) throws SchemaException {
        ObjectDelta<? extends FocusType> focusDelta = focusContext.getAggregatedWaveDelta(focusContext.getLensContext().getExecutionWave());
        if (focusDelta == null) {
            return createEmptyAssignmentDelta(focusContext);
        }
        ContainerDelta<AssignmentType> assignmentDelta = focusDelta.findContainerDelta(new ItemPath(FocusType.F_ASSIGNMENT));
        if (assignmentDelta == null) {
            return createEmptyAssignmentDelta(focusContext);
        }
        return assignmentDelta;
    }

	private <F extends FocusType> ContainerDelta<AssignmentType> createEmptyAssignmentDelta(LensFocusContext<F> focusContext) {
        return new ContainerDelta<>(getAssignmentContainerDefinition(focusContext), prismContext);
    }

	private <F extends FocusType> PrismContainerDefinition<AssignmentType> getAssignmentContainerDefinition(LensFocusContext<F> focusContext) {
		return focusContext.getObjectDefinition().findContainerDefinition(FocusType.F_ASSIGNMENT);
	}

}
