/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.activity.policy;

import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityPolicyConstraintsType;

@Component
public class ActivityPolicyConstraintsEvaluator {

    private static ActivityPolicyConstraintsEvaluator instance;

    @Autowired
    private ExecutionTimeConstraintEvaluator executionTimeEvaluator;

    @PostConstruct
    public void init() {
        instance = this;
    }

    public static ActivityPolicyConstraintsEvaluator get() {
        return instance;
    }

    public List<EvaluatedActivityPolicyRuleTrigger<?>> evaluateConstraints(
            ActivityPolicyConstraintsType constraints, ActivityPolicyRuleEvaluationContext context, OperationResult result) {

        if (constraints == null) {
            return List.of();
        }

        List<EvaluatedActivityPolicyRuleTrigger<?>> triggers = new ArrayList<>();

        if (constraints.getExecutionTime() != null) {
            triggers.addAll(executionTimeEvaluator.evaluate(constraints.getExecutionTime(), context, result));
        }

        return triggers;
    }
}
