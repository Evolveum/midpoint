/*
 * Copyright (c) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.activity.policy.evaluator;

import java.util.List;
import java.util.Set;

import com.evolveum.midpoint.repo.common.policy.EvaluatedCompositeTrigger;
import com.evolveum.midpoint.repo.common.policy.EvaluatedPolicyRuleTrigger;
import com.evolveum.midpoint.schema.policy.PolicyConstraintKind;

import jakarta.annotation.PostConstruct;
import jakarta.xml.bind.JAXBElement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.repo.common.activity.policy.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyConstraintsType;

import static com.evolveum.midpoint.schema.policy.PolicyConstraintKind.*;

// TODO create package com.evolveum.midpoint.repo.common.policy and merge this class with
//  CompositeConstraintEvaluator (from model) it will be quite a lot of refactoring moving
//  stuff from model to repo-common. [viliam]
// TODO too much code duplication with CompositeConstraintEvaluator, refactor !!!! [viliam]
@Component
public class ActivityCompositeConstraintEvaluator
        implements ActivityPolicyConstraintEvaluator<PolicyConstraintsType, EvaluatedCompositeTrigger> {

    private static final String OP_EVALUATE = ActivityCompositeConstraintEvaluator.class.getName() + ".evaluate";

    private static ActivityCompositeConstraintEvaluator instance;

    @Autowired private ActivityPolicyConstraintsEvaluator activityPolicyConstraintsEvaluator;

    @PostConstruct
    public void init() {
        instance = this;
    }

    public static ActivityCompositeConstraintEvaluator get() {
        return instance;
    }

    @Override
    public List<EvaluatedCompositeTrigger> evaluate(
            JAXBElement<PolicyConstraintsType> constraint,
            ActivityPolicyRuleEvaluationContext context,
            OperationResult parentResult) {

        OperationResult result = parentResult.subresult(OP_EVALUATE)
                .setMinor()
                .build();
        try {
            boolean isAnd = QNameUtil.match(PolicyConstraintsType.F_AND, constraint.getName());
            boolean isOr = QNameUtil.match(PolicyConstraintsType.F_OR, constraint.getName());
            boolean isNot = QNameUtil.match(PolicyConstraintsType.F_NOT, constraint.getName());
            assert isAnd || isOr || isNot;
            List<EvaluatedPolicyRuleTrigger<?>> triggers =
                    activityPolicyConstraintsEvaluator.evaluateConstraints(constraint.getValue(), !isOr, context, result);
            EvaluatedCompositeTrigger rv;
            if (isNot) {
                if (triggers.isEmpty()) {
                    rv = createTrigger(NOT, constraint, triggers);
                } else {
                    rv = null;
                }
            } else {
                if (!triggers.isEmpty()) {
                    rv = createTrigger(isAnd ? AND : OR, constraint, triggers);
                } else {
                    rv = null;
                }
            }
            return rv != null ? List.of(rv) : List.of();
        } catch (Throwable t) {
            result.recordFatalError(t.getMessage(), t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    @Override
    public Set<DataNeed> getDataNeeds(JAXBElement<PolicyConstraintsType> constraint) {
        return activityPolicyConstraintsEvaluator.getDataNeeds(constraint.getValue());
    }

    private EvaluatedCompositeTrigger createTrigger(
            PolicyConstraintKind kind,
            JAXBElement<PolicyConstraintsType> element,
            List<EvaluatedPolicyRuleTrigger<?>> triggers) {
        return new EvaluatedCompositeTrigger(
                kind,
                element.getValue(),
                null, // TODO what with this? [pavol]
                null, // TODO what with this? [pavol]
                triggers);
    }
}
