/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.policy.evaluators;

import static com.evolveum.midpoint.schema.constants.ExpressionConstants.VAR_RULE_EVALUATION_CONTEXT;
import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.createDisplayInformation;

import jakarta.xml.bind.JAXBElement;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.context.EvaluatedAssignment;
import com.evolveum.midpoint.model.impl.lens.LensExpressionUtil;
import com.evolveum.midpoint.model.impl.lens.projector.policy.AssignmentPolicyRuleEvaluationContext;
import com.evolveum.midpoint.model.impl.lens.projector.policy.ObjectState;
import com.evolveum.midpoint.model.impl.lens.projector.policy.PolicyRuleEvaluationContext;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.LocalizationUtil;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.LocalizableMessageBuilder;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@Component
public class ConstraintEvaluatorHelper {

    private static final String VAR_EVALUATOR_HELPER = "evaluatorHelper";
    private static final String VAR_CONSTRAINT_ELEMENT = "constraintElement";
    private static final String VAR_CONSTRAINT = "constraint";

    @Autowired protected ExpressionFactory expressionFactory;

    // corresponds with PolicyRuleBasedAspect.processNameFromApprovalActions
    public <O extends ObjectType> VariablesMap createVariablesMap(
            PolicyRuleEvaluationContext<O> rctx,
            JAXBElement<? extends AbstractPolicyConstraintType> constraintElement) {
        VariablesMap var = new VariablesMap();
        PrismObject<O> object = rctx.getObject();
        PrismObjectDefinition<O> objectDefinition = rctx.getObjectDefinition();
        var.put(ExpressionConstants.VAR_USER, object, objectDefinition);
        var.put(ExpressionConstants.VAR_FOCUS, object, objectDefinition);
        var.put(ExpressionConstants.VAR_OBJECT, object, objectDefinition);
        var.put(ExpressionConstants.VAR_OBJECT_DISPLAY_INFORMATION,
                LocalizationUtil.createLocalizableMessageType(createDisplayInformation(object, false)), LocalizableMessageType.class);
        if (rctx instanceof AssignmentPolicyRuleEvaluationContext) {
            AssignmentPolicyRuleEvaluationContext<?> actx = (AssignmentPolicyRuleEvaluationContext<?>) rctx;
            PrismObject<?> target = actx.evaluatedAssignment.getTarget();
            if (target != null) {
                var.put(ExpressionConstants.VAR_TARGET, target, target.getDefinition());
            } else {
                var.put(ExpressionConstants.VAR_TARGET, null, getObjectTypeDefinition());
            }
            var.put(ExpressionConstants.VAR_TARGET_DISPLAY_INFORMATION,
                    LocalizationUtil.createLocalizableMessageType(createDisplayInformation(target, false)), LocalizableMessageType.class);
            var.put(ExpressionConstants.VAR_EVALUATED_ASSIGNMENT, actx.evaluatedAssignment, EvaluatedAssignment.class);
            AssignmentType assignment = actx.evaluatedAssignment.getAssignment(actx.state == ObjectState.BEFORE);
            var.put(ExpressionConstants.VAR_ASSIGNMENT, assignment, AssignmentType.class);
        } else {
            SchemaRegistry schemaRegistry = PrismContext.get().getSchemaRegistry();
            var.put(ExpressionConstants.VAR_TARGET, null, getObjectTypeDefinition());
            var.put(ExpressionConstants.VAR_TARGET_DISPLAY_INFORMATION, null, LocalizableMessageType.class);
            var.put(ExpressionConstants.VAR_EVALUATED_ASSIGNMENT, null, EvaluatedAssignment.class);
            PrismContainerDefinition<AssignmentType> assignmentDef = schemaRegistry
                    .findObjectDefinitionByCompileTimeClass(AssignmentHolderType.class)
                        .findContainerDefinition(AssignmentHolderType.F_ASSIGNMENT);
            var.put(ExpressionConstants.VAR_ASSIGNMENT, null, assignmentDef);
        }
        var.put(VAR_RULE_EVALUATION_CONTEXT, rctx, PolicyRuleEvaluationContext.class);
        var.put(VAR_EVALUATOR_HELPER, this, ConstraintEvaluatorHelper.class);
        var.put(VAR_CONSTRAINT, constraintElement != null ? constraintElement.getValue() : null, AbstractPolicyConstraintType.class);
        var.put(VAR_CONSTRAINT_ELEMENT, constraintElement, JAXBElement.class);
        return var;
    }

    private PrismObjectDefinition<?> getObjectTypeDefinition() {
        return PrismContext.get().getSchemaRegistry().findObjectDefinitionByCompileTimeClass(ObjectType.class);
    }

    boolean evaluateBoolean(
            ExpressionType expressionBean, VariablesMap variablesMap,
            String contextDescription, PolicyRuleEvaluationContext<?> ctx, OperationResult result)
            throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        return LensExpressionUtil.evaluateBoolean(
                expressionBean, variablesMap, ctx.elementContext, contextDescription, ctx.task, result);
    }

    LocalizableMessageType evaluateLocalizableMessageType(
            ExpressionType expressionBean, VariablesMap variablesMap,
            String contextDescription, PolicyRuleEvaluationContext<?> ctx, OperationResult result)
            throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        return LensExpressionUtil.evaluateLocalizableMessageType(
                expressionBean, variablesMap, ctx.elementContext, contextDescription, ctx.task, result);
    }

    private SingleLocalizableMessageType interpretLocalizableMessageTemplate(
            LocalizableMessageTemplateType template,
            PolicyRuleEvaluationContext<?> rctx,
            JAXBElement<? extends AbstractPolicyConstraintType> constraintElement,
            OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        return LensExpressionUtil.interpretLocalizableMessageTemplate(
                template, createVariablesMap(rctx, constraintElement), rctx.elementContext, rctx.task, result);
    }

    public LocalizableMessage createLocalizableMessage(
            JAXBElement<? extends AbstractPolicyConstraintType> constraintElement,
            PolicyRuleEvaluationContext<?> rctx,
            LocalizableMessage builtInMessage,
            OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException,
            ConfigurationException, SecurityViolationException {
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

    public LocalizableMessage createLocalizableShortMessage(
            JAXBElement<? extends AbstractPolicyConstraintType> constraintElement,
            PolicyRuleEvaluationContext<?> rctx,
            LocalizableMessage builtInMessage,
            OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException,
            ConfigurationException, SecurityViolationException {
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

    LocalizableMessage createBeforeAfterMessage(PolicyRuleEvaluationContext<?> ctx) {
        return LocalizableMessageBuilder.buildKey(ctx.state == ObjectState.AFTER ?
                SchemaConstants.POLICY_CONSTRAINTS_AFTER_KEY : SchemaConstants.POLICY_CONSTRAINTS_BEFORE_KEY);
    }
}
