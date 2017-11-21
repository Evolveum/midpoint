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

import com.evolveum.midpoint.model.api.context.ModelState;
import com.evolveum.midpoint.model.impl.lens.projector.policy.PolicyRuleEvaluationContext;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ModificationPolicyConstraintType;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author semancik
 * @author mederly
 */
@Component
public abstract class ModificationConstraintEvaluator<T extends ModificationPolicyConstraintType> implements PolicyConstraintEvaluator<T> {

	private static final Trace LOGGER = TraceManager.getTrace(ModificationConstraintEvaluator.class);

	@Autowired protected ConstraintEvaluatorHelper evaluatorHelper;

	@NotNull
	protected <F extends FocusType> String createStateKey(PolicyRuleEvaluationContext<F> rctx) {
		ModelState state = rctx.lensContext.getState();
		String stateKey;
		if (state == ModelState.INITIAL || state == ModelState.PRIMARY) {
			stateKey = "toBe";
		} else {
			stateKey = "was";
			// TODO derive more precise information from executed deltas, if needed
		}
		return stateKey;
	}

	boolean expressionPasses(T constraint, PolicyRuleEvaluationContext<?> ctx, OperationResult result)
			throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
			ConfigurationException, ExpressionEvaluationException {
		if (constraint.getExpression() != null) {
			if (!evaluatorHelper.evaluateBoolean(constraint.getExpression(), evaluatorHelper.createExpressionVariables(ctx),
					"expression in modification constraint " + constraint.getName() + " (" + ctx.state + ")", ctx.task, result)) {
				return false;
			}
			// TODO retrieve localization messages from return (it should be Object then, not Boolean)
		}
		return true;
	}
}
