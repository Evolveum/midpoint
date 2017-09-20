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

import com.evolveum.midpoint.model.api.PipelineItem;
import com.evolveum.midpoint.model.api.ScriptExecutionException;
import com.evolveum.midpoint.model.api.context.EvaluatedPolicyRuleTrigger;
import com.evolveum.midpoint.model.api.context.EvaluatedStateTrigger;
import com.evolveum.midpoint.model.impl.lens.projector.policy.AssignmentPolicyRuleEvaluationContext;
import com.evolveum.midpoint.model.impl.lens.projector.policy.PolicyRuleEvaluationContext;
import com.evolveum.midpoint.model.impl.scripting.ExecutionContext;
import com.evolveum.midpoint.model.impl.scripting.PipelineData;
import com.evolveum.midpoint.model.impl.scripting.ScriptingExpressionEvaluator;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.marshaller.QueryConvertor;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.LocalizableMessageBuilder;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyConstraintsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.StatePolicyConstraintType;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.bind.JAXBElement;

import java.util.*;

import static com.evolveum.midpoint.schema.constants.ExpressionConstants.VAR_OBJECT;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyConstraintKindType.ASSIGNMENT_STATE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyConstraintKindType.OBJECT_STATE;
import static java.util.Collections.emptyList;

/**
 * @author mederly
 */
@Component
public class StateConstraintEvaluator implements PolicyConstraintEvaluator<StatePolicyConstraintType> {

	private static final Trace LOGGER = TraceManager.getTrace(StateConstraintEvaluator.class);

	private static final String OBJECT_CONSTRAINT_KEY_PREFIX = "objectState.";
	private static final String ASSIGNMENT_CONSTRAINT_KEY_PREFIX = "assignmentState.";
	private static final String KEY_NAMED = "named";
	private static final String KEY_UNNAMED = "unnamed";

	@Autowired private PrismContext prismContext;
	@Autowired private MatchingRuleRegistry matchingRuleRegistry;
	@Autowired protected ExpressionFactory expressionFactory;
	@Autowired protected ConstraintEvaluatorHelper evaluatorHelper;
	@Autowired protected ScriptingExpressionEvaluator scriptingExpressionEvaluator;

	@Override
	public <F extends FocusType> EvaluatedPolicyRuleTrigger<?> evaluate(JAXBElement<StatePolicyConstraintType> constraint,
			PolicyRuleEvaluationContext<F> rctx, OperationResult result)
			throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException {

		if (QNameUtil.match(constraint.getName(), PolicyConstraintsType.F_ASSIGNMENT_STATE)) {
			if (!(rctx instanceof AssignmentPolicyRuleEvaluationContext)) {
				return null;            // assignment state can be evaluated only in the context of an assignment
			} else {
				return evaluateForAssignment(constraint.getValue(), (AssignmentPolicyRuleEvaluationContext<F>) rctx, result);
			}
		} else if (QNameUtil.match(constraint.getName(), PolicyConstraintsType.F_OBJECT_STATE)) {
			return evaluateForObject(constraint.getValue(), rctx, result);
		} else {
			throw new AssertionError("unexpected state constraint " + constraint.getName());
		}
	}

	private <F extends FocusType> EvaluatedPolicyRuleTrigger<?> evaluateForObject(StatePolicyConstraintType constraint,
			PolicyRuleEvaluationContext<F> ctx, OperationResult result)
			throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException {

		int count =
				(constraint.getFilter() != null ? 1 : 0)
				+ (constraint.getExpression() != null ? 1 : 0)
				+ (constraint.getExecuteScript() != null ? 1 : 0);

		if (count != 1) {
			throw new SchemaException("Exactly one of filter, expression, executeScript element must be present.");
		}

		PrismObject<F> object = ctx.getObject();
		if (object == null) {
			return null;
		}
		if (constraint.getFilter() != null) {
			ObjectFilter filter = QueryConvertor
					.parseFilter(constraint.getFilter(), object.asObjectable().getClass(), prismContext);
			if (!filter.match(object.getValue(), matchingRuleRegistry)) {
				return null;
			}
		}
		if (constraint.getExecuteScript() != null) {
			Map<String, Object> variables = new HashMap<>();
			variables.put(VAR_OBJECT.getLocalPart(), object);
			variables.put("ruleEvaluationContext", ctx);
			ExecutionContext resultingContext;
			try {
				resultingContext = scriptingExpressionEvaluator.evaluateExpression(constraint.getExecuteScript(), variables, ctx.task, result);
			} catch (ScriptExecutionException e) {
				throw new SystemException(e);       // TODO
			}
			PipelineData output = resultingContext.getFinalOutput();
			LOGGER.trace("Scripting expression returned {} item(s); console output is:\n{}",
					output != null ? output.getData().size() : null, resultingContext.getConsoleOutput());
			List<PipelineItem> items = output != null ? output.getData() : emptyList();
			if (items.isEmpty()) {
				return null;
			}
			// TODO retrieve localization messages from output
		}
		if (constraint.getExpression() != null) {
			if (!evaluatorHelper.evaluateBoolean(constraint.getExpression(), evaluatorHelper.createExpressionVariables(ctx),
					"expression in object state constraint " + constraint.getName() + " (" + ctx.state + ")", ctx.task, result)) {
				return null;
			}
			// TODO retrieve localization messages from return (it should be Object then, not Boolean)
		}

		return new EvaluatedStateTrigger(OBJECT_STATE, constraint, createMessage(OBJECT_CONSTRAINT_KEY_PREFIX, constraint, ctx, result));
	}

	private <F extends FocusType> EvaluatedPolicyRuleTrigger<?> evaluateForAssignment(StatePolicyConstraintType constraint,
			AssignmentPolicyRuleEvaluationContext<F> ctx, OperationResult result)
			throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
		if (constraint.getFilter() != null) {
			throw new UnsupportedOperationException("Filter is not supported for assignment state constraints yet.");
		}
		if (constraint.getExpression() == null) {
			return null;        // shouldn't occur
		}
		if (!ctx.isApplicableToState()) {
			return null;
		}
		boolean match = evaluatorHelper.evaluateBoolean(constraint.getExpression(), evaluatorHelper.createExpressionVariables(ctx),
				"expression in assignment state constraint " + constraint.getName() + " (" + ctx.state + ")", ctx.task, result);
		if (match) {
			return new EvaluatedStateTrigger(ASSIGNMENT_STATE, constraint, createMessage(ASSIGNMENT_CONSTRAINT_KEY_PREFIX, constraint, ctx, result));
		}
		return null;
	}

	@NotNull
	private <F extends FocusType> LocalizableMessage createMessage(String constraintKeyPrefix,
			StatePolicyConstraintType constraint, PolicyRuleEvaluationContext<F> ctx, OperationResult result)
			throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
		List<Object> args = new ArrayList<>();
		args.add(evaluatorHelper.createBeforeAfterMessage(ctx));
		String keySuffix;
		if (constraint.getName() != null) {
			args.add(constraint.getName());
			keySuffix = KEY_NAMED;
		} else {
			keySuffix = KEY_UNNAMED;
		}
		LocalizableMessage builtInMessage = new LocalizableMessageBuilder()
				.key(SchemaConstants.DEFAULT_POLICY_CONSTRAINT_KEY_PREFIX + constraintKeyPrefix + keySuffix)
				.args(args)
				.build();
		return evaluatorHelper.createLocalizableMessage(constraint, ctx, builtInMessage, result);
	}
}
