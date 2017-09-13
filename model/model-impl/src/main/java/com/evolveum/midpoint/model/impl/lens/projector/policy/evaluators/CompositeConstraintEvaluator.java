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
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyConstraintKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyConstraintsType;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.bind.JAXBElement;
import java.util.List;

import static com.evolveum.midpoint.util.LocalizableMessageBuilder.buildFallbackMessage;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyConstraintKindType.*;

/**
 * @author mederly
 */
@Component
public class CompositeConstraintEvaluator implements PolicyConstraintEvaluator<PolicyConstraintsType> {

	@Autowired private PolicyRuleProcessor policyRuleProcessor;

	@Override
	public <F extends FocusType> EvaluatedPolicyRuleTrigger evaluate(JAXBElement<PolicyConstraintsType> constraint,
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
				return createTrigger(NOT, constraint.getValue(), triggers);
			}
		} else {
			if (!triggers.isEmpty()) {
				return createTrigger(isAnd ? AND : OR, constraint.getValue(), triggers);
			}
		}
		return null;
	}

	@NotNull
	private EvaluatedCompositeTrigger createTrigger(PolicyConstraintKindType kind, PolicyConstraintsType value,
			List<EvaluatedPolicyRuleTrigger<?>> triggers) {
		return new EvaluatedCompositeTrigger(kind, value, buildFallbackMessage("'" + kind.value() + "' policy constraint applies"), triggers);
	}
}
