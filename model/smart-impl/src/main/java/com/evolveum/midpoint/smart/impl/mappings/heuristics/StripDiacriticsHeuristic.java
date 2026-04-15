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

/**
 * Heuristic that strips diacritical marks from characters.
 * Useful for normalizing names, converting accented characters to ASCII equivalents.
 * Example: "Michał" -> "Michal", "Müller" -> "Muller", "José" -> "Jose"
 */
@Component
public class StripDiacriticsHeuristic implements HeuristicRule {

    @Override
    public String getName() {
        return "stripDiacritics";
    }

    @Override
    public String getDescription() {
        return "Strip diacritical marks from input";
    }

    /**
     * Only applicable if source values contain characters with diacritics.
     */
    @Override
    public boolean isApplicable(ValuesPairSample<?, ?> sample) {
        return sample.pairs().stream()
                .flatMap(pair -> pair.getSourceValues(sample.direction()).stream())
                .filter(value -> value instanceof String)
                .map(value -> (String) value)
                .anyMatch(str -> str != null && hasDiacritics(str));
    }

    /**
     * Checks if a string contains diacritical marks.
     * Handles both decomposable (e.g., é) and non-decomposable (e.g., Ł, Ø) characters.
     */
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
                "basic.toAscii(input)",
                "Strip diacritical marks");
    }

    @Override
    public ExpressionType outboundExpression(String focusPropertyName, MappingExpressionFactory factory) {
        return factory.createScriptExpression(
                "basic.toAscii(" + focusPropertyName + ")",
                "Strip diacritical marks");
    }
}
