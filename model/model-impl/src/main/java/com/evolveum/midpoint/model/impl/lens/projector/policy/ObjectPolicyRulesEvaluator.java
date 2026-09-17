/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.impl.lens.projector.policy;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.api.context.DirectlyEvaluatedClockworkPolicyRule;
import com.evolveum.midpoint.model.impl.lens.DirectlyEvaluatedClockworkPolicyRuleImpl;
import com.evolveum.midpoint.model.impl.lens.LensElementContext;
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.prism.delta.PlusMinusZero;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Evaluates policy rules attached to the objects represented by {@link LensElementContext} (focus and projections).
 */
abstract class ObjectPolicyRulesEvaluator<O extends ObjectType> extends PolicyRuleEvaluator {

    private static final Trace LOGGER = TraceManager.getTrace(ObjectPolicyRulesEvaluator.class);

    @NotNull private final LensElementContext<O> elementContext;

    /** Selects focus vs. projection rules. */
    @NotNull private final Predicate<DirectlyEvaluatedClockworkPolicyRule> ruleSelector;

    ObjectPolicyRulesEvaluator(
            @NotNull LensElementContext<O> elementContext,
            @NotNull Task task,
            @NotNull Predicate<DirectlyEvaluatedClockworkPolicyRule> ruleSelector) {
        super(elementContext.getLensContext(), task);
        this.elementContext = elementContext;
        this.ruleSelector = ruleSelector;
    }

    void evaluate(OperationResult result)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, SecurityViolationException,
            ConfigurationException, CommunicationException, SubscriptionComplianceException {

        collector.initialize(result);
        List<DirectlyEvaluatedClockworkPolicyRuleImpl> rules = collector.collectObjectRules(result);

        LOGGER.trace("Selecting rules from {} object-attached policy rules", rules.size());
        List<DirectlyEvaluatedClockworkPolicyRuleImpl> applicableRules = selectAndSetApplicableRules(rules);
        evaluateCollectedRules(applicableRules, result);
    }

    private @NotNull List<DirectlyEvaluatedClockworkPolicyRuleImpl> selectAndSetApplicableRules(
            List<DirectlyEvaluatedClockworkPolicyRuleImpl> rules) {
        List<DirectlyEvaluatedClockworkPolicyRuleImpl> applicableRules = new ArrayList<>();
        for (DirectlyEvaluatedClockworkPolicyRuleImpl rule : rules) {
            if (ruleSelector.test(rule)) {
                applicableRules.add(rule);
            } else {
                LOGGER.trace("Rule '{}' is not applicable to the focus/projection, skipping: {} (selecting {})",
                        rule.getName(), rule, ruleSelector);
            }
        }
        elementContext.setObjectPolicyRules(applicableRules);
        return applicableRules;
    }

    private void evaluateCollectedRules(
            List<DirectlyEvaluatedClockworkPolicyRuleImpl> applicableRules, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException,
            ConfigurationException, SecurityViolationException, SubscriptionComplianceException {
        LOGGER.trace("Evaluating {} applicable rules", applicableRules.size());
        List<ObjectPolicyRuleEvaluationContext<O>> contextsToEvaluate =
                applicableRules.stream()
                        .map(rule -> new ObjectPolicyRuleEvaluationContext<>(rule, elementContext, task))
                        .collect(Collectors.toList());
        evaluateRules(contextsToEvaluate, result);
    }

    @Override
    void record(OperationResult result) throws SchemaException {
        List<DirectlyEvaluatedClockworkPolicyRuleImpl> rulesToRecord =
                selectRulesToRecord(elementContext.getObjectPolicyRules()).stream()
                        .filter(rule -> !isFromDeletedAssignment(rule))
                        .toList();
        new PolicyStateRecorder().applyObjectState(elementContext, rulesToRecord);
    }

    /**
     * Rules coming from assignments that are being deleted are evaluated (e.g. to run scripts on unassignment,
     * see {@link PolicyRulesCollector#collectObjectRules(OperationResult)}), but they must not be recorded into
     * the object: the resulting object no longer has the assignment, so its marks, situations and triggered
     * rules do not apply to it. See #10641.
     */
    private boolean isFromDeletedAssignment(DirectlyEvaluatedClockworkPolicyRuleImpl rule) {
        var originatingAssignment = rule.getOriginatingAssignment();
        if (originatingAssignment == null) {
            return false;
        }
        // The origin covers also assignments deleted in previous waves: those are evaluated again in later waves
        // (with an unchanged "no change" item, so the mode alone is not sufficient). Compare with the handling
        // of assignment-related policy state in PolicyStateRecorder.
        boolean deleted = originatingAssignment.isBeingDeleted()
                || originatingAssignment.getMode() == PlusMinusZero.MINUS;
        if (deleted) {
            LOGGER.trace("Not recording rule '{}' as its originating assignment is being deleted: {}",
                    rule.getName(), originatingAssignment);
        }
        return deleted;
    }

    /** Evaluates object policy rules attached to the focus. */
    static class FocusPolicyRulesEvaluator<F extends AssignmentHolderType> extends ObjectPolicyRulesEvaluator<F> {
        FocusPolicyRulesEvaluator(@NotNull LensFocusContext<F> focusContext, @NotNull Task task) {
            super(focusContext, task, new Predicate<>() {
                @Override
                public boolean test(DirectlyEvaluatedClockworkPolicyRule rule) {
                    return rule.isApplicableToFocusObject();
                }

                @Override
                public String toString() {
                    return "rules applicable to focus (as an object)";
                }
            });
        }
    }

    /** Evaluates object policy rules attached to projections. */
    static class ProjectionPolicyRulesEvaluator extends ObjectPolicyRulesEvaluator<ShadowType> {
        ProjectionPolicyRulesEvaluator(@NotNull LensProjectionContext projectionContext, @NotNull Task task) {
            super(projectionContext, task, new Predicate<>() {
                @Override
                public boolean test(DirectlyEvaluatedClockworkPolicyRule rule) {
                    return rule.isApplicableToProjection();
                }

                @Override
                public String toString() {
                    return "rules applicable to projection";
                }
            });
        }
    }
}
