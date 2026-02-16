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

@Component
public class TrimAndLowerCaseHeuristic implements HeuristicRule {

    @Override
    public String getName() {
        return "trimAndLowerCase";
    }

    @Override
    public String getDescription() {
        return "Trim whitespace and convert to lowercase";
    }

    @Override
    public int getOrder() {
        return 2;
    }

    /**
     * Only applicable if source values have whitespace or uppercase letters.
     */
    @Override
    public boolean isApplicable(ValuesPairSample<?, ?> sample) {
        return sample.pairs().stream()
                .flatMap(pair -> pair.getSourceValues(sample.direction()).stream())
                .filter(value -> value instanceof String)
                .map(value -> (String) value)
                .anyMatch(str -> str != null &&
                        (!str.equals(str.trim()) || !str.equals(str.toLowerCase())));
    }

    @Override
    public ExpressionType inboundExpression(MappingExpressionFactory factory) {
        return factory.createScriptExpression(
                "basic.lc(basic.trim(input))",
                "Trim and convert to lowercase");
    }

    @Override
    public ExpressionType outboundExpression(String focusPropertyName, MappingExpressionFactory factory) {
        return factory.createScriptExpression(
                "basic.lc(basic.trim(" + focusPropertyName + "))",
                "Trim and convert to lowercase");
    }
}
