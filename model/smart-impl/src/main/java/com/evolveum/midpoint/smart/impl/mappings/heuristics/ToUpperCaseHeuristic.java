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
public class ToUpperCaseHeuristic implements HeuristicRule {

    @Override
    public String getName() {
        return "toUpperCase";
    }

    @Override
    public String getDescription() {
        return "Convert input to uppercase";
    }

    /**
     * Only applicable if source values contain lowercase letters.
     */
    @Override
    public boolean isApplicable(ValuesPairSample<?, ?> sample) {
        return sample.pairs().stream()
                .flatMap(pair -> pair.getSourceValues(sample.direction()).stream())
                .filter(value -> value instanceof String)
                .map(value -> (String) value)
                .anyMatch(str -> str != null && !str.equals(str.toUpperCase()));
    }

    @Override
    public ExpressionType inboundExpression(MappingExpressionFactory factory) {
        return factory.createScriptExpression(
                "basic.uc(input)",
                "Convert to uppercase");
    }

    @Override
    public ExpressionType outboundExpression(String focusPropertyName, MappingExpressionFactory factory) {
        return factory.createScriptExpression(
                "basic.uc(" + focusPropertyName + ")",
                "Convert to uppercase");
    }
}
