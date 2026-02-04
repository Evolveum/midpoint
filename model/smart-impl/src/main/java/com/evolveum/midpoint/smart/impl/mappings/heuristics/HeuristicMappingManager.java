/*
 * Copyright (c) 2025 Evolveum and contributors
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

import org.springframework.stereotype.Component;

@Component
public class HeuristicMappingManager {

    private static final Trace LOGGER = TraceManager.getTrace(HeuristicMappingManager.class);

    private final List<HeuristicMapping> heuristics;
    private final MappingsQualityAssessor qualityAssessor;

    public HeuristicMappingManager(List<HeuristicMapping> heuristics, MappingsQualityAssessor qualityAssessor) {
        this.heuristics = heuristics;
        this.qualityAssessor = qualityAssessor;
    }

    /**
     * Attempts to find the best heuristic mapping.
     * All heuristics are evaluated, and the one with the highest quality (higher than zero) is returned.
     */
    public Optional<HeuristicResult> findBestMatch(ValuesPairSample<?, ?> sample, Task task, OperationResult result) {
        HeuristicResult bestResult = null;

        for (HeuristicMapping heuristic : heuristics) {
            HeuristicResult heuristicResult = evaluateHeuristic(heuristic, sample, task, result);
            if (heuristicResult == null || heuristicResult.quality() == 0) {
                continue;
            }

            if (bestResult == null || heuristicResult.quality() > bestResult.quality()) {
                LOGGER.debug("New best heuristic '{}' with quality {}", heuristicResult.heuristicName(), heuristicResult.quality());
                bestResult = heuristicResult;
            }
        }

        if (bestResult != null) {
            LOGGER.info("Best heuristic '{}' with quality {}", bestResult.heuristicName(), bestResult.quality());
            return Optional.of(bestResult);
        }

        return Optional.empty();
    }

    private HeuristicResult evaluateHeuristic(HeuristicMapping heuristic, ValuesPairSample<?, ?> sample, Task task, OperationResult result) {
        try {
            if (!heuristic.isApplicable(sample)) {
                return null;
            }

            LOGGER.trace("Evaluating heuristic: {}", heuristic.getName());
            var expression = generateExpression(heuristic, sample);

            var assessment = qualityAssessor.assessMappingsQuality(sample, expression, task, result);
            if (assessment == null || assessment.status() != MappingsQualityAssessor.AssessmentStatus.OK) {
                return null;
            }

            float quality = assessment.quality();
            LOGGER.debug("Heuristic '{}' quality: {}", heuristic.getName(), quality);
            return new HeuristicResult(heuristic.getName(), expression, quality);

        } catch (ExpressionEvaluationException | SecurityViolationException e) {
            LOGGER.debug("Heuristic '{}' evaluation failed: {}", heuristic.getName(), e.getMessage());
            return null;
        }
    }

    private ExpressionType generateExpression(HeuristicMapping heuristic, ValuesPairSample<?, ?> sample) {
        if (sample.direction() == MappingDirection.INBOUND) {
            return heuristic.generateInboundExpression();
        } else {
            String focusPropertyName = sample.focusPropertyPath().lastName().getLocalPart();
            return heuristic.generateOutboundExpression(focusPropertyName);
        }
    }
}
