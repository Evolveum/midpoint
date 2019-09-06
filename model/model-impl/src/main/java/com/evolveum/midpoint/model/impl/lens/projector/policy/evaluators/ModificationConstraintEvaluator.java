/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.policy.evaluators;

import com.evolveum.midpoint.model.api.context.ModelState;
import com.evolveum.midpoint.model.impl.lens.projector.policy.PolicyRuleEvaluationContext;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.RelationRegistry;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ModificationPolicyConstraintType;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.bind.JAXBElement;

/**
 * @author semancik
 * @author mederly
 */
@Component
public abstract class ModificationConstraintEvaluator<T extends ModificationPolicyConstraintType> implements PolicyConstraintEvaluator<T> {

	private static final Trace LOGGER = TraceManager.getTrace(ModificationConstraintEvaluator.class);

	@Autowired protected ConstraintEvaluatorHelper evaluatorHelper;
	@Autowired protected PrismContext prismContext;
	@Autowired protected RelationRegistry relationRegistry;

	@NotNull
	protected <AH extends AssignmentHolderType> String createStateKey(PolicyRuleEvaluationContext<AH> rctx) {
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

	boolean expressionPasses(JAXBElement<T> constraintElement, PolicyRuleEvaluationContext<?> ctx, OperationResult result)
			throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
			ConfigurationException, ExpressionEvaluationException {
		T constraint = constraintElement.getValue();
		if (constraint.getExpression() != null) {
			if (!evaluatorHelper.evaluateBoolean(constraint.getExpression(), evaluatorHelper.createExpressionVariables(ctx, constraintElement),
					"expression in modification constraint " + constraint.getName() + " (" + ctx.state + ")", ctx.task, result)) {
				return false;
			}
			// TODO retrieve localization messages from return (it should be Object then, not Boolean)
		}
		return true;
	}
}
