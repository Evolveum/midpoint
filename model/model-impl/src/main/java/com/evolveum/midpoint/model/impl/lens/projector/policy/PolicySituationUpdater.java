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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.Objects;

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

//	private List<EvaluatedPolicyRuleTriggerType> getTriggers(EvaluatedAssignmentImpl<?> evaluatedAssignment) {
//		List<EvaluatedPolicyRuleTriggerType> rv = new ArrayList<>();
//		for (EvaluatedPolicyRule policyRule : evaluatedAssignment.getAllTargetsPolicyRules()) {
//			for (EvaluatedPolicyRuleTrigger<?> trigger : policyRule.getTriggers()) {
//				EvaluatedPolicyRuleTriggerType triggerType = trigger.toEvaluatedPolicyRuleTriggerType(policyRule, false).clone();
//				simplifyTrigger(triggerType);
//				rv.add(triggerType);
//			}
//		}
//		return rv;
//	}
//
//	private void simplifyTrigger(EvaluatedPolicyRuleTriggerType trigger) {
//		deleteAssignments(trigger.getAssignmentPath());
//		if (trigger instanceof EvaluatedExclusionTriggerType) {
//			EvaluatedExclusionTriggerType exclusionTrigger = (EvaluatedExclusionTriggerType) trigger;
//			deleteAssignments(exclusionTrigger.getConflictingObjectPath());
//			exclusionTrigger.setConflictingAssignment(null);
//		} else if (trigger instanceof EvaluatedSituationTriggerType) {
//			for (EvaluatedPolicyRuleType sourceRule : ((EvaluatedSituationTriggerType) trigger).getSourceRule()) {
//				for (EvaluatedPolicyRuleTriggerType sourceTrigger : sourceRule.getTrigger()) {
//					simplifyTrigger(sourceTrigger);
//				}
//			}
//		}
//	}

//	private void deleteAssignments(AssignmentPathType path) {
//		if (path == null) {
//			return;
//		}
//		for (AssignmentPathSegmentType segment : path.getSegment()) {
//			segment.setAssignment(null);
//		}
//	}

