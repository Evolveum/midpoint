/*
 * Copyright (c) 2010-2017 Evolveum
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
package com.evolveum.midpoint.model.impl.lens.projector.policy;

import com.evolveum.midpoint.model.api.context.EvaluatedAssignment;
import com.evolveum.midpoint.model.api.context.EvaluatedPolicyRule;
import com.evolveum.midpoint.model.api.context.EvaluatedPolicyRuleTrigger;
import com.evolveum.midpoint.model.impl.lens.EvaluatedAssignmentImpl;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.*;
import com.evolveum.midpoint.prism.delta.builder.DeltaBuilder;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.util.PolicyRuleTypeUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Takes care of updating policySituation for focus and assignments. (Originally was a part of PolicyRuleProcessor.)
 *
 * @author semancik
 * @author mederly
 */
@Component
public class PolicySituationUpdater {

	private static final Trace LOGGER = TraceManager.getTrace(PolicySituationUpdater.class);

	@Autowired private PrismContext prismContext;

	public <O extends ObjectType> ObjectDelta<O> applyAssignmentSituation(LensContext<O> context, ObjectDelta<O> focusDelta)
			throws SchemaException {
		if (context.getFocusClass() == null || !FocusType.class.isAssignableFrom(context.getFocusClass())) {
			return focusDelta;
		}
		LensContext<? extends FocusType> contextOfFocus = (LensContext<FocusType>) context;
		if (focusDelta != null && focusDelta.isAdd()) {
			applyAssignmentSituationOnAdd(contextOfFocus, (PrismObject<? extends FocusType>) focusDelta.getObjectToAdd());
			return focusDelta;
		} else if (focusDelta == null || focusDelta.isModify()) {
			return applyAssignmentSituationOnModify(contextOfFocus, focusDelta);
		} else {
			return focusDelta;
		}
	}

	private <F extends FocusType, T extends FocusType> void applyAssignmentSituationOnAdd(LensContext<F> context,
			PrismObject<T> objectToAdd) throws SchemaException {
		if (context.getEvaluatedAssignmentTriple() == null) {
			return;
		}
		T focus = objectToAdd.asObjectable();
		for (EvaluatedAssignmentImpl<?> evaluatedAssignment : context.getEvaluatedAssignmentTriple().getNonNegativeValues()) {
			LOGGER.trace("Applying assignment situation on object ADD for {}", evaluatedAssignment);
			List<EvaluatedPolicyRuleTriggerType> triggers = getTriggers(evaluatedAssignment);
			if (!shouldSituationBeUpdated(evaluatedAssignment, triggers)) {
				continue;
			}
			AssignmentType assignment = evaluatedAssignment.getAssignmentType();
			if (assignment.getId() != null) {
				ItemDelta.applyTo(getAssignmentModificationDelta(evaluatedAssignment, triggers), objectToAdd);
			} else {
				int i = focus.getAssignment().indexOf(assignment);
				if (i < 0) {
					throw new IllegalStateException("Assignment to be replaced not found in an object to add: " + assignment + " / " + objectToAdd);
				}
				copyPolicyData(focus.getAssignment().get(i), evaluatedAssignment, triggers);
			}
		}
	}

	private <F extends FocusType, T extends ObjectType> ObjectDelta<T> applyAssignmentSituationOnModify(LensContext<F> context,
			ObjectDelta<T> objectDelta) throws SchemaException {
		if (context.getEvaluatedAssignmentTriple() == null) {
			return objectDelta;
		}
		for (EvaluatedAssignmentImpl<?> evaluatedAssignment : context.getEvaluatedAssignmentTriple().getNonNegativeValues()) {
			LOGGER.trace("Applying assignment situation on object MODIFY for {}", evaluatedAssignment);
			List<EvaluatedPolicyRuleTriggerType> triggers = getTriggers(evaluatedAssignment);
			if (!shouldSituationBeUpdated(evaluatedAssignment, triggers)) {
				continue;
			}
			AssignmentType assignment = evaluatedAssignment.getAssignmentType();
			if (assignment.getId() != null) {
				objectDelta = swallow(context, objectDelta, getAssignmentModificationDelta(evaluatedAssignment, triggers));
			} else {
				if (objectDelta == null) {
					throw new IllegalStateException("No object delta!");
				}
				ContainerDelta<Containerable> assignmentDelta = objectDelta.findContainerDelta(FocusType.F_ASSIGNMENT);
				if (assignmentDelta == null) {
					throw new IllegalStateException("Unnumbered assignment (" + assignment
							+ ") couldn't be found in object delta (no assignment modification): " + objectDelta);
				}
				PrismContainerValue<AssignmentType> assignmentInDelta = assignmentDelta.findValueToAddOrReplace(assignment.asPrismContainerValue());
				if (assignmentInDelta == null) {
					throw new IllegalStateException("Unnumbered assignment (" + assignment
							+ ") couldn't be found in object delta (no corresponding assignment value): " + objectDelta);
				}
				copyPolicyData(assignmentInDelta.asContainerable(), evaluatedAssignment, triggers);
			}
		}
		return objectDelta;
	}

