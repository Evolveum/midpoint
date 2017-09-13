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

import com.evolveum.midpoint.model.impl.expr.ModelExpressionThreadLocalHolder;
import com.evolveum.midpoint.model.impl.lens.projector.policy.AssignmentPolicyRuleEvaluationContext;
import com.evolveum.midpoint.model.impl.lens.projector.policy.ObjectState;
import com.evolveum.midpoint.model.impl.lens.projector.policy.PolicyRuleEvaluationContext;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.repo.common.expression.Expression;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.LocalizationUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.LocalizableMessageBuilder;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.namespace.QName;
import java.util.List;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.util.MiscUtil.getSingleValue;

/**
 * @author mederly
 */
@Component
public class ConstraintEvaluatorHelper {

	@Autowired private PrismContext prismContext;
	@Autowired protected ExpressionFactory expressionFactory;

	public <F extends FocusType> ExpressionVariables createExpressionVariables(PolicyRuleEvaluationContext<F> rctx) {
		ExpressionVariables var = new ExpressionVariables();
		PrismObject<F> object = rctx.getObject();
		var.addVariableDefinition(ExpressionConstants.VAR_USER, object);
		var.addVariableDefinition(ExpressionConstants.VAR_FOCUS, object);
		var.addVariableDefinition(ExpressionConstants.VAR_OBJECT, object);
		if (rctx instanceof AssignmentPolicyRuleEvaluationContext) {
			AssignmentPolicyRuleEvaluationContext actx = (AssignmentPolicyRuleEvaluationContext<F>) rctx;
			var.addVariableDefinition(ExpressionConstants.VAR_TARGET, actx.evaluatedAssignment.getTarget());
			var.addVariableDefinition(ExpressionConstants.VAR_EVALUATED_ASSIGNMENT, actx.evaluatedAssignment);
			var.addVariableDefinition(ExpressionConstants.VAR_ASSIGNMENT, actx.evaluatedAssignment.getAssignmentType(actx.state == ObjectState.BEFORE));
		} else {
			var.addVariableDefinition(ExpressionConstants.VAR_TARGET, null);
			var.addVariableDefinition(ExpressionConstants.VAR_EVALUATED_ASSIGNMENT, null);
			var.addVariableDefinition(ExpressionConstants.VAR_ASSIGNMENT, null);
		}
		return var;
	}

	public boolean evaluateBoolean(ExpressionType expressionBean, ExpressionVariables expressionVariables,
			String contextDescription, Task task, OperationResult result)
			throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException {
		return evaluateExpressionSingle(expressionBean, expressionVariables, contextDescription, task, result,
				DOMUtil.XSD_BOOLEAN, false);
	}

	public String evaluateString(ExpressionType expressionBean, ExpressionVariables expressionVariables,
			String contextDescription, Task task, OperationResult result)
			throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException {
		return evaluateExpressionSingle(expressionBean, expressionVariables, contextDescription, task, result,
				DOMUtil.XSD_STRING, null);
	}

	public <T> T evaluateExpressionSingle(ExpressionType expressionBean, ExpressionVariables expressionVariables,
			String contextDescription, Task task, OperationResult result, QName typeName, T defaultValue)
			throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException {
		PrismPropertyDefinition<T> resultDef = new PrismPropertyDefinitionImpl<>(
				new QName(SchemaConstants.NS_C, "result"), typeName, prismContext);
		Expression<PrismPropertyValue<T>,PrismPropertyDefinition<T>> expression =
				expressionFactory.makeExpression(expressionBean, resultDef, contextDescription, task, result);
		ExpressionEvaluationContext context = new ExpressionEvaluationContext(null, expressionVariables, contextDescription, task, result);
		PrismValueDeltaSetTriple<PrismPropertyValue<T>> exprResultTriple = ModelExpressionThreadLocalHolder
				.evaluateExpressionInContext(expression, context, task, result);
		List<T> results = exprResultTriple.getZeroSet().stream()
				.map(ppv -> (T) ppv.getRealValue())
				.collect(Collectors.toList());
		return getSingleValue(results, defaultValue, contextDescription);
	}

