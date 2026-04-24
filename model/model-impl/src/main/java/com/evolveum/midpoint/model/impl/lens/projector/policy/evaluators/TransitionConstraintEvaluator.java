/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.impl.lens.projector.policy.evaluators;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import jakarta.xml.bind.JAXBElement;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.context.EvaluatedTransitionTrigger;
import com.evolveum.midpoint.model.impl.lens.projector.policy.ObjectState;
import com.evolveum.midpoint.model.impl.lens.projector.policy.PolicyRuleEvaluationContext;
import com.evolveum.midpoint.repo.common.policy.EvaluatedPolicyRuleTrigger;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.LocalizableMessageBuilder;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TransitionPolicyConstraintType;

@Component
public class TransitionConstraintEvaluator
        implements PolicyConstraintEvaluator<TransitionPolicyConstraintType, EvaluatedTransitionTrigger> {

    private static final String OP_EVALUATE = TransitionConstraintEvaluator.class.getName() + ".evaluate";

    private static final String CONSTRAINT_KEY = "transition";

    @Autowired private ConstraintEvaluatorHelper evaluatorHelper;
    @Autowired private PolicyConstraintsEvaluator policyConstraintsEvaluator;

    @Override
    public @NotNull <O extends ObjectType> Collection<EvaluatedTransitionTrigger> evaluate(
            @NotNull JAXBElement<TransitionPolicyConstraintType> constraintElement,
            @NotNull PolicyRuleEvaluationContext<O> rctx,
            @NonNull OperationResult parentResult)
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
                return List.of(
                        new EvaluatedTransitionTrigger(
                                trans,
                                createMessage(constraintElement, rctx, result),
                                createShortMessage(constraintElement, rctx, result),
                                triggers));
            } else {
                return List.of();
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
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        if (expected == null) {
            return true;
        }
        PolicyRuleEvaluationContext<O> subContext = rctx.cloneWithStateConstraints(state);
        List<EvaluatedPolicyRuleTrigger<?>> subTriggers =
                policyConstraintsEvaluator.evaluateConstraints(trans.getConstraints(), true, subContext, result);
        triggers.addAll(subTriggers);
        boolean real = !subTriggers.isEmpty();
        return expected == real;
    }

    private LocalizableMessage createMessage(
            JAXBElement<TransitionPolicyConstraintType> constraintElement,
            PolicyRuleEvaluationContext<?> ctx,
            OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        LocalizableMessage builtInMessage = new LocalizableMessageBuilder()
                .key(SchemaConstants.DEFAULT_POLICY_CONSTRAINT_KEY_PREFIX + CONSTRAINT_KEY)
                .build();
        return evaluatorHelper.createLocalizableMessage(constraintElement, ctx, builtInMessage, result);
    }

    private LocalizableMessage createShortMessage(
            JAXBElement<TransitionPolicyConstraintType> constraintElement,
            PolicyRuleEvaluationContext<?> ctx,
            OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        LocalizableMessage builtInMessage = new LocalizableMessageBuilder()
                .key(SchemaConstants.DEFAULT_POLICY_CONSTRAINT_SHORT_MESSAGE_KEY_PREFIX + CONSTRAINT_KEY)
                .build();
        return evaluatorHelper.createLocalizableShortMessage(constraintElement, ctx, builtInMessage, result);
    }
}
