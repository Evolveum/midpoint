/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.policy;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.api.context.EvaluatedPolicyRule;
import com.evolveum.midpoint.model.impl.lens.EvaluatedPolicyRuleImpl;
import com.evolveum.midpoint.model.impl.lens.LensElementContext;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * Evaluates policy rules attached to the objects represented by {@link LensElementContext} (focus and projections).
 */
abstract class ObjectPolicyRulesEvaluator<O extends ObjectType> extends PolicyRuleEvaluator {

    private static final Trace LOGGER = TraceManager.getTrace(ObjectPolicyRulesEvaluator.class);

    @NotNull private final LensElementContext<O> elementContext;
    @NotNull private final Predicate<EvaluatedPolicyRule> ruleSelector;

    ObjectPolicyRulesEvaluator(
            @NotNull LensElementContext<O> elementContext,
            @NotNull Task task,
            @NotNull Predicate<EvaluatedPolicyRule> ruleSelector) {
        super(elementContext.getLensContext(), task);
        this.elementContext = elementContext;
        this.ruleSelector = ruleSelector;
    }

    void evaluate(OperationResult result)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, SecurityViolationException,
            ConfigurationException, CommunicationException {

        collector.initialize(result);
        List<EvaluatedPolicyRuleImpl> rules = collector.collectFocusRules(result);

        LOGGER.trace("Selecting rules from {} focus-attached policy rules", rules.size());
        List<EvaluatedPolicyRuleImpl> applicableRules = selectAndSetApplicableRules(rules);
        evaluateCollectedRules(applicableRules, result);
    }

    private @NotNull List<EvaluatedPolicyRuleImpl> selectAndSetApplicableRules(List<EvaluatedPolicyRuleImpl> rules) {
        List<EvaluatedPolicyRuleImpl> applicableRules = new ArrayList<>();
        elementContext.clearObjectPolicyRules();
        for (EvaluatedPolicyRuleImpl rule : rules) {
            if (ruleSelector.test(rule)) {
                applicableRules.add(rule);
                elementContext.addObjectPolicyRule(rule);
            } else {
                LOGGER.trace("Rule {} is not applicable to the focus/projection, skipping: {}", rule.getName(), rule);
            }
        }
        return applicableRules;
    }

    private void evaluateCollectedRules(
            List<EvaluatedPolicyRuleImpl> applicableRules, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        LOGGER.trace("Evaluating {} applicable rules", applicableRules.size());
        RulesEvaluationContext globalCtx = new RulesEvaluationContext();
        List<ObjectPolicyRuleEvaluationContext<O>> contextsToEvaluate =
                applicableRules.stream()
                        .map(rule -> new ObjectPolicyRuleEvaluationContext<>(rule, globalCtx, elementContext, task))
                        .collect(Collectors.toList());
        evaluateRules(contextsToEvaluate, result);
        new PolicyStateRecorder().applyObjectState(elementContext, globalCtx.rulesToRecord);
    }

    /**
     * Evaluates policy rules attached to the focus.
     */
    static class FocusPolicyRulesEvaluator<F extends AssignmentHolderType> extends ObjectPolicyRulesEvaluator<F> {
        FocusPolicyRulesEvaluator(@NotNull LensFocusContext<F> focusContext, @NotNull Task task) {
            super(focusContext, task, EvaluatedPolicyRule::isApplicableToFocusObject);
        }
    }

    static class ProjectionPolicyRulesEvaluator extends ObjectPolicyRulesEvaluator<ShadowType> {
        ProjectionPolicyRulesEvaluator(@NotNull LensProjectionContext projectionContext, @NotNull Task task) {
            super(projectionContext, task, EvaluatedPolicyRule::isApplicableToProjection);
        }
    }
}
