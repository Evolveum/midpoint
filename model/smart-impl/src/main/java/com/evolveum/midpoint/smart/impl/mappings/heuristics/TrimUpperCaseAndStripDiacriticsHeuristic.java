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

import java.text.Normalizer;

@Component
public class TrimUpperCaseAndStripDiacriticsHeuristic implements HeuristicRule {

    @Override
    public String getName() {
        return "trimUpperCaseAndStripDiacritics";
    }

    @Override
    public String getDescription() {
        return "Trim whitespace, convert to uppercase, and strip diacritical marks";
    }

    @Override
    public int getOrder() {
        return 3;
    }

    @Override
    public boolean isApplicable(ValuesPairSample<?, ?> sample) {
        return sample.pairs().stream()
                .flatMap(pair -> pair.getSourceValues(sample.direction()).stream())
                .filter(value -> value instanceof String)
                .map(value -> (String) value)
                .anyMatch(str -> str != null && 
                        (hasDiacritics(str) || !str.equals(str.trim()) || !str.equals(str.toUpperCase())));
    }

    private boolean hasDiacritics(String str) {
        String normalized = Normalizer.normalize(str, Normalizer.Form.NFD);
        if (normalized.matches(".*\\p{InCombiningDiacriticalMarks}+.*")) {
            return true;
        }
        return str.matches(".*[ŁłØøÅåÆæŒœĐđŦŧĦħŊŋĸŁłŔŕŖŗŘřŚśŜŝŞşŠšŢţŤťŦŧŨũŪūŬŭŮůŰűŲųŴŵŶŷŸŹźŻżŽž].*");
    }

    @Override
    public ExpressionType inboundExpression(MappingExpressionFactory factory) {
        return factory.createScriptExpression(
                "basic.uc(basic.toAscii(basic.trim(input)))",
                "Trim, convert to uppercase, and strip diacritical marks");
    }

    @Override
    public ExpressionType outboundExpression(String focusPropertyName, MappingExpressionFactory factory) {
        return factory.createScriptExpression(
                "basic.uc(basic.toAscii(basic.trim(" + focusPropertyName + ")))",
                "Trim, convert to uppercase, and strip diacritical marks");
    }
}
