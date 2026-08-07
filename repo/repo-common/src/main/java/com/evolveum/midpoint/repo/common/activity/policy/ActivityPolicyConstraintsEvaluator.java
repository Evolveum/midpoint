/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.activity.policy;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.xml.bind.JAXBElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.repo.common.activity.policy.evaluator.ActivityCompositeConstraintEvaluator;
import com.evolveum.midpoint.repo.common.activity.policy.evaluator.ExecutionAttemptsConstraintEvaluator;
import com.evolveum.midpoint.repo.common.activity.policy.evaluator.ExecutionTimeConstraintEvaluator;
import com.evolveum.midpoint.repo.common.activity.policy.evaluator.ItemProcessingResultConstraintEvaluator;
import com.evolveum.midpoint.repo.common.policy.EvaluatedPolicyRuleTrigger;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.PolicyRuleTypeUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Evaluates policy constraints in the activity context.
 *
 * Only some constraints are supported here:
 * {@link PolicyConstraintsType#F_EXECUTION_TIME},
 * {@link PolicyConstraintsType#F_EXECUTION_ATTEMPTS},
 * {@link PolicyConstraintsType#F_ITEM_PROCESSING_RESULT}
 * and the logical constraints ({@link PolicyConstraintsType#F_AND}, {@link PolicyConstraintsType#F_OR},
 * {@link PolicyConstraintsType#F_NOT}) composed of them.
 *
 * All other constraints are skipped: activity policies share {@link PolicyConstraintsType} with focus policy rules,
 * so a rule may well contain constraints that are meaningful only in the focus context. Skipped constraints do not
 * participate in the evaluation at all - in particular, they do not veto the implicit/explicit conjunction.
 */
@Component
public class ActivityPolicyConstraintsEvaluator {

    private static final Trace LOGGER = TraceManager.getTrace(ActivityPolicyConstraintsEvaluator.class);

    @Autowired private ExecutionTimeConstraintEvaluator executionTimeEvaluator;

    @Autowired private ItemProcessingResultConstraintEvaluator itemStateEvaluator;

    @Autowired private ExecutionAttemptsConstraintEvaluator executionAttemptsConstraintEvaluator;

    @Autowired private ActivityCompositeConstraintEvaluator compositeEvaluator;

    public List<EvaluatedPolicyRuleTrigger<?>> evaluateConstraints(
            PolicyConstraintsType constraintsBean,
            boolean allMustApply,
            ActivityPolicyRuleEvaluationContext context,
            OperationResult result) {

        if (constraintsBean == null) {
            return List.of();
        }

        List<EvaluatedPolicyRuleTrigger<?>> triggers = new ArrayList<>();

        for (JAXBElement<AbstractPolicyConstraintType> element : toConstraintList(constraintsBean)) {
            //noinspection unchecked
            ActivityPolicyConstraintEvaluator<AbstractPolicyConstraintType, ?> evaluator =
                    (ActivityPolicyConstraintEvaluator<AbstractPolicyConstraintType, ?>) findEvaluator(element);
            if (evaluator == null) {
                continue; // constraint not relevant in the activity context, does not affect the evaluation
            }

            List<? extends EvaluatedPolicyRuleTrigger<?>> newTriggers = evaluator.evaluate(element, context, result);
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

    /** Returns information about data needed to evaluate a particular (potentially composite) constraint. */
    public @NotNull Set<DataNeed> getDataNeeds(PolicyConstraintsType constraintsBean) {
        Set<DataNeed> dataNeeds = new HashSet<>();
        for (JAXBElement<AbstractPolicyConstraintType> element : toConstraintList(constraintsBean)) {
            //noinspection unchecked
            ActivityPolicyConstraintEvaluator<AbstractPolicyConstraintType, ?> evaluator =
                    (ActivityPolicyConstraintEvaluator<AbstractPolicyConstraintType, ?>) findEvaluator(element);
            if (evaluator == null) {
                continue;
            }
            dataNeeds.addAll(evaluator.getDataNeeds(element));
        }
        return dataNeeds;
    }

    private List<JAXBElement<AbstractPolicyConstraintType>> toConstraintList(PolicyConstraintsType constraints) {
        // Unresolved constraint references are ignored here, just like other unsupported constraints.
        return PolicyRuleTypeUtil.toConstraintsList(constraints, true);
    }

    private @Nullable ActivityPolicyConstraintEvaluator<?, ?> findEvaluator(JAXBElement<AbstractPolicyConstraintType> element) {
        AbstractPolicyConstraintType constraint = element.getValue();

        if (constraint instanceof DurationThresholdPolicyConstraintType) {
            if (PolicyConstraintsType.F_EXECUTION_TIME.equals(element.getName())) {
                return executionTimeEvaluator;
            }
        } else if (constraint instanceof NumericThresholdPolicyConstraintType) {
            if (PolicyConstraintsType.F_EXECUTION_ATTEMPTS.equals(element.getName())) {
                return executionAttemptsConstraintEvaluator;
            }
        } else if (constraint instanceof ItemProcessingResultPolicyConstraintType) {
            return itemStateEvaluator;
        } else if (constraint instanceof PolicyConstraintsType) {
            return compositeEvaluator;
        }

        LOGGER.trace("Constraint {} ({}) is not supported in the activity context, skipping",
                element.getName(), constraint.getClass().getSimpleName());
        return null;
    }
}
