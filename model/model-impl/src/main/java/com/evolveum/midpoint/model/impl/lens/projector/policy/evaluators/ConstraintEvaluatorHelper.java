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

import com.evolveum.midpoint.model.impl.lens.LensUtil;
import com.evolveum.midpoint.model.impl.lens.projector.policy.AssignmentPolicyRuleEvaluationContext;
import com.evolveum.midpoint.model.impl.lens.projector.policy.ObjectState;
import com.evolveum.midpoint.model.impl.lens.projector.policy.PolicyRuleEvaluationContext;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.LocalizationUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.LocalizableMessageBuilder;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.namespace.QName;

import static com.evolveum.midpoint.schema.constants.ExpressionConstants.VAR_RULE_EVALUATION_CONTEXT;

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
		var.addVariableDefinition(VAR_RULE_EVALUATION_CONTEXT, rctx);
		return var;
	}

	public boolean evaluateBoolean(ExpressionType expressionBean, ExpressionVariables expressionVariables,
			String contextDescription, Task task, OperationResult result)
			throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException {
		return LensUtil.evaluateBoolean(expressionBean, expressionVariables, contextDescription, expressionFactory, prismContext,
				task, result);
	}

	public String evaluateString(ExpressionType expressionBean, ExpressionVariables expressionVariables,
			String contextDescription, Task task, OperationResult result)
			throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException {
		return LensUtil.evaluateString(expressionBean, expressionVariables, contextDescription, expressionFactory, prismContext,
				task, result);
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

	public <F extends FocusType> LocalizableMessage createLocalizableMessage(
			AbstractPolicyConstraintType constraint, PolicyRuleEvaluationContext<F> rctx,
			LocalizableMessage builtInMessage, OperationResult result) throws ExpressionEvaluationException,
			ObjectNotFoundException, SchemaException {
		if (constraint.getPresentation() != null && constraint.getPresentation().getMessage() != null) {
			LocalizableMessageType messageType =
					createLocalizableMessageType(constraint.getPresentation().getMessage(), rctx, result);
			return LocalizationUtil.parseLocalizableMessageType(messageType,
					// if user-configured fallback message is present; we ignore the built-in constraint message
					// TODO consider ignoring it always if custom presentation/message is provided
					messageType.getFallbackMessage() != null ? null : builtInMessage);
		} else if (constraint.getName() != null) {
			return new LocalizableMessageBuilder()
					.key(SchemaConstants.POLICY_CONSTRAINT_KEY_PREFIX + constraint.getName())
					.fallbackLocalizableMessage(builtInMessage)
					.build();
		} else {
			return builtInMessage;
		}
	}

	public <F extends FocusType> LocalizableMessage createLocalizableShortMessage(
			AbstractPolicyConstraintType constraint, PolicyRuleEvaluationContext<F> rctx,
			LocalizableMessage builtInMessage, OperationResult result) throws ExpressionEvaluationException,
			ObjectNotFoundException, SchemaException {
		if (constraint.getPresentation() != null && constraint.getPresentation().getShortMessage() != null) {
			LocalizableMessageType messageType =
					createLocalizableMessageType(constraint.getPresentation().getShortMessage(), rctx, result);
			return LocalizationUtil.parseLocalizableMessageType(messageType,
					// if user-configured fallback message is present; we ignore the built-in constraint message
					// TODO consider ignoring it always if custom presentation/message is provided
					messageType.getFallbackMessage() != null ? null : builtInMessage);
		} else if (constraint.getName() != null) {
			return new LocalizableMessageBuilder()
					.key(SchemaConstants.POLICY_CONSTRAINT_SHORT_MESSAGE_KEY_PREFIX + constraint.getName())
					.fallbackLocalizableMessage(builtInMessage)
					.build();
		} else {
			return builtInMessage;
		}
	}

	public LocalizableMessage createBeforeAfterMessage(PolicyRuleEvaluationContext<?> ctx) {
		return LocalizableMessageBuilder.buildKey(ctx.state == ObjectState.AFTER ?
				SchemaConstants.POLICY_CONSTRAINTS_AFTER_KEY : SchemaConstants.POLICY_CONSTRAINTS_BEFORE_KEY);
	}
}