	@NotNull
	private <F extends FocusType> List<ItemDelta<?, ?>> getAssignmentModificationDelta(
			EvaluatedAssignmentImpl<F> evaluatedAssignment, List<EvaluatedPolicyRuleTriggerType> triggers) throws SchemaException {
		Long id = evaluatedAssignment.getAssignmentType().getId();
		if (id == null) {
			throw new IllegalArgumentException("Assignment with no ID: " + evaluatedAssignment);
		}
		List<ItemDelta<?, ?>> deltas = new ArrayList<>();
		Set<String> currentSituations = new HashSet<>(evaluatedAssignment.getAssignmentType().getPolicySituation());
		Set<String> newSituations = new HashSet<>(evaluatedAssignment.getPolicySituations());
		CollectionUtils.addIgnoreNull(deltas, createSituationDelta(
				new ItemPath(FocusType.F_ASSIGNMENT, id, AssignmentType.F_POLICY_SITUATION), currentSituations, newSituations));
		Set<EvaluatedPolicyRuleTriggerType> currentTriggers = new HashSet<>(evaluatedAssignment.getAssignmentType().getTrigger());
		Set<EvaluatedPolicyRuleTriggerType> newTriggers = new HashSet<>(PolicyRuleTypeUtil.pack(triggers));
		CollectionUtils.addIgnoreNull(deltas, createTriggerDelta(
				new ItemPath(FocusType.F_ASSIGNMENT, id, AssignmentType.F_TRIGGER), currentTriggers, newTriggers));
		return deltas;
	}

	private <T extends ObjectType, F extends FocusType> ObjectDelta<T> swallow(LensContext<F> context, ObjectDelta<T> objectDelta,
			List<ItemDelta<?,?>> deltas) throws SchemaException {
		if (deltas.isEmpty()) {
			return objectDelta;
		}
		if (objectDelta == null) {
			if (context.getFocusClass() == null) {
				throw new IllegalStateException("No focus class in " + context);
			}
			if (context.getFocusContext() == null) {
				throw new IllegalStateException("No focus context in " + context);
			}
			objectDelta = (ObjectDelta) new ObjectDelta<F>(context.getFocusClass(), ChangeType.MODIFY, prismContext);
			objectDelta.setOid(context.getFocusContext().getOid());
		}
		objectDelta.swallow(deltas);
		return objectDelta;
	}

	private List<EvaluatedPolicyRuleTriggerType> getTriggers(EvaluatedAssignmentImpl<?> evaluatedAssignment) {
		List<EvaluatedPolicyRuleTriggerType> rv = new ArrayList<>();
		for (EvaluatedPolicyRule policyRule : evaluatedAssignment.getAllTargetsPolicyRules()) {
			for (EvaluatedPolicyRuleTrigger<?> trigger : policyRule.getTriggers()) {
				EvaluatedPolicyRuleTriggerType triggerType = trigger.toEvaluatedPolicyRuleTriggerType(policyRule).clone();
				simplifyTrigger(triggerType);
				rv.add(triggerType);
			}
		}
		return rv;
	}

	private void simplifyTrigger(EvaluatedPolicyRuleTriggerType trigger) {
		deleteAssignments(trigger.getAssignmentPath());
		if (trigger instanceof EvaluatedExclusionTriggerType) {
			EvaluatedExclusionTriggerType exclusionTrigger = (EvaluatedExclusionTriggerType) trigger;
			deleteAssignments(exclusionTrigger.getConflictingObjectPath());
			exclusionTrigger.setConflictingAssignment(null);
		} else if (trigger instanceof EvaluatedSituationTriggerType) {
			for (EvaluatedPolicyRuleType sourceRule : ((EvaluatedSituationTriggerType) trigger).getSourceRule()) {
				for (EvaluatedPolicyRuleTriggerType sourceTrigger : sourceRule.getTrigger()) {
					simplifyTrigger(sourceTrigger);
				}
			}
		}
	}

