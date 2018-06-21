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
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import static com.evolveum.midpoint.schema.constants.ExpressionConstants.VAR_RULE_EVALUATION_CONTEXT;
import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.createDisplayInformation;

/**
 * @author mederly
 */
@Component
public class ConstraintEvaluatorHelper {

	public static final QName VAR_EVALUATOR_HELPER = new QName(SchemaConstants.NS_C, "evaluatorHelper");
	public static final QName VAR_CONSTRAINT_ELEMENT = new QName(SchemaConstants.NS_C, "constraintElement");
	public static final QName VAR_CONSTRAINT = new QName(SchemaConstants.NS_C, "constraint");

	@Autowired private PrismContext prismContext;
	@Autowired protected ExpressionFactory expressionFactory;

	// corresponds with PolicyRuleBasedAspect.processNameFromApprovalActions
	public <F extends FocusType> ExpressionVariables createExpressionVariables(PolicyRuleEvaluationContext<F> rctx,
			JAXBElement<? extends AbstractPolicyConstraintType> constraintElement) {
		ExpressionVariables var = new ExpressionVariables();
		PrismObject<F> object = rctx.getObject();
		var.addVariableDefinition(ExpressionConstants.VAR_USER, object);
		var.addVariableDefinition(ExpressionConstants.VAR_FOCUS, object);
		var.addVariableDefinition(ExpressionConstants.VAR_OBJECT, object);
		var.addVariableDefinition(ExpressionConstants.VAR_OBJECT_DISPLAY_INFORMATION, LocalizationUtil.createLocalizableMessageType(createDisplayInformation(object, false)));
		if (rctx instanceof AssignmentPolicyRuleEvaluationContext) {
			AssignmentPolicyRuleEvaluationContext actx = (AssignmentPolicyRuleEvaluationContext<F>) rctx;
			PrismObject target = actx.evaluatedAssignment.getTarget();
			var.addVariableDefinition(ExpressionConstants.VAR_TARGET, target);
			var.addVariableDefinition(ExpressionConstants.VAR_TARGET_DISPLAY_INFORMATION, LocalizationUtil.createLocalizableMessageType(createDisplayInformation(target, false)));
			var.addVariableDefinition(ExpressionConstants.VAR_EVALUATED_ASSIGNMENT, actx.evaluatedAssignment);
			var.addVariableDefinition(ExpressionConstants.VAR_ASSIGNMENT, actx.evaluatedAssignment.getAssignmentType(actx.state == ObjectState.BEFORE));
		} else {
			var.addVariableDefinition(ExpressionConstants.VAR_TARGET, null);
			var.addVariableDefinition(ExpressionConstants.VAR_TARGET_DISPLAY_INFORMATION, null);
			var.addVariableDefinition(ExpressionConstants.VAR_EVALUATED_ASSIGNMENT, null);
			var.addVariableDefinition(ExpressionConstants.VAR_ASSIGNMENT, null);
		}
		var.addVariableDefinition(VAR_RULE_EVALUATION_CONTEXT, rctx);
		var.addVariableDefinition(VAR_EVALUATOR_HELPER, this);
		var.addVariableDefinition(VAR_CONSTRAINT, constraintElement != null ? constraintElement.getValue() : null);
		var.addVariableDefinition(VAR_CONSTRAINT_ELEMENT, constraintElement);
		return var;
	}

	public boolean evaluateBoolean(ExpressionType expressionBean, ExpressionVariables expressionVariables,
			String contextDescription, Task task, OperationResult result)
			throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
		return LensUtil.evaluateBoolean(expressionBean, expressionVariables, contextDescription, expressionFactory, prismContext,
				task, result);
	}

	public LocalizableMessageType evaluateLocalizableMessageType(ExpressionType expressionBean, ExpressionVariables expressionVariables,
			String contextDescription, Task task, OperationResult result)
			throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
		return LensUtil.evaluateLocalizableMessageType(expressionBean, expressionVariables, contextDescription, expressionFactory, prismContext,
				task, result);
	}

	public String evaluateString(ExpressionType expressionBean, ExpressionVariables expressionVariables,
			String contextDescription, Task task, OperationResult result)
			throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
		return LensUtil.evaluateString(expressionBean, expressionVariables, contextDescription, expressionFactory, prismContext,
				task, result);
	}

	public <F extends FocusType> SingleLocalizableMessageType interpretLocalizableMessageTemplate(LocalizableMessageTemplateType template,
			PolicyRuleEvaluationContext<F> rctx, JAXBElement<? extends AbstractPolicyConstraintType> constraintElement, OperationResult result)
			throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {
		return LensUtil.interpretLocalizableMessageTemplate(template, createExpressionVariables(rctx, constraintElement), expressionFactory, prismContext, rctx.task, result);
	}

	public <F extends FocusType> LocalizableMessage createLocalizableMessage(
			JAXBElement<? extends AbstractPolicyConstraintType> constraintElement, PolicyRuleEvaluationContext<F> rctx,
			LocalizableMessage builtInMessage, OperationResult result) throws ExpressionEvaluationException,
			ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {
		AbstractPolicyConstraintType constraint = constraintElement.getValue();
		if (constraint.getPresentation() != null && constraint.getPresentation().getMessage() != null) {
			SingleLocalizableMessageType messageType =
					interpretLocalizableMessageTemplate(constraint.getPresentation().getMessage(), rctx, constraintElement, result);
			return LocalizationUtil.toLocalizableMessage(messageType);
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
			JAXBElement<? extends AbstractPolicyConstraintType> constraintElement, PolicyRuleEvaluationContext<F> rctx,
			LocalizableMessage builtInMessage, OperationResult result) throws ExpressionEvaluationException,
			ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {
		AbstractPolicyConstraintType constraint = constraintElement.getValue();
		if (constraint.getPresentation() != null && constraint.getPresentation().getShortMessage() != null) {
			SingleLocalizableMessageType messageType =
					interpretLocalizableMessageTemplate(constraint.getPresentation().getShortMessage(), rctx, constraintElement, result);
			return LocalizationUtil.toLocalizableMessage(messageType);
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
