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

import jakarta.annotation.PostConstruct;
import jakarta.xml.bind.JAXBElement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@Component
public class ActivityPolicyConstraintsEvaluator {

    private static ActivityPolicyConstraintsEvaluator instance;

    @Autowired private ExecutionTimeConstraintEvaluator executionTimeEvaluator;

    @Autowired private ItemStateConstraintEvaluator itemStateEvaluator;

    @Autowired private RestartActivityConstraintEvaluator restartActivityEvaluator;

    @Autowired private ActivityCompositeConstraintEvaluator compositeEvaluator;

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

        List<JAXBElement<AbstractPolicyConstraintType>> toConstraintList = toConstraintList(constraints);
        for (JAXBElement<AbstractPolicyConstraintType> element : toConstraintList) {
            ActivityPolicyConstraintEvaluator evaluator = findEvaluator(element);

            List<? extends EvaluatedActivityPolicyRuleTrigger<?>> evaluatedTriggers =
                    evaluator.evaluate(element.getValue(), context, result);
            triggers.addAll(evaluatedTriggers);
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
            if (ActivityPolicyConstraintsType.F_EXECUTION_TIME.equals(constraint.getName())) {
                return executionTimeEvaluator;
            }
        } else if (constraint instanceof NumericThresholdPolicyConstraintType) {
            if (ActivityPolicyConstraintsType.F_RESTART_ACTIVITY.equals(constraint.getName())) {
                return restartActivityEvaluator;
            }
        } else if (constraint instanceof ItemStatePolicyConstraintType) {
            return itemStateEvaluator;
        }

        throw new IllegalArgumentException("No evaluator found for constraint type: " + constraint.getClass());
    }
}
