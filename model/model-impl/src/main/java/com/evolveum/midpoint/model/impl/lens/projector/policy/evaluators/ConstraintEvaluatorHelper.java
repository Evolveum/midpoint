/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.policy.evaluators;

import com.evolveum.midpoint.model.api.context.EvaluatedAssignment;
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

    public static final String VAR_EVALUATOR_HELPER = "evaluatorHelper";
    public static final String VAR_CONSTRAINT_ELEMENT = "constraintElement";
    public static final String VAR_CONSTRAINT = "constraint";

    @Autowired private PrismContext prismContext;
    @Autowired protected ExpressionFactory expressionFactory;

    // corresponds with PolicyRuleBasedAspect.processNameFromApprovalActions
    public <AH extends AssignmentHolderType> ExpressionVariables createExpressionVariables(PolicyRuleEvaluationContext<AH> rctx,
            JAXBElement<? extends AbstractPolicyConstraintType> constraintElement) {
        ExpressionVariables var = new ExpressionVariables();
        PrismObject<AH> object = rctx.getObject();
        var.put(ExpressionConstants.VAR_USER, object, object.getDefinition());
        var.put(ExpressionConstants.VAR_FOCUS, object, object.getDefinition());
        var.put(ExpressionConstants.VAR_OBJECT, object, object.getDefinition());
        var.put(ExpressionConstants.VAR_OBJECT_DISPLAY_INFORMATION,
                LocalizationUtil.createLocalizableMessageType(createDisplayInformation(object, false)), LocalizableMessageType.class);
        if (rctx instanceof AssignmentPolicyRuleEvaluationContext) {
            AssignmentPolicyRuleEvaluationContext actx = (AssignmentPolicyRuleEvaluationContext<AH>) rctx;
            PrismObject target = actx.evaluatedAssignment.getTarget();
            var.put(ExpressionConstants.VAR_TARGET, target, target.getDefinition());
            var.put(ExpressionConstants.VAR_TARGET_DISPLAY_INFORMATION,
                    LocalizationUtil.createLocalizableMessageType(createDisplayInformation(target, false)), LocalizableMessageType.class);
            var.put(ExpressionConstants.VAR_EVALUATED_ASSIGNMENT, actx.evaluatedAssignment, EvaluatedAssignment.class);
            AssignmentType assignmentType = actx.evaluatedAssignment.getAssignmentType(actx.state == ObjectState.BEFORE);
            var.put(ExpressionConstants.VAR_ASSIGNMENT, assignmentType, assignmentType.asPrismContainerValue().getDefinition());
        } else {
            PrismObjectDefinition<ObjectType> targetDef = rctx.lensContext.getPrismContext().getSchemaRegistry().findObjectDefinitionByCompileTimeClass(ObjectType.class);
            var.put(ExpressionConstants.VAR_TARGET, null, targetDef);
            var.put(ExpressionConstants.VAR_TARGET_DISPLAY_INFORMATION, null, LocalizableMessageType.class);
            var.put(ExpressionConstants.VAR_EVALUATED_ASSIGNMENT, null, EvaluatedAssignment.class);
            PrismContainerDefinition<AssignmentType> assignmentDef = rctx.lensContext.getPrismContext().getSchemaRegistry()
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

    public <AH extends AssignmentHolderType> SingleLocalizableMessageType interpretLocalizableMessageTemplate(LocalizableMessageTemplateType template,
            PolicyRuleEvaluationContext<AH> rctx, JAXBElement<? extends AbstractPolicyConstraintType> constraintElement, OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {
        return LensUtil.interpretLocalizableMessageTemplate(template, createExpressionVariables(rctx, constraintElement), expressionFactory, prismContext, rctx.task, result);
    }

    public <AH extends AssignmentHolderType> LocalizableMessage createLocalizableMessage(
            JAXBElement<? extends AbstractPolicyConstraintType> constraintElement, PolicyRuleEvaluationContext<AH> rctx,
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

    public <AH extends AssignmentHolderType> LocalizableMessage createLocalizableShortMessage(
            JAXBElement<? extends AbstractPolicyConstraintType> constraintElement, PolicyRuleEvaluationContext<AH> rctx,
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
