/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.activity.policy;

import java.util.ArrayList;
import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.repo.common.activity.policy.evaluator.ActivityCompositeConstraintEvaluator;
import com.evolveum.midpoint.repo.common.activity.policy.evaluator.ExecutionTimeConstraintEvaluator;
import com.evolveum.midpoint.repo.common.activity.policy.evaluator.ItemStateConstraintEvaluator;
import com.evolveum.midpoint.repo.common.activity.policy.evaluator.ExecutionAttemptsConstraintEvaluator;
import jakarta.xml.bind.JAXBElement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@Component
public class ActivityPolicyConstraintsEvaluator {

    @Autowired private ExecutionTimeConstraintEvaluator executionTimeEvaluator;

    @Autowired private ItemStateConstraintEvaluator itemStateEvaluator;

    @Autowired private ExecutionAttemptsConstraintEvaluator executionAttemptsConstraintEvaluator;

    @Autowired private ActivityCompositeConstraintEvaluator compositeEvaluator;

    public List<EvaluatedActivityPolicyRuleTrigger<?>> evaluateConstraints(
            ActivityPolicyConstraintsType constraints, boolean allMustApply, ActivityPolicyRuleEvaluationContext context, OperationResult result) {

        if (constraints == null) {
            return List.of();
        }

        List<EvaluatedActivityPolicyRuleTrigger<?>> triggers = new ArrayList<>();

        List<JAXBElement<AbstractPolicyConstraintType>> toConstraintList = toConstraintList(constraints);
        for (JAXBElement<AbstractPolicyConstraintType> element : toConstraintList) {
            ActivityPolicyConstraintEvaluator evaluator = findEvaluator(element);

            List<? extends EvaluatedActivityPolicyRuleTrigger<?>> newTriggers = evaluator.evaluate(element, context, result);
            if (!newTriggers.isEmpty()) {
                triggers.addAll(newTriggers);
            } else {
                if (allMustApply) {
                    // If we require all constraints to apply, and this one does not, we can stop evaluating.
                    return List.of();
                }
            }
        }

        return triggers;
    }

    private List<JAXBElement<AbstractPolicyConstraintType>> toConstraintList(ActivityPolicyConstraintsType constraints) {
        List<JAXBElement<AbstractPolicyConstraintType>> list = new ArrayList<>();
        if (constraints.getExecutionTime() != null) {
            list.add(createJAXBElement(ActivityPolicyConstraintsType.F_EXECUTION_TIME, constraints.getExecutionTime()));
        }
        if (constraints.getItemState() != null) {
            list.add(createJAXBElement(ActivityPolicyConstraintsType.F_ITEM_STATE, constraints.getItemState()));
        }
        if (constraints.getExecutionAttempts() != null) {
            list.add(createJAXBElement(ActivityPolicyConstraintsType.F_EXECUTION_ATTEMPTS, constraints.getExecutionAttempts()));
        }
        return list;
    }

    private JAXBElement<AbstractPolicyConstraintType> createJAXBElement(QName name, AbstractPolicyConstraintType constraint) {
        return new JAXBElement<>(name, AbstractPolicyConstraintType.class, constraint);
    }

    private ActivityPolicyConstraintEvaluator<?, ?> findEvaluator(JAXBElement<AbstractPolicyConstraintType> element) {
        AbstractPolicyConstraintType constraint = element.getValue();

        if (constraint instanceof DurationThresholdPolicyConstraintType) {
            if (ActivityPolicyConstraintsType.F_EXECUTION_TIME.equals(element.getName())) {
                return executionTimeEvaluator;
            }
        } else if (constraint instanceof NumericThresholdPolicyConstraintType) {
            if (ActivityPolicyConstraintsType.F_EXECUTION_ATTEMPTS.equals(element.getName())) {
                return executionAttemptsConstraintEvaluator;
            }
        } else if (constraint instanceof ItemStatePolicyConstraintType) {
            return itemStateEvaluator;
        } else if (constraint instanceof PolicyConstraintsType) {
            return compositeEvaluator;
        }

        throw new IllegalArgumentException("No evaluator found for constraint type: " + constraint.getClass());
    }
}
