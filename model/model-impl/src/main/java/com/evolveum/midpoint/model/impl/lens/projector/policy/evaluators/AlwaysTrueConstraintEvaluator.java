/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.policy.evaluators;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyConstraintKindType.ALWAYS_TRUE;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.JAXBElement;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.context.EvaluatedAlwaysTrueTrigger;
import com.evolveum.midpoint.model.impl.lens.projector.policy.ObjectPolicyRuleEvaluationContext;
import com.evolveum.midpoint.model.impl.lens.projector.policy.PolicyRuleEvaluationContext;
import com.evolveum.midpoint.model.impl.scripting.ScriptingExpressionEvaluator;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.LocalizableMessageBuilder;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AlwaysTruePolicyConstraintType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

@Component
@Experimental
public class AlwaysTrueConstraintEvaluator implements PolicyConstraintEvaluator<AlwaysTruePolicyConstraintType> {

    private static final String OP_EVALUATE = AlwaysTrueConstraintEvaluator.class.getName() + ".evaluate";

    private static final String CONSTRAINT_KEY_PREFIX = "alwaysTrue.";
    private static final String KEY_NAMED = "named";
    private static final String KEY_UNNAMED = "unnamed";

    @Autowired protected ExpressionFactory expressionFactory;
    @Autowired protected ConstraintEvaluatorHelper evaluatorHelper;
    @Autowired protected ScriptingExpressionEvaluator scriptingExpressionEvaluator;

    @Override
    public <O extends ObjectType> EvaluatedAlwaysTrueTrigger evaluate(
            @NotNull JAXBElement<AlwaysTruePolicyConstraintType> constraint,
            @NotNull PolicyRuleEvaluationContext<O> rctx,
            OperationResult parentResult)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException,
            ConfigurationException, SecurityViolationException {

        OperationResult result = parentResult.subresult(OP_EVALUATE)
                .setMinor()
                .build();
        try {
            if (rctx instanceof ObjectPolicyRuleEvaluationContext<?>) {
                return evaluateForObject(constraint, rctx, result);
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

    private EvaluatedAlwaysTrueTrigger evaluateForObject(
            @NotNull JAXBElement<AlwaysTruePolicyConstraintType> constraintElement,
            PolicyRuleEvaluationContext<?> ctx, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException,
            ConfigurationException, SecurityViolationException {

        return new EvaluatedAlwaysTrueTrigger(
                ALWAYS_TRUE, constraintElement.getValue(),
                createMessage(constraintElement, ctx, result),
                createShortMessage(constraintElement, ctx, result));
    }

    @NotNull
    private LocalizableMessage createMessage(
            @NotNull JAXBElement<AlwaysTruePolicyConstraintType> constraintElement,
            PolicyRuleEvaluationContext<?> ctx, OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        LocalizableMessage builtInMessage = createBuiltInMessage(
                SchemaConstants.DEFAULT_POLICY_CONSTRAINT_KEY_PREFIX + CONSTRAINT_KEY_PREFIX,
                constraintElement,
                ctx,
                result);
        return evaluatorHelper.createLocalizableMessage(constraintElement, ctx, builtInMessage, result);
    }

    @NotNull
    private LocalizableMessage createShortMessage(
            JAXBElement<AlwaysTruePolicyConstraintType> constraintElement,
            PolicyRuleEvaluationContext<?> ctx,
            OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        LocalizableMessage builtInMessage = createBuiltInMessage(
                SchemaConstants.DEFAULT_POLICY_CONSTRAINT_SHORT_MESSAGE_KEY_PREFIX + CONSTRAINT_KEY_PREFIX,
                constraintElement,
                ctx,
                result);
        return evaluatorHelper.createLocalizableShortMessage(constraintElement, ctx, builtInMessage, result);
    }

    @NotNull
    private LocalizableMessage createBuiltInMessage(
            String keyPrefix,
            JAXBElement<AlwaysTruePolicyConstraintType> constraintElement,
            PolicyRuleEvaluationContext<?> ctx,
            OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        AlwaysTruePolicyConstraintType constraint = constraintElement.getValue();
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
                .key(keyPrefix + keySuffix)
                .args(args)
                .build();
        return evaluatorHelper.createLocalizableMessage(constraintElement, ctx, builtInMessage, result);
    }
}
