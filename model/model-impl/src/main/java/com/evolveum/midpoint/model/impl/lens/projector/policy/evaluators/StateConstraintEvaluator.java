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
import com.evolveum.midpoint.model.impl.expr.ModelExpressionThreadLocalHolder;
import com.evolveum.midpoint.model.impl.lens.EvaluatedAssignmentImpl;
import com.evolveum.midpoint.model.impl.lens.projector.policy.AssignmentPolicyRuleEvaluationContext;
import com.evolveum.midpoint.model.impl.lens.projector.policy.PolicyRuleEvaluationContext;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.marshaller.QueryConvertor;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.repo.common.expression.*;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.util.MiscUtil.getSingleValue;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyConstraintKindType.ASSIGNMENT_STATE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyConstraintKindType.FOCUS_STATE;

/**
 * @author mederly
 */
@Component
public class StateConstraintEvaluator implements PolicyConstraintEvaluator<StatePolicyConstraintType> {

	private static final Trace LOGGER = TraceManager.getTrace(StateConstraintEvaluator.class);

	@Autowired private PrismContext prismContext;
	@Autowired private MatchingRuleRegistry matchingRuleRegistry;
	@Autowired protected ExpressionFactory expressionFactory;

	@Override
	public <F extends FocusType> EvaluatedPolicyRuleTrigger<?> evaluate(JAXBElement<StatePolicyConstraintType> constraint,
			PolicyRuleEvaluationContext<F> rctx, OperationResult result)
			throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException {

		boolean assignmentState;
		if (QNameUtil.match(constraint.getName(), PolicyConstraintsType.F_ASSIGNMENT_STATE)) {
			assignmentState = true;
		} else if (QNameUtil.match(constraint.getName(), PolicyConstraintsType.F_FOCUS_STATE)) {
			assignmentState = false;
		} else {
			throw new AssertionError("unexpected state constraint " + constraint.getName());
		}

		List<ObjectInState<F>> objects = new ArrayList<>();
		List<FocusStateType> states = constraint.getValue().getCheckInState().isEmpty()
				? Collections.singletonList(FocusStateType.CURRENT)
				: constraint.getValue().getCheckInState();
		for (FocusStateType state : states) {
			PrismObject<F> object;
			if (state == null) {
				state = FocusStateType.CURRENT;
			}
			switch (state) {
				case OLD: object = rctx.focusContext.getObjectOld(); break;
				case NEW: object = rctx.focusContext.getObjectNew(); break;
				case CURRENT:
					object = rctx.focusContext.getObjectCurrent();
					if (object == null) {
						object = rctx.focusContext.getObjectNew();
					}
					break;
				default: throw new AssertionError("Wrong state: " + state);
			}
			if (object != null && !ObjectInState.contains(objects, object)) {
				objects.add(new ObjectInState<>(object, state));
			}
		}
		if (objects.isEmpty()) {
			return null;
		}

		if (assignmentState) {
			if (!(rctx instanceof AssignmentPolicyRuleEvaluationContext)) {
				return null;            // assignment state can be evaluated only in the context of an assignment
			} else {
				return evaluateForAssignment(constraint.getValue(), (AssignmentPolicyRuleEvaluationContext<F>) rctx, objects, result);
			}
		} else {
			return evaluateForFocus(constraint.getValue(), rctx, objects, result);
		}
	}

	private static class ObjectInState<F extends FocusType> {
		final PrismObject<F> object;
		final FocusStateType state;

		private ObjectInState(PrismObject<F> object, FocusStateType state) {
			this.object = object;
			this.state = state;
		}

		public static <F extends FocusType> boolean contains(List<ObjectInState<F>> objects, PrismObject<F> object) {
			return objects.stream().anyMatch(os -> os.object.equals(object));
		}
	}

