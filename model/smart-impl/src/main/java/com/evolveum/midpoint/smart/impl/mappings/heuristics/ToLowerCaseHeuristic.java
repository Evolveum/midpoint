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

@Component
public class ToLowerCaseHeuristic extends AbstractHeuristicMapping {

    @Override
    public String getName() {
        return "toLowerCase";
    }

    @Override
    public String getDescription() {
        return "Convert input to lowercase";
    }

    /**
     * Only applicable if source values contain uppercase letters.
     */
    @Override
    public boolean isApplicable(ValuesPairSample<?, ?> sample) {
        return sample.pairs().stream()
                .flatMap(pair -> pair.getSourceValues(sample.direction()).stream())
                .filter(value -> value instanceof String)
                .map(value -> (String) value)
                .anyMatch(str -> str != null && !str.equals(str.toLowerCase()));
    }

    @Override
    public ExpressionType generateInboundExpression() {
        return createScriptExpression(
                "input?.toLowerCase()",
                "Convert to lowercase");
    }

    @Override
    public ExpressionType generateOutboundExpression(String focusPropertyName) {
        return createScriptExpression(
                focusPropertyName + "?.toLowerCase()",
                "Convert to lowercase");
    }
}
