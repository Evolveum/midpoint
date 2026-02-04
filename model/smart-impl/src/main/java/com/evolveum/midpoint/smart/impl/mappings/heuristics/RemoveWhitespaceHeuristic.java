/*
 * Copyright (c) 2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.smart.impl.mappings.heuristics;

import com.evolveum.midpoint.smart.impl.mappings.ValuesPairSample;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;

import org.springframework.stereotype.Component;

/**
 * Heuristic that removes all whitespace characters from the input.
 * Useful for phone numbers, IDs, or when spaces need to be completely eliminated.
 */
@Component
public class RemoveWhitespaceHeuristic extends AbstractHeuristicMapping {

    @Override
    public String getName() {
        return "removeWhitespace";
    }

    @Override
    public String getDescription() {
        return "Remove all whitespace characters";
    }

    /**
     * Only applicable if source values contain any whitespace characters.
     */
    @Override
    public boolean isApplicable(ValuesPairSample<?, ?> sample) {
        return sample.pairs().stream()
                .flatMap(pair -> pair.getSourceValues(sample.direction()).stream())
                .filter(value -> value instanceof String)
                .map(value -> (String) value)
                .anyMatch(str -> str != null && str.matches(".*\\s+.*"));
    }

    @Override
    public ExpressionType generateInboundExpression() {
        return createScriptExpression(
                "input?.replaceAll('\\\\s+', '')",
                "Remove all whitespace");
    }

    @Override
    public ExpressionType generateOutboundExpression(String focusPropertyName) {
        return createScriptExpression(
                focusPropertyName + "?.replaceAll('\\\\s+', '')",
                "Remove all whitespace");
    }
}
