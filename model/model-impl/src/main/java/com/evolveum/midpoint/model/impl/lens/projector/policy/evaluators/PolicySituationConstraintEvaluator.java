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

package com.evolveum.midpoint.model.impl.lens.projector.policy.evaluators;

import com.evolveum.midpoint.model.api.context.EvaluatedPolicyRule;
import com.evolveum.midpoint.model.api.context.EvaluatedPolicyRuleTrigger;
import com.evolveum.midpoint.model.api.context.EvaluatedSituationTrigger;
import com.evolveum.midpoint.model.impl.lens.EvaluatedAssignmentImpl;
import com.evolveum.midpoint.model.impl.lens.projector.policy.AssignmentPolicyRuleEvaluationContext;
import com.evolveum.midpoint.model.impl.lens.projector.policy.PolicyRuleEvaluationContext;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.LocalizableMessageBuilder;
import com.evolveum.midpoint.util.TreeNode;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractPolicyConstraintType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicySituationPolicyConstraintType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.bind.JAXBElement;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author semancik
 * @author mederly
 */
@Component
public class PolicySituationConstraintEvaluator implements PolicyConstraintEvaluator<PolicySituationPolicyConstraintType> {

	private static final String CONSTRAINT_KEY = "situation";

	@Autowired private ConstraintEvaluatorHelper evaluatorHelper;

	@Override
	public <F extends FocusType> EvaluatedPolicyRuleTrigger evaluate(JAXBElement<PolicySituationPolicyConstraintType> constraint,
			PolicyRuleEvaluationContext<F> rctx, OperationResult result)
			throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException {

		// For assignments we consider only directly attached "situation" policy rules. In the future, we might configure this.
		// So, if someone wants to report (forward) triggers from a target, he must ensure that a particular
		// "situation" constraint is present directly on it.
		if (rctx instanceof AssignmentPolicyRuleEvaluationContext && !((AssignmentPolicyRuleEvaluationContext) rctx).isDirect) {
			return null;
		}

		// Single pass only (for the time being)
		PolicySituationPolicyConstraintType situationConstraint = constraint.getValue();
		Collection<EvaluatedPolicyRule> sourceRules =
				selectTriggeredRules(rctx, situationConstraint.getSituation());
		if (sourceRules.isEmpty()) {
			return null;
		}
		return new EvaluatedSituationTrigger(situationConstraint, createMessage(sourceRules, constraint.getValue(), rctx, result), sourceRules);
	}

	private LocalizableMessage createMessage(Collection<EvaluatedPolicyRule> sourceRules,
			AbstractPolicyConstraintType constraint, PolicyRuleEvaluationContext<?> ctx, OperationResult result)
			throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
		// determine if there's a single message that could be retrieved
		List<TreeNode<LocalizableMessage>> messageTrees = sourceRules.stream()
				.flatMap(r -> r.extractMessages().stream())
				.collect(Collectors.toList());
		LocalizableMessage defaultMessage;
		if (messageTrees.size() == 1) {
			defaultMessage = messageTrees.get(0).getUserObject();
		} else {
			defaultMessage = new LocalizableMessageBuilder()
					.key(SchemaConstants.DEFAULT_POLICY_CONSTRAINT_KEY_PREFIX + CONSTRAINT_KEY)
					.build();
		}
		return evaluatorHelper.createLocalizableMessage(constraint, ctx, defaultMessage, result);
	}


	private <F extends FocusType> Collection<EvaluatedPolicyRule> selectTriggeredRules(
			PolicyRuleEvaluationContext<F> rctx, List<String> situations) {
		Collection<EvaluatedPolicyRule> rules;
		if (rctx instanceof AssignmentPolicyRuleEvaluationContext) {
			EvaluatedAssignmentImpl<F> evaluatedAssignment = ((AssignmentPolicyRuleEvaluationContext<F>) rctx).evaluatedAssignment;
			// We consider all rules here, i.e. also those that are triggered on targets induced by this one.
			// Decision whether to trigger such rules lies on "primary" policy constraints. (E.g. approvals would
			// not trigger, whereas exclusions probably would.) Overall, our responsibility is simply to collect
			// all triggered rules.
			rules = evaluatedAssignment.getAllTargetsPolicyRules();
		} else {
			rules = rctx.focusContext.getPolicyRules();
		}
		return rules.stream()
				.filter(r -> r.isTriggered() && situations.contains(r.getPolicySituation()))
				.collect(Collectors.toList());
	}
}
