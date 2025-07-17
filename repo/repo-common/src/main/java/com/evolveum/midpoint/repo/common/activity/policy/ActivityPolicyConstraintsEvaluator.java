/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.policy;

import java.util.ArrayList;
import java.util.List;
import javax.xml.namespace.QName;

import jakarta.xml.bind.JAXBElement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@Component
public class ActivityPolicyConstraintsEvaluator {

    @Autowired private ExecutionTimeConstraintEvaluator executionTimeEvaluator;

    @Autowired private ItemStateConstraintEvaluator itemStateEvaluator;

    @Autowired private RestartActivityConstraintEvaluator restartActivityEvaluator;

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
        if (constraints.getRestartActivity() != null) {
            list.add(createJAXBElement(ActivityPolicyConstraintsType.F_RESTART_ACTIVITY, constraints.getRestartActivity()));
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
            if (ActivityPolicyConstraintsType.F_RESTART_ACTIVITY.equals(element.getName())) {
                return restartActivityEvaluator;
            }
        } else if (constraint instanceof ItemStatePolicyConstraintType) {
            return itemStateEvaluator;
        } else if (constraint instanceof PolicyConstraintsType) {
            return compositeEvaluator;
        }

        throw new IllegalArgumentException("No evaluator found for constraint type: " + constraint.getClass());
    }
}
