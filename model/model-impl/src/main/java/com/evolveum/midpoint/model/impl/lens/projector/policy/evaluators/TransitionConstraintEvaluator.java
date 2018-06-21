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

import com.evolveum.midpoint.model.api.context.EvaluatedPolicyRuleTrigger;
import com.evolveum.midpoint.model.api.context.EvaluatedTransitionTrigger;
import com.evolveum.midpoint.model.impl.lens.projector.policy.ObjectState;
import com.evolveum.midpoint.model.impl.lens.projector.policy.PolicyRuleEvaluationContext;
import com.evolveum.midpoint.model.impl.lens.projector.policy.PolicyRuleProcessor;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.LocalizableMessageBuilder;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractPolicyConstraintType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyConstraintKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TransitionPolicyConstraintType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.bind.JAXBElement;
import java.util.ArrayList;
import java.util.List;

/**
 * @author mederly
 */
@Component
public class TransitionConstraintEvaluator implements PolicyConstraintEvaluator<TransitionPolicyConstraintType> {

	private static final String CONSTRAINT_KEY = "transition";

	@Autowired private ConstraintEvaluatorHelper evaluatorHelper;
	@Autowired private PolicyRuleProcessor policyRuleProcessor;

	@Override
	public <F extends FocusType> EvaluatedPolicyRuleTrigger evaluate(JAXBElement<TransitionPolicyConstraintType> constraintElement,
			PolicyRuleEvaluationContext<F> rctx, OperationResult result)
			throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {

		TransitionPolicyConstraintType trans = constraintElement.getValue();
		List<EvaluatedPolicyRuleTrigger<?>> triggers = new ArrayList<>();
		boolean match =
			evaluateState(trans, rctx, ObjectState.BEFORE, trans.isStateBefore(), triggers, result)
					&& evaluateState(trans, rctx, ObjectState.AFTER, trans.isStateAfter(), triggers, result);
		if (match) {
			return new EvaluatedTransitionTrigger(PolicyConstraintKindType.TRANSITION, trans,
					createMessage(constraintElement, rctx, result),
					createShortMessage(constraintElement, rctx, result),
					triggers);
		} else {
			return null;
		}
	}

	private <F extends FocusType> boolean evaluateState(TransitionPolicyConstraintType trans,
			PolicyRuleEvaluationContext<F> rctx, ObjectState state, Boolean expected,
			List<EvaluatedPolicyRuleTrigger<?>> triggers, OperationResult result)
			throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {
		if (expected == null) {
			return true;
		}
		PolicyRuleEvaluationContext<F> subContext = rctx.cloneWithStateConstraints(state);
		List<EvaluatedPolicyRuleTrigger<?>> subTriggers = policyRuleProcessor
				.evaluateConstraints(trans.getConstraints(), true, subContext, result);
		triggers.addAll(subTriggers);
		boolean real = !subTriggers.isEmpty();
		return expected == real;
	}

	private LocalizableMessage createMessage(JAXBElement<TransitionPolicyConstraintType> constraintElement, PolicyRuleEvaluationContext<?> ctx, OperationResult result)
			throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {
		LocalizableMessage builtInMessage = new LocalizableMessageBuilder()
				.key(SchemaConstants.DEFAULT_POLICY_CONSTRAINT_KEY_PREFIX + CONSTRAINT_KEY)
				.build();
		return evaluatorHelper.createLocalizableMessage(constraintElement, ctx, builtInMessage, result);
	}

	private LocalizableMessage createShortMessage(JAXBElement<TransitionPolicyConstraintType> constraintElement, PolicyRuleEvaluationContext<?> ctx, OperationResult result)
			throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {
		LocalizableMessage builtInMessage = new LocalizableMessageBuilder()
				.key(SchemaConstants.DEFAULT_POLICY_CONSTRAINT_SHORT_MESSAGE_KEY_PREFIX + CONSTRAINT_KEY)
				.build();
		return evaluatorHelper.createLocalizableShortMessage(constraintElement, ctx, builtInMessage, result);
	}
}
