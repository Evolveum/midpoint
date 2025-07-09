/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.policy;

import java.util.List;
import javax.xml.namespace.QName;

import jakarta.annotation.PostConstruct;
import jakarta.xml.bind.JAXBElement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityPolicyConstraintsType;

// TODO create package com.evolveum.midpoint.repo.common.policy and merge this class with
//  CompositeConstraintEvaluator (from model) it will be quite a lot of refactoring moving
//  stuff from model to repo-common. [viliam]
// TODO too much code duplication with CompositeConstraintEvaluator, refactor !!!! [viliam]
@Component
public class ActivityCompositeConstraintEvaluator
        implements ActivityPolicyConstraintEvaluator<ActivityPolicyConstraintsType, ActivityCompositeTrigger> {

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
    public List<ActivityCompositeTrigger> evaluate(
            JAXBElement<ActivityPolicyConstraintsType> constraint, ActivityPolicyRuleEvaluationContext context, OperationResult parentResult) {

        OperationResult result = parentResult.subresult(OP_EVALUATE)
                .setMinor()
                .build();
        try {
            boolean isAnd = QNameUtil.match(ActivityPolicyConstraintsType.F_AND, constraint.getName());
            boolean isOr = QNameUtil.match(ActivityPolicyConstraintsType.F_OR, constraint.getName());
            boolean isNot = QNameUtil.match(ActivityPolicyConstraintsType.F_NOT, constraint.getName());
            assert isAnd || isOr || isNot;
            List<EvaluatedActivityPolicyRuleTrigger<?>> triggers =
                    activityPolicyConstraintsEvaluator.evaluateConstraints(constraint.getValue(), !isOr, context, result);
            ActivityCompositeTrigger rv;
            if (isNot) {
                if (triggers.isEmpty()) {
                    rv = createTrigger(ActivityPolicyConstraintsType.F_NOT, constraint, triggers);
                } else {
                    rv = null;
                }
            } else {
                if (!triggers.isEmpty()) {
                    rv = createTrigger(
                            isAnd ? ActivityPolicyConstraintsType.F_AND : ActivityPolicyConstraintsType.F_OR,
                            constraint,
                            triggers);
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

    private ActivityCompositeTrigger createTrigger(
            QName kind,
            JAXBElement<ActivityPolicyConstraintsType> element,
            List<EvaluatedActivityPolicyRuleTrigger<?>> triggers) {
        return new ActivityCompositeTrigger(kind, element.getValue(), triggers);
    }
}
