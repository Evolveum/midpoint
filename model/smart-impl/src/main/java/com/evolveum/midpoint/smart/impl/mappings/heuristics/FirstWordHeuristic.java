/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.smart.impl.mappings.heuristics;

import com.evolveum.midpoint.smart.impl.mappings.ValuesPairSample;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;

import org.springframework.stereotype.Component;

/**
 * Heuristic that extracts the first word from the input.
 * Useful for extracting given names from full names or first part of compound identifiers.
 */
@Component
public class FirstWordHeuristic implements HeuristicRule {

    @Override
    public String getName() {
        return "firstWord";
    }

    @Override
    public String getDescription() {
        return "Extract first word from input";
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
    public ExpressionType inboundExpression(MappingExpressionFactory factory) {
        return factory.createScriptExpression(
                "input?.split('\\\\s+')[0]",
                "Extract first word");
    }

    @Override
    public ExpressionType outboundExpression(String focusPropertyName, MappingExpressionFactory factory) {
        return factory.createScriptExpression(
                focusPropertyName + "?.split('\\\\s+')[0]",
                "Extract first word");
    }
}