	public <F extends FocusType> LocalizableMessageType createLocalizableMessageType(LocalizableMessageTemplateType template,
			PolicyRuleEvaluationContext<F> rctx, OperationResult result)
			throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
		ExpressionVariables var = createExpressionVariables(rctx);
		LocalizableMessageType rv = new LocalizableMessageType();
		if (template.getKey() != null) {
			rv.setKey(template.getKey());
		} else if (template.getKeyExpression() != null) {
			rv.setKey(evaluateString(template.getKeyExpression(), var, "localizable message key expression", rctx.task, result));
		}
		if (!template.getArgument().isEmpty() && !template.getArgumentExpression().isEmpty()) {
			throw new IllegalArgumentException("Both argument and argumentExpression items are non empty");
		} else if (!template.getArgumentExpression().isEmpty()) {
			for (ExpressionType argumentExpression : template.getArgumentExpression()) {
				String argument = evaluateString(argumentExpression, var,
						"localizable message argument expression", rctx.task, result);
				// TODO other argument types OR even localizable messages themselves!
				rv.getArgument().add(new LocalizableMessageArgumentType().value(argument));
			}
		} else {
			// TODO allow localizable messages templates here
			rv.getArgument().addAll(template.getArgument());
		}
		if (template.getFallbackMessage() != null) {
			rv.setFallbackMessage(template.getFallbackMessage());
		} else if (template.getFallbackMessageExpression() != null) {
			rv.setFallbackMessage(evaluateString(template.getFallbackMessageExpression(), var, "localizable message fallback expression", rctx.task, result));
		}
		return rv;
	}

	public LocalizableMessage createObjectSpecification(PrismObject<?> object) {
		if (object != null) {
			String objectClassName = object.asObjectable().getClass().getSimpleName();
			LocalizableMessage objectTypeMessage = new LocalizableMessageBuilder()
					.key(SchemaConstants.OBJECT_TYPE_KEY_PREFIX + objectClassName)
					.fallbackMessage(objectClassName)
					.build();
			return new LocalizableMessageBuilder()
					.key("TechnicalObjectSpecification")
					.args(objectTypeMessage, object.asObjectable().getName(), object.getOid())
					.build();
		} else {
			return LocalizableMessageBuilder.buildFallbackMessage("?");          // should not really occur!
		}
	}

	public <F extends FocusType> LocalizableMessage createLocalizableMessage(
			AbstractPolicyConstraintType constraint, PolicyRuleEvaluationContext<F> rctx,
			LocalizableMessage defaultMessage, OperationResult result) throws ExpressionEvaluationException,
			ObjectNotFoundException, SchemaException {
		if (constraint.getPresentation() != null && constraint.getPresentation().getMessage() != null) {
			LocalizableMessageType messageType =
					createLocalizableMessageType(constraint.getPresentation().getMessage(), rctx, result);
			return LocalizationUtil.parseLocalizableMessageType(messageType, defaultMessage);
		} else if (constraint.getName() != null) {
			return new LocalizableMessageBuilder()
					.key(SchemaConstants.POLICY_CONSTRAINT_KEY_PREFIX + constraint.getName())
					.fallbackLocalizableMessage(defaultMessage)
					.build();
		} else {
			return defaultMessage;
		}
	}

	public LocalizableMessage createBeforeAfterMessage(PolicyRuleEvaluationContext<?> ctx) {
		return LocalizableMessageBuilder.buildKey(ctx.state == ObjectState.AFTER ?
				SchemaConstants.POLICY_CONSTRAINTS_AFTER_KEY : SchemaConstants.POLICY_CONSTRAINTS_BEFORE_KEY);
	}
}
