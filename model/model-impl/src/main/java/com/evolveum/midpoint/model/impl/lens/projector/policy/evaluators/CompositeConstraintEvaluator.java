/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.impl.lens.projector.policy.evaluators;

import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.repo.common.activity.policy.evaluator.ActivityCompositeConstraintEvaluator;
import com.evolveum.midpoint.schema.policy.PolicyConstraintKind;

import jakarta.annotation.PostConstruct;
import jakarta.xml.bind.JAXBElement;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.repo.common.policy.EvaluatedCompositeTrigger;
import com.evolveum.midpoint.model.impl.lens.projector.policy.PolicyRuleEvaluationContext;
import com.evolveum.midpoint.repo.common.policy.EvaluatedPolicyRuleTrigger;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.LocalizableMessageBuilder;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyConstraintsType;

import static com.evolveum.midpoint.schema.policy.PolicyConstraintKind.*;

/** TODO deduplicate with {@link ActivityCompositeConstraintEvaluator} */
@Component
public class CompositeConstraintEvaluator implements PolicyConstraintEvaluator<PolicyConstraintsType, EvaluatedCompositeTrigger> {

    private static final String OP_EVALUATE = CompositeConstraintEvaluator.class.getName() + ".evaluate";

    @Autowired private ConstraintEvaluatorHelper evaluatorHelper;
    @Autowired private PolicyConstraintsEvaluator policyConstraintsEvaluator;

    private static CompositeConstraintEvaluator instance;

    @PostConstruct
    public void init() {
        instance = this;
    }

    public static CompositeConstraintEvaluator get() {
        return instance;
    }

    @Override
    public @NotNull <O extends ObjectType> Collection<EvaluatedCompositeTrigger> evaluate(
            @NotNull JAXBElement<PolicyConstraintsType> constraint,
            @NotNull PolicyRuleEvaluationContext<O> rctx,
            @NonNull OperationResult parentResult)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException,
            ConfigurationException, SecurityViolationException {

        OperationResult result = parentResult.subresult(OP_EVALUATE)
                .setMinor()
                .build();
        try {
            boolean isAnd = QNameUtil.match(PolicyConstraintsType.F_AND, constraint.getName());
            boolean isOr = QNameUtil.match(PolicyConstraintsType.F_OR, constraint.getName());
            boolean isNot = QNameUtil.match(PolicyConstraintsType.F_NOT, constraint.getName());
            assert isAnd || isOr || isNot;
            List<EvaluatedPolicyRuleTrigger<?>> triggers =
                    policyConstraintsEvaluator.evaluateConstraints(constraint.getValue(), !isOr, rctx, result);
            EvaluatedCompositeTrigger rv;
            if (isNot) {
                if (triggers.isEmpty()) {
                    rv = createTrigger(NOT, constraint, triggers, rctx, result);
                } else {
                    rv = null;
                }
            } else {
                if (!triggers.isEmpty()) {
                    rv = createTrigger(isAnd ? AND : OR, constraint, triggers, rctx, result);
                } else {
                    rv = null;
                }
            }
            if (rv != null) {
                result.addReturn("trigger", rv.toDiagShortcut());
            }
            return rv != null ? List.of(rv) : List.of();
        } catch (Throwable t) {
            result.recordFatalError(t.getMessage(), t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    @NotNull
    private EvaluatedCompositeTrigger createTrigger(
            PolicyConstraintKind kind,
            JAXBElement<PolicyConstraintsType> constraintElement,
            List<EvaluatedPolicyRuleTrigger<?>> triggers,
            PolicyRuleEvaluationContext<?> rctx,
            OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        return new EvaluatedCompositeTrigger(
                kind,
                constraintElement.getValue(),
                createMessage(kind, constraintElement, rctx, result),
                createShortMessage(kind, constraintElement, rctx, result),
                triggers);
    }

    private LocalizableMessage createMessage(
            PolicyConstraintKind kind, JAXBElement<PolicyConstraintsType> constraintElement,
            PolicyRuleEvaluationContext<?> ctx, OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        LocalizableMessage builtInMessage = new LocalizableMessageBuilder()
                .key(SchemaConstants.DEFAULT_POLICY_CONSTRAINT_KEY_PREFIX + kind.getSerializableVersion().value())
                .build();
        return evaluatorHelper.createLocalizableMessage(constraintElement, ctx, builtInMessage, result);
    }

    private LocalizableMessage createShortMessage(
            PolicyConstraintKind kind, JAXBElement<PolicyConstraintsType> constraintElement,
            PolicyRuleEvaluationContext<?> ctx, OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        LocalizableMessage builtInMessage = new LocalizableMessageBuilder()
                .key(SchemaConstants.DEFAULT_POLICY_CONSTRAINT_SHORT_MESSAGE_KEY_PREFIX + kind.getSerializableVersion().value())
                .build();
        return evaluatorHelper.createLocalizableShortMessage(constraintElement, ctx, builtInMessage, result);
    }
}
