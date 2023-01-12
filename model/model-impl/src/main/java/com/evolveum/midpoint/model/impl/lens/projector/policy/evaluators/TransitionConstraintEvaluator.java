/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.policy.evaluators;

import com.evolveum.midpoint.model.api.context.EvaluatedPolicyRuleTrigger;
import com.evolveum.midpoint.model.api.context.EvaluatedTransitionTrigger;
import com.evolveum.midpoint.model.impl.lens.projector.policy.ObjectState;
import com.evolveum.midpoint.model.impl.lens.projector.policy.PolicyRuleEvaluationContext;
import com.evolveum.midpoint.model.impl.lens.projector.policy.PolicyRuleProcessor;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.LocalizableMessageBuilder;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyConstraintKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TransitionPolicyConstraintType;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.bind.JAXBElement;
import java.util.ArrayList;
import java.util.List;

@Component
public class TransitionConstraintEvaluator implements PolicyConstraintEvaluator<TransitionPolicyConstraintType> {

    private static final String OP_EVALUATE = TransitionConstraintEvaluator.class.getName() + ".evaluate";

    private static final String CONSTRAINT_KEY = "transition";

    @Autowired private ConstraintEvaluatorHelper evaluatorHelper;
    @Autowired private PolicyRuleProcessor policyRuleProcessor;

    @Override
    public <O extends ObjectType> EvaluatedPolicyRuleTrigger<?> evaluate(
            @NotNull JAXBElement<TransitionPolicyConstraintType> constraintElement,
            @NotNull PolicyRuleEvaluationContext<O> rctx,
            OperationResult parentResult)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException,
            ConfigurationException, SecurityViolationException {

        OperationResult result = parentResult.subresult(OP_EVALUATE)
                .setMinor()
                .build();
        try {
            TransitionPolicyConstraintType trans = constraintElement.getValue();
            List<EvaluatedPolicyRuleTrigger<?>> triggers = new ArrayList<>();
            boolean match =
                    evaluateState(trans, rctx, ObjectState.BEFORE, trans.isStateBefore(), triggers, result)
                            && evaluateState(trans, rctx, ObjectState.AFTER, trans.isStateAfter(), triggers, result);
            if (match) {
                return new EvaluatedTransitionTrigger(PolicyConstraintKindType.TRANSITION, trans,
                        createMessage(constraintElement, rctx, result),
                        createShortMessage(constraintElement, rctx, result),
                        triggers);
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

    private <O extends ObjectType> boolean evaluateState(
            TransitionPolicyConstraintType trans,
            PolicyRuleEvaluationContext<O> rctx, ObjectState state, Boolean expected,
            List<EvaluatedPolicyRuleTrigger<?>> triggers, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {
        if (expected == null) {
            return true;
        }
        PolicyRuleEvaluationContext<O> subContext = rctx.cloneWithStateConstraints(state);
        List<EvaluatedPolicyRuleTrigger<?>> subTriggers = policyRuleProcessor
                .evaluateConstraints(trans.getConstraints(), true, subContext, result);
        triggers.addAll(subTriggers);
        boolean real = !subTriggers.isEmpty();
        return expected == real;
    }

    private LocalizableMessage createMessage(JAXBElement<TransitionPolicyConstraintType> constraintElement, PolicyRuleEvaluationContext<?> ctx, OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {
        LocalizableMessage builtInMessage = new LocalizableMessageBuilder()
                .key(SchemaConstants.DEFAULT_POLICY_CONSTRAINT_KEY_PREFIX + CONSTRAINT_KEY)
                .build();
        return evaluatorHelper.createLocalizableMessage(constraintElement, ctx, builtInMessage, result);
    }

    private LocalizableMessage createShortMessage(JAXBElement<TransitionPolicyConstraintType> constraintElement, PolicyRuleEvaluationContext<?> ctx, OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {
        LocalizableMessage builtInMessage = new LocalizableMessageBuilder()
                .key(SchemaConstants.DEFAULT_POLICY_CONSTRAINT_SHORT_MESSAGE_KEY_PREFIX + CONSTRAINT_KEY)
                .build();
        return evaluatorHelper.createLocalizableShortMessage(constraintElement, ctx, builtInMessage, result);
    }
}
