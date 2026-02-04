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
 * Heuristic that extracts the last word from the input.
 * Useful for extracting family names from full names or last part of compound identifiers.
 */
@Component
public class LastWordHeuristic extends AbstractHeuristicMapping {

    @Override
    public String getName() {
        return "lastWord";
    }

    @Override
    public String getDescription() {
        return "Extract last word from input";
    }

    /**
     * Only applicable if source values contain spaces (multi-word strings).
     */
    @Override
    public boolean isApplicable(ValuesPairSample<?, ?> sample) {
        return sample.pairs().stream()
                .flatMap(pair -> pair.getSourceValues(sample.direction()).stream())
                .filter(value -> value instanceof String)
                .map(value -> (String) value)
                .anyMatch(str -> str != null && str.trim().contains(" "));
    }

    @Override
    public ExpressionType generateInboundExpression() {
        return createScriptExpression(
                "def words = input?.split('\\\\s+'); words ? words[-1] : null",
                "Extract last word");
    }

    @Override
    public ExpressionType generateOutboundExpression(String focusPropertyName) {
        return createScriptExpression(
                "def words = " + focusPropertyName + "?.split('\\\\s+'); words ? words[-1] : null",
                "Extract last word");
    }
}
