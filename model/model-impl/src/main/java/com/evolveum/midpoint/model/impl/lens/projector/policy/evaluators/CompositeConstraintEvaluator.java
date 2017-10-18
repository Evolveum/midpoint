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

import com.evolveum.midpoint.model.api.context.EvaluatedCompositeTrigger;
import com.evolveum.midpoint.model.api.context.EvaluatedPolicyRuleTrigger;
import com.evolveum.midpoint.model.impl.lens.projector.policy.PolicyRuleEvaluationContext;
import com.evolveum.midpoint.model.impl.lens.projector.policy.PolicyRuleProcessor;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.LocalizableMessageBuilder;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.bind.JAXBElement;
import java.util.List;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyConstraintKindType.*;

/**
 * @author mederly
 */
@Component
public class CompositeConstraintEvaluator implements PolicyConstraintEvaluator<PolicyConstraintsType> {

	@Autowired private ConstraintEvaluatorHelper evaluatorHelper;
	@Autowired private PolicyRuleProcessor policyRuleProcessor;

	@Override
	public <F extends FocusType> EvaluatedCompositeTrigger evaluate(JAXBElement<PolicyConstraintsType> constraint,
			PolicyRuleEvaluationContext<F> rctx, OperationResult result)
			throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException {

		boolean isAnd = QNameUtil.match(PolicyConstraintsType.F_AND, constraint.getName());
		boolean isOr = QNameUtil.match(PolicyConstraintsType.F_OR, constraint.getName());
		boolean isNot = QNameUtil.match(PolicyConstraintsType.F_NOT, constraint.getName());
		assert isAnd || isOr || isNot;
		List<EvaluatedPolicyRuleTrigger<?>> triggers = policyRuleProcessor
				.evaluateConstraints(constraint.getValue(), !isOr, rctx, result);
		if (isNot) {
			if (triggers.isEmpty()) {
				return createTrigger(NOT, constraint.getValue(), triggers, rctx, result);
			}
		} else {
			if (!triggers.isEmpty()) {
				return createTrigger(isAnd ? AND : OR, constraint.getValue(), triggers, rctx, result);
			}
		}
		return null;
	}

	@NotNull
	private EvaluatedCompositeTrigger createTrigger(PolicyConstraintKindType kind, PolicyConstraintsType value,
			List<EvaluatedPolicyRuleTrigger<?>> triggers,
			PolicyRuleEvaluationContext<?> rctx, OperationResult result)
			throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
		return new EvaluatedCompositeTrigger(kind, value,
				createMessage(kind, value, rctx, result),
				createShortMessage(kind, value, rctx, result),
				triggers);
	}

	private LocalizableMessage createMessage(PolicyConstraintKindType kind,
			AbstractPolicyConstraintType constraint, PolicyRuleEvaluationContext<?> ctx, OperationResult result)
			throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
		LocalizableMessage builtInMessage = new LocalizableMessageBuilder()
				.key(SchemaConstants.DEFAULT_POLICY_CONSTRAINT_KEY_PREFIX + kind.value())
				.build();
		return evaluatorHelper.createLocalizableMessage(constraint, ctx, builtInMessage, result);
	}

	private LocalizableMessage createShortMessage(PolicyConstraintKindType kind,
			AbstractPolicyConstraintType constraint, PolicyRuleEvaluationContext<?> ctx, OperationResult result)
			throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
		LocalizableMessage builtInMessage = new LocalizableMessageBuilder()
				.key(SchemaConstants.DEFAULT_POLICY_CONSTRAINT_SHORT_MESSAGE_KEY_PREFIX + kind.value())
				.build();
		return evaluatorHelper.createLocalizableShortMessage(constraint, ctx, builtInMessage, result);
	}
}
