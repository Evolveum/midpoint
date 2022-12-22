/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.policy.evaluators;

import static com.evolveum.midpoint.util.MiscUtil.schemaCheck;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyConstraintKindType.CUSTOM;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.JAXBElement;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.context.EvaluatedCustomConstraintTrigger;
import com.evolveum.midpoint.model.impl.lens.projector.policy.AssignmentPolicyRuleEvaluationContext;
import com.evolveum.midpoint.model.impl.lens.projector.policy.PolicyRuleEvaluationContext;
import com.evolveum.midpoint.model.impl.scripting.ScriptingExpressionEvaluator;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.LocalizableMessageBuilder;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CustomPolicyConstraintType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;

/**
 * TODO describe
 */
@Component
public class CustomConstraintEvaluator implements PolicyConstraintEvaluator<CustomPolicyConstraintType> {

    @SuppressWarnings("unused")
    private static final Trace LOGGER = TraceManager.getTrace(CustomConstraintEvaluator.class);

    private static final String OP_EVALUATE = CustomConstraintEvaluator.class.getName() + ".evaluate";

    private static final String OBJECT_CONSTRAINT_KEY_PREFIX = "custom.";
    private static final String ASSIGNMENT_CONSTRAINT_KEY_PREFIX = "custom.assignment.";
    private static final String KEY_NAMED = "named";
    private static final String KEY_UNNAMED = "unnamed";

    @Autowired protected ExpressionFactory expressionFactory;
    @Autowired protected ConstraintEvaluatorHelper evaluatorHelper;
    @Autowired protected ScriptingExpressionEvaluator scriptingExpressionEvaluator;

    @Override
    public <AH extends AssignmentHolderType> EvaluatedCustomConstraintTrigger evaluate(
            @NotNull JAXBElement<CustomPolicyConstraintType> constraint,
            @NotNull PolicyRuleEvaluationContext<AH> rctx,
            OperationResult parentResult)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException,
            ConfigurationException, SecurityViolationException {

        OperationResult result = parentResult.subresult(OP_EVALUATE)
                .setMinor()
                .build();
        try {

            CustomPolicyConstraintType constraintValue = constraint.getValue();
            ExpressionType expression = constraintValue.getExpression();
            schemaCheck(expression != null, "Expression must be present");

            if (evaluatorHelper.evaluateBoolean(
                    expression,
                    evaluatorHelper.createVariablesMap(rctx, constraint),
                    "expression in custom constraint " + constraintValue.getName(),
                    rctx, result)) {
                boolean onAssignment = rctx instanceof AssignmentPolicyRuleEvaluationContext;
                String keyPrefix = onAssignment ? ASSIGNMENT_CONSTRAINT_KEY_PREFIX : OBJECT_CONSTRAINT_KEY_PREFIX;
                return new EvaluatedCustomConstraintTrigger(
                        CUSTOM,
                        constraintValue,
                        createMessage(keyPrefix, constraint, rctx, onAssignment, result),
                        createShortMessage(keyPrefix, constraint, rctx, onAssignment, result));
            } else {
                return null;
            }
        } catch (Throwable t) {
            result.recordFatalError(t.getMessage(), t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    // TODO deduplicate with state constraint evaluation
    @NotNull
    private <AH extends AssignmentHolderType> LocalizableMessage createMessage(
            String constraintKeyPrefix,
            JAXBElement<CustomPolicyConstraintType> constraintElement,
            PolicyRuleEvaluationContext<AH> ctx,
            boolean assignmentTarget,
            OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        LocalizableMessage builtInMessage =
                createBuiltInMessage(
                        SchemaConstants.DEFAULT_POLICY_CONSTRAINT_KEY_PREFIX + constraintKeyPrefix,
                        constraintElement,
                        ctx,
                        assignmentTarget,
                        result);
        return evaluatorHelper.createLocalizableMessage(constraintElement, ctx, builtInMessage, result);
    }

    // TODO deduplicate with state constraint evaluation
    @NotNull
    private <AH extends AssignmentHolderType> LocalizableMessage createBuiltInMessage(String keyPrefix,
            JAXBElement<CustomPolicyConstraintType> constraintElement, PolicyRuleEvaluationContext<AH> ctx,
            boolean assignmentTarget, OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        CustomPolicyConstraintType constraint = constraintElement.getValue();
        List<Object> args = new ArrayList<>();
        args.add(evaluatorHelper.createBeforeAfterMessage(ctx));
        if (assignmentTarget) {
            addAssignmentTargetArgument(args, ctx);
        }
        String keySuffix;
        if (constraint.getName() != null) {
            args.add(constraint.getName());
            keySuffix = KEY_NAMED;
        } else {
            keySuffix = KEY_UNNAMED;
        }
        LocalizableMessage builtInMessage = new LocalizableMessageBuilder()
                .key(keyPrefix + keySuffix)
                .args(args)
                .build();
        return evaluatorHelper.createLocalizableMessage(constraintElement, ctx, builtInMessage, result);
    }

    private <AH extends AssignmentHolderType> void addAssignmentTargetArgument(
            List<Object> args, PolicyRuleEvaluationContext<AH> ctx) {
        if (!(ctx instanceof AssignmentPolicyRuleEvaluationContext)) {
            args.add("");
        } else {
            AssignmentPolicyRuleEvaluationContext<AH> actx = (AssignmentPolicyRuleEvaluationContext<AH>) ctx;
            args.add(ObjectTypeUtil.createDisplayInformation(actx.evaluatedAssignment.getTarget(), false));
        }
    }

    // TODO deduplicate with state constraint evaluation
    @NotNull
    private <AH extends AssignmentHolderType> LocalizableMessage createShortMessage(
            String constraintKeyPrefix,
            JAXBElement<CustomPolicyConstraintType> constraintElement,
            PolicyRuleEvaluationContext<AH> ctx,
            boolean assignmentTarget,
            OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        LocalizableMessage builtInMessage =
                createBuiltInMessage(
                        SchemaConstants.DEFAULT_POLICY_CONSTRAINT_SHORT_MESSAGE_KEY_PREFIX + constraintKeyPrefix,
                        constraintElement,
                        ctx,
                        assignmentTarget,
                        result);
        return evaluatorHelper.createLocalizableShortMessage(constraintElement, ctx, builtInMessage, result);
    }
}
