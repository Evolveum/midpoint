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

import com.evolveum.midpoint.model.api.context.EvaluatedPolicyRule;
import com.evolveum.midpoint.model.api.context.PolicyRuleExternalizationOptions;
import com.evolveum.midpoint.model.impl.lens.EvaluatedAssignmentImpl;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.PlusMinusZero;
import com.evolveum.midpoint.prism.delta.builder.DeltaBuilder;
import com.evolveum.midpoint.prism.path.IdItemPathSegment;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.Objects;

/**
 * Takes care of updating policySituation and triggered rules for focus and assignments. (Originally was a part of PolicyRuleProcessor.)
 *
 * @author semancik
 * @author mederly
 */
@Component
public class PolicyStateRecorder {

	private static final Trace LOGGER = TraceManager.getTrace(PolicyStateRecorder.class);

	@Autowired private PrismContext prismContext;

	public <F extends FocusType> void applyObjectState(LensContext<F> context, List<EvaluatedPolicyRule> rulesToRecord) throws SchemaException {
		// compute policySituation and triggeredPolicyRules and compare it with the expected state
		// note that we use the new state for the comparison, because if values match we do not need to do anything
		LensFocusContext<F> focusContext = context.getFocusContext();
		if (focusContext.isDelete()) {
			return;
		}
		F objectNew = focusContext.getObjectNew().asObjectable();
		ComputationResult cr = compute(rulesToRecord, objectNew.getPolicySituation(), objectNew.getTriggeredPolicyRule());
		if (cr.situationsNeedUpdate) {
			focusContext.addToPendingObjectPolicyStateModifications(DeltaBuilder.deltaFor(ObjectType.class, prismContext)
					.item(ObjectType.F_POLICY_SITUATION)
							.oldRealValues(cr.oldPolicySituations)
							.replaceRealValues(cr.newPolicySituations)
					.asItemDelta());
		}
		if (cr.rulesNeedUpdate) {
			focusContext.addToPendingObjectPolicyStateModifications(DeltaBuilder.deltaFor(ObjectType.class, prismContext)
					.item(ObjectType.F_TRIGGERED_POLICY_RULE)
							.oldRealValues(cr.oldTriggeredRules)
							.replaceRealValues(cr.newTriggeredRules)
					.asItemDelta());
		}
	}

	public <F extends FocusType> void applyAssignmentState(LensContext<F> context,
			EvaluatedAssignmentImpl<F> evaluatedAssignment, PlusMinusZero mode, List<EvaluatedPolicyRule> rulesToRecord) throws SchemaException {
		LensFocusContext<F> focusContext = context.getFocusContext();
		if (focusContext.isDelete()) {
			return;
		}
		AssignmentType assignmentNew = evaluatedAssignment.getAssignmentType(false);
		AssignmentType assignmentOld = evaluatedAssignment.getAssignmentType(true);
		if (assignmentOld == null && assignmentNew == null) {
			throw new IllegalStateException("Policy situation/rules for assignment cannot be updated, because the "
					+ "assignment itself is missing in "+evaluatedAssignment+", in object "+focusContext.getObjectAny());
		}
		// this value is to be used to find correct assignment in objectDelta to apply the modifications (if no ID is present)
		@NotNull AssignmentType assignmentToMatch = assignmentOld != null ? assignmentOld : assignmentNew;
		// this value is used to compute policy situation/rules modifications
		@NotNull AssignmentType assignmentToCompute = assignmentNew != null ? assignmentNew : assignmentOld;

		Long id = assignmentToMatch.getId();
		ComputationResult cr = compute(rulesToRecord, assignmentToCompute.getPolicySituation(), assignmentToCompute.getTriggeredPolicyRule());
		if (cr.situationsNeedUpdate) {
			focusContext.addToPendingAssignmentPolicyStateModifications(assignmentToMatch,
					mode, DeltaBuilder.deltaFor(FocusType.class, prismContext)
					.item(FocusType.F_ASSIGNMENT, new IdItemPathSegment(id), AssignmentType.F_POLICY_SITUATION)
					.oldRealValues(cr.oldPolicySituations)
					.replaceRealValues(cr.newPolicySituations)
					.asItemDelta());
		}
		if (cr.rulesNeedUpdate) {
			focusContext.addToPendingAssignmentPolicyStateModifications(assignmentToMatch,
					mode, DeltaBuilder.deltaFor(FocusType.class, prismContext)
					.item(FocusType.F_ASSIGNMENT, new IdItemPathSegment(id), AssignmentType.F_TRIGGERED_POLICY_RULE)
					.oldRealValues(cr.oldTriggeredRules)
					.replaceRealValues(cr.newTriggeredRules)
					.asItemDelta());
		}
	}

	private ComputationResult compute(@NotNull List<EvaluatedPolicyRule> rulesToRecord, @NotNull List<String> existingPolicySituation,
			@NotNull List<EvaluatedPolicyRuleType> existingTriggeredPolicyRule) {
		ComputationResult cr = new ComputationResult();
		for (EvaluatedPolicyRule rule : rulesToRecord) {
			cr.newPolicySituations.add(rule.getPolicySituation());
			RecordPolicyActionType recordAction = rule.getEnabledAction(RecordPolicyActionType.class);
			if (recordAction.getPolicyRules() != TriggeredPolicyRulesStorageStrategyType.NONE) {
				PolicyRuleExternalizationOptions externalizationOptions = new PolicyRuleExternalizationOptions(
						recordAction.getPolicyRules(), false, true);
				rule.addToEvaluatedPolicyRuleTypes(cr.newTriggeredRules, externalizationOptions);
			}
		}
		cr.oldPolicySituations.addAll(existingPolicySituation);
		cr.oldTriggeredRules.addAll(existingTriggeredPolicyRule);
		cr.situationsNeedUpdate = !Objects.equals(cr.oldPolicySituations, cr.newPolicySituations);
		cr.rulesNeedUpdate = !Objects.equals(cr.oldTriggeredRules, cr.newTriggeredRules);   // hope hashCode is computed well
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
