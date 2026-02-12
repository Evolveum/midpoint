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
public class TrimAndUpperCaseHeuristic implements HeuristicRule {

    @Override
    public String getName() {
        return "trimAndUpperCase";
    }

    @Override
    public String getDescription() {
        return "Trim whitespace and convert to uppercase";
    }

    @Override
    public int getOrder() {
        return 2;
    }

    @Override
    public boolean isApplicable(ValuesPairSample<?, ?> sample) {
        return sample.pairs().stream()
                .flatMap(pair -> pair.getSourceValues(sample.direction()).stream())
                .filter(value -> value instanceof String)
                .map(value -> (String) value)
                .anyMatch(str -> str != null &&
                        (!str.equals(str.trim()) || !str.equals(str.toUpperCase())));
    }

    @Override
    public ExpressionType inboundExpression(MappingExpressionFactory factory) {
        return factory.createScriptExpression(
                "input?.trim()?.toUpperCase()",
                "Trim and convert to uppercase");
    }

    @Override
    public ExpressionType outboundExpression(String focusPropertyName, MappingExpressionFactory factory) {
        return factory.createScriptExpression(
                focusPropertyName + "?.trim()?.toUpperCase()",
                "Trim and convert to uppercase");
    }
}