	private <F extends FocusType> EvaluatedPolicyRuleTrigger<?> evaluateForFocus(StatePolicyConstraintType constraint,
			PolicyRuleEvaluationContext<F> ctx, List<ObjectInState<F>> objects, OperationResult result)
			throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException {

		if (constraint.getFilter() == null && constraint.getExpression() == null) {
			return null;        // shouldn't occur
		}

		for (ObjectInState<F> objectInState : objects) {
			PrismObject<F> object = objectInState.object;
			boolean match = true;
			if (constraint.getFilter() != null) {
				ObjectFilter filter = QueryConvertor
						.parseFilter(constraint.getFilter(), object.asObjectable().getClass(), prismContext);
				if (!filter.match(object.getValue(), matchingRuleRegistry)) {
					match = false;
				}
			}
			if (match && constraint.getExpression() != null) {
				match = isConstraintSatisfied(constraint.getExpression(), createExpressionVariables(ctx, object),
						"expression in focus state constraint " + constraint.getName() + " (" + objectInState.state + ")", ctx.task, result);
			}

			if (match) {
				return new EvaluatedPolicyRuleTrigger<>(FOCUS_STATE, constraint, "Focus state ("+ objectInState.state.value() + ") matches " +
						(constraint.getName() != null ? "constraint '" + constraint.getName() + "'" : "the constraint"));
			}
		}
		return null;
	}

	private <F extends FocusType> EvaluatedPolicyRuleTrigger<?> evaluateForAssignment(StatePolicyConstraintType constraint,
			AssignmentPolicyRuleEvaluationContext<F> ctx, List<ObjectInState<F>> objects,
			OperationResult result)
			throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
		if (constraint.getFilter() != null) {
			throw new UnsupportedOperationException("Filter is not supported for assignment state constraints yet.");
		}
		if (constraint.getExpression() == null) {
			return null;        // shouldn't occur
		}

		for (ObjectInState<F> objectInState : objects) {
			boolean match = isConstraintSatisfied(constraint.getExpression(), createExpressionVariables(ctx, objectInState.object),
					"expression in assignment state constraint " + constraint.getName() + " (" + objectInState.state + ")", ctx.task, result);
			if (match) {
				return new EvaluatedPolicyRuleTrigger<>(ASSIGNMENT_STATE, constraint, "Assignment state ("+ objectInState.state.value() + ") matches " +
						(constraint.getName() != null ? "constraint '" + constraint.getName() + "'" : "the constraint"));
			}
		}
		return null;
	}

	private <F extends FocusType> ExpressionVariables createExpressionVariables(PolicyRuleEvaluationContext<F> ctx,
			PrismObject<F> object) {
		ExpressionVariables var = new ExpressionVariables();
		var.addVariableDefinition(ExpressionConstants.VAR_USER, object);
		var.addVariableDefinition(ExpressionConstants.VAR_FOCUS, object);
		EvaluatedAssignmentImpl<F> evaluatedAssignment = ctx instanceof AssignmentPolicyRuleEvaluationContext
				? ((AssignmentPolicyRuleEvaluationContext<F>) ctx).evaluatedAssignment : null;
		var.addVariableDefinition(ExpressionConstants.VAR_TARGET, evaluatedAssignment != null ? evaluatedAssignment.getTarget() : null);
		var.addVariableDefinition(ExpressionConstants.VAR_ASSIGNMENT, evaluatedAssignment);                // TODO: ok?
		return var;
	}

	private boolean isConstraintSatisfied(ExpressionType expressionBean, ExpressionVariables expressionVariables,
			String contextDescription, Task task, OperationResult result)
			throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException {
		PrismPropertyDefinition<Boolean> resultDef = new PrismPropertyDefinitionImpl<>(
				new QName(SchemaConstants.NS_C, "result"), DOMUtil.XSD_BOOLEAN, prismContext);
		Expression<PrismPropertyValue<Boolean>,PrismPropertyDefinition<Boolean>> expression =
				expressionFactory.makeExpression(expressionBean, resultDef, contextDescription, task, result);
		ExpressionEvaluationContext context = new ExpressionEvaluationContext(null, expressionVariables, contextDescription, task, result);
		PrismValueDeltaSetTriple<PrismPropertyValue<Boolean>> exprResultTriple = ModelExpressionThreadLocalHolder
				.evaluateExpressionInContext(expression, context, task, result);
		List<Boolean> results = exprResultTriple.getZeroSet().stream()
				.map(ppv -> (Boolean) ppv.getRealValue())
				.collect(Collectors.toList());
		return getSingleValue(results, false, contextDescription);
	}

}
