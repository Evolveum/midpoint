/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.smart.impl.mappings.heuristics;

import java.util.List;
import java.util.Optional;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.impl.mappings.MappingDirection;
import com.evolveum.midpoint.smart.impl.mappings.ValuesPairSample;
import com.evolveum.midpoint.smart.impl.scoring.MappingsQualityAssessor;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectFactory;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ScriptExpressionEvaluatorType;

import org.springframework.stereotype.Component;

@Component
public class HeuristicRuleMatcher {

    private static final Trace LOGGER = TraceManager.getTrace(HeuristicRuleMatcher.class);

    private final List<HeuristicRule> rules;
    private final MappingsQualityAssessor qualityAssessor;

    public HeuristicRuleMatcher(List<HeuristicRule> rules, MappingsQualityAssessor qualityAssessor) {
        this.rules = rules;
        this.qualityAssessor = qualityAssessor;
    }

    /**
     * Static factory method for creating script expressions.
     * Used as a method reference passed to heuristic rules.
     */
    private static ExpressionType createScriptExpression(String groovyCode, String description) {
        return new ExpressionType()
                .description(description)
                .expressionEvaluator(
                        new ObjectFactory().createScript(
                                new ScriptExpressionEvaluatorType().code(groovyCode)));
    }

    /**
     * Attempts to find the best heuristic rule.
     * All rules are evaluated, and the one with the highest quality (higher than zero) is returned.
     */
    public Optional<HeuristicResult> findBestMatch(ValuesPairSample<?, ?> sample, Task task, OperationResult result) {
        HeuristicRule bestRule = null;
        ExpressionType bestExpression = null;
        float bestQuality = 0f;

        for (HeuristicRule rule : rules) {
            if (!rule.isApplicable(sample)) {
                continue;
            }

            LOGGER.trace("Evaluating rule: {}", rule.getName());
            ExpressionType expression = generateExpression(rule, sample);

            try {
                var assessment = qualityAssessor.assessMappingsQuality(sample, expression, task, result);
                if (assessment == null || assessment.status() != MappingsQualityAssessor.AssessmentStatus.OK) {
                    continue;
                }

                float quality = assessment.quality();
                LOGGER.debug("Rule '{}' quality: {}", rule.getName(), quality);

                if (quality > bestQuality) {
                    LOGGER.debug("New best rule '{}' with quality {}", rule.getName(), quality);
                    bestRule = rule;
                    bestExpression = expression;
                    bestQuality = quality;
                }
            } catch (ExpressionEvaluationException | SecurityViolationException e) {
                LOGGER.debug("Rule '{}' evaluation failed: {}", rule.getName(), e.getMessage());
            }
        }

        if (bestRule != null) {
            LOGGER.info("Best rule '{}' with quality {}", bestRule.getName(), bestQuality);
            return Optional.of(new HeuristicResult(bestRule.getName(), bestExpression, bestQuality));
        }

        return Optional.empty();
    }


    private ExpressionType generateExpression(HeuristicRule rule, ValuesPairSample<?, ?> sample) {
        if (sample.direction() == MappingDirection.INBOUND) {
            return rule.inboundExpression(HeuristicRuleMatcher::createScriptExpression);
        } else {
            String focusPropertyName = sample.focusPropertyPath().lastName().getLocalPart();
            return rule.outboundExpression(focusPropertyName, HeuristicRuleMatcher::createScriptExpression);
        }
    }
}