	private void deleteAssignments(AssignmentPathType path) {
		if (path == null) {
			return;
		}
		for (AssignmentPathSegmentType segment : path.getSegment()) {
			segment.setAssignment(null);
		}
	}

	private void copyPolicyData(AssignmentType targetAssignment, EvaluatedAssignmentImpl<?> evaluatedAssignment,
			List<EvaluatedPolicyRuleTriggerType> triggers) {
		targetAssignment.getPolicySituation().clear();
		targetAssignment.getPolicySituation().addAll(evaluatedAssignment.getPolicySituations());
		targetAssignment.getTrigger().clear();
		targetAssignment.getTrigger().addAll(PolicyRuleTypeUtil.pack(triggers));
	}

	private <F extends FocusType> boolean shouldSituationBeUpdated(EvaluatedAssignment<F> evaluatedAssignment,
			List<EvaluatedPolicyRuleTriggerType> triggers) {
		if (PolicyRuleTypeUtil.canBePacked(evaluatedAssignment.getAssignmentType().getTrigger())) {
			return true;
		}
		Set<String> currentSituations = new HashSet<>(evaluatedAssignment.getAssignmentType().getPolicySituation());
		List<EvaluatedPolicyRuleTriggerType> currentTriggersUnpacked = PolicyRuleTypeUtil.unpack(evaluatedAssignment.getAssignmentType().getTrigger());
		// if the current situations different from the ones in the old assignment => update
		// (provided that the situations in the assignment were _not_ changed directly via a delta!!!) TODO check this
		if (!currentSituations.equals(new HashSet<>(evaluatedAssignment.getPolicySituations()))) {
			LOGGER.trace("computed policy situations are different from the current ones");
			return true;
		}
		if (!PolicyRuleTypeUtil.triggerCollectionsEqual(triggers, currentTriggersUnpacked)) {
			LOGGER.trace("computed policy rules triggers are different from the current ones");
			return true;
		}
		return false;
	}

	public <O extends ObjectType, F extends FocusType> void storeFocusPolicySituation(LensContext<O> context)
			throws SchemaException {
		if (context.getFocusContext() == null || context.getFocusClass() == null || !FocusType.class.isAssignableFrom(context.getFocusClass())) {
			return;
		}
		@SuppressWarnings({"raw", "unchecked"})
		LensFocusContext<F> focusContext = (LensFocusContext<F>) context.getFocusContext();
		Set<String> currentSituations = focusContext.getObjectCurrent() != null ?
				new HashSet<>(focusContext.getObjectCurrent().asObjectable().getPolicySituation()) : Collections.emptySet();
		Set<String> newSituations = new HashSet<>(focusContext.getPolicySituations());
		PropertyDelta<String> situationsDelta = createSituationDelta(
				new ItemPath(FocusType.F_POLICY_SITUATION), currentSituations, newSituations);
		if (situationsDelta != null) {
			focusContext.swallowToProjectionWaveSecondaryDelta(situationsDelta);
		}
	}

	@Nullable
	private PropertyDelta<String> createSituationDelta(ItemPath path, Set<String> currentSituations, Set<String> newSituations)
			throws SchemaException {
		if (newSituations.equals(currentSituations)) {
			return null;
		}
		@SuppressWarnings({ "unchecked", "raw" })
		PropertyDelta<String> situationsDelta = (PropertyDelta<String>) DeltaBuilder.deltaFor(FocusType.class, prismContext)
				.item(path)
				.add(CollectionUtils.subtract(newSituations, currentSituations).toArray())
				.delete(CollectionUtils.subtract(currentSituations, newSituations).toArray())
				.asItemDelta();
		situationsDelta.setEstimatedOldValues(PrismPropertyValue.wrap(currentSituations));
		return situationsDelta;
	}

	private PropertyDelta<EvaluatedPolicyRuleTriggerType> createTriggerDelta(ItemPath path, Set<EvaluatedPolicyRuleTriggerType> currentTriggers, Set<EvaluatedPolicyRuleTriggerType> newTriggers)
			throws SchemaException {
		if (newTriggers.equals(currentTriggers)) {
			return null;
		}
		@SuppressWarnings({ "unchecked", "raw" })
		PropertyDelta<EvaluatedPolicyRuleTriggerType> triggersDelta = (PropertyDelta<EvaluatedPolicyRuleTriggerType>)
				DeltaBuilder.deltaFor(FocusType.class, prismContext)
						.item(path)
						.replace(newTriggers.toArray())			// TODO or add + delete?
						.asItemDelta();
		triggersDelta.setEstimatedOldValues(PrismPropertyValue.wrap(currentTriggers));
		return triggersDelta;
	}

}