//	private <F extends FocusType> boolean shouldSituationBeUpdated(EvaluatedAssignment<F> evaluatedAssignment,
//			List<EvaluatedPolicyRuleTriggerType> triggers) {
//		if (PolicyRuleTypeUtil.canBePacked(evaluatedAssignment.getAssignmentType().getTrigger())) {
//			return true;
//		}
//		Set<String> currentSituations = new HashSet<>(evaluatedAssignment.getAssignmentType().getPolicySituation());
//		List<EvaluatedPolicyRuleTriggerType> currentTriggersUnpacked = PolicyRuleTypeUtil.unpack(evaluatedAssignment.getAssignmentType().getTrigger());
//		// if the current situations different from the ones in the old assignment => update
//		// (provided that the situations in the assignment were _not_ changed directly via a delta!!!) TODO check this
//		if (!currentSituations.equals(new HashSet<>(evaluatedAssignment.getPolicySituations()))) {
//			LOGGER.trace("computed policy situations are different from the current ones");
//			return true;
//		}
//		if (!PolicyRuleTypeUtil.triggerCollectionsEqual(triggers, currentTriggersUnpacked)) {
//			LOGGER.trace("computed policy rules triggers are different from the current ones");
//			return true;
//		}
//		return false;
//	}


	public <F extends FocusType> void applyObjectSituation(LensContext<F> context, List<EvaluatedPolicyRule> rulesToRecord) throws SchemaException {
		// compute policySituation and triggeredPolicyRules and compare it with the expected state
		// note that we use the new state for the comparison, because if values match we do not need to do anything
		LensFocusContext<F> focusContext = context.getFocusContext();
		F objectNew = focusContext.getObjectNew().asObjectable();
		ComputationResult cr = compute(rulesToRecord, objectNew.getPolicySituation(), objectNew.getTriggeredPolicyRule());
		if (cr.situationsNeedUpdate) {
			focusContext.addToPendingPolicySituationModifications(DeltaBuilder.deltaFor(ObjectType.class, prismContext)
					.item(ObjectType.F_POLICY_SITUATION)
							.oldRealValues(cr.oldPolicySituations)
							.replaceRealValues(cr.newPolicySituations)
					.asItemDelta());
		}
		if (cr.rulesNeedUpdate) {
			focusContext.addToPendingPolicySituationModifications(DeltaBuilder.deltaFor(ObjectType.class, prismContext)
					.item(ObjectType.F_TRIGGERED_POLICY_RULE)
							.oldRealValues(cr.oldTriggeredRules)
							.replaceRealValues(cr.newTriggeredRules)
					.asItemDelta());
		}
	}

	public <F extends FocusType> void applyAssignmentSituation(LensContext<F> context,
			EvaluatedAssignmentImpl<F> evaluatedAssignment, List<EvaluatedPolicyRule> rulesToRecord) throws SchemaException {
		LensFocusContext<F> focusContext = context.getFocusContext();
		AssignmentType assignment = evaluatedAssignment.getAssignmentType(false);
		Long id = assignment.getId();
		ComputationResult cr = compute(rulesToRecord, assignment.getPolicySituation(), assignment.getTriggeredPolicyRule());
		boolean missedUpdate = false;
		if (cr.situationsNeedUpdate) {
			if (id != null) {
				focusContext.addToPendingPolicySituationModifications(DeltaBuilder.deltaFor(ObjectType.class, prismContext)
						.item(FocusType.F_ASSIGNMENT, id, AssignmentType.F_POLICY_SITUATION)
								.oldRealValues(cr.oldPolicySituations)
								.replaceRealValues(cr.newPolicySituations)
						.asItemDelta());
			} else {
				missedUpdate = true;
			}
		}
		if (cr.rulesNeedUpdate) {
			if (id != null) {
				focusContext.addToPendingPolicySituationModifications(DeltaBuilder.deltaFor(ObjectType.class, prismContext)
						.item(FocusType.F_ASSIGNMENT, id, AssignmentType.F_TRIGGERED_POLICY_RULE)
								.oldRealValues(cr.oldTriggeredRules)
								.replaceRealValues(cr.newTriggeredRules)
						.asItemDelta());
			} else {
				missedUpdate = true;
			}
		}
		if (missedUpdate) {
			LOGGER.warn("Policy situation/rules for assignment cannot be updated, because it has no ID; assignment = {}, in object {}",
					assignment, focusContext.getObjectAny());
		}
	}

	private ComputationResult compute(@NotNull List<EvaluatedPolicyRule> rulesToRecord, @NotNull List<String> existingPolicySituation,
			@NotNull List<EvaluatedPolicyRuleType> existingTriggeredPolicyRule) {
		ComputationResult cr = new ComputationResult();
		for (EvaluatedPolicyRule rule : rulesToRecord) {
			cr.newPolicySituations.add(rule.getPolicySituation());
			cr.newTriggeredRules.add(rule.toEvaluatedPolicyRuleType(true));
		}
		cr.oldPolicySituations.addAll(existingPolicySituation);
		cr.oldTriggeredRules.addAll(existingTriggeredPolicyRule);
		cr.situationsNeedUpdate = !Objects.equals(cr.oldPolicySituations, cr.newPolicySituations);
		cr.rulesNeedUpdate = !Objects.equals(cr.oldTriggeredRules, cr.newTriggeredRules);
		return cr;
	}

	private class ComputationResult {
		final Set<String> oldPolicySituations = new HashSet<>();
		final Set<String> newPolicySituations = new HashSet<>();
		final Set<EvaluatedPolicyRuleType> oldTriggeredRules = new HashSet<>();
		final Set<EvaluatedPolicyRuleType> newTriggeredRules = new HashSet<>();
		boolean situationsNeedUpdate;
		boolean rulesNeedUpdate;
	}
}
