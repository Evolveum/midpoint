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
                "input ? stripDiacritics(input) : null\n\n" +
                "String stripDiacritics(String text) {\n" +
                "    def normalized = java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFD)\n" +
                "        .replaceAll('\\\\p{InCombiningDiacriticalMarks}+', '')\n" +
                "    return normalized\n" +
                "        .replace('Ł', 'L').replace('ł', 'l')\n" +
                "        .replace('Ø', 'O').replace('ø', 'o')\n" +
                "        .replace('Å', 'A').replace('å', 'a')\n" +
                "        .replace('Æ', 'AE').replace('æ', 'ae')\n" +
                "        .replace('Œ', 'OE').replace('œ', 'oe')\n" +
                "        .replace('Đ', 'D').replace('đ', 'd')\n" +
                "        .replace('Ħ', 'H').replace('ħ', 'h')\n" +
                "        .replace('Ŋ', 'N').replace('ŋ', 'n')\n" +
                "        .replace('Ŧ', 'T').replace('ŧ', 't')\n" +
                "        .replace('ß', 'ss')\n" +
                "}",
                "Strip diacritical marks");
    }

    @Override
    public ExpressionType outboundExpression(String focusPropertyName, MappingExpressionFactory factory) {
        return factory.createScriptExpression(
                focusPropertyName + " ? stripDiacritics(" + focusPropertyName + ") : null\n\n" +
                "String stripDiacritics(String text) {\n" +
                "    def normalized = java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFD)\n" +
                "        .replaceAll('\\\\p{InCombiningDiacriticalMarks}+', '')\n" +
                "    return normalized\n" +
                "        .replace('Ł', 'L').replace('ł', 'l')\n" +
                "        .replace('Ø', 'O').replace('ø', 'o')\n" +
                "        .replace('Å', 'A').replace('å', 'a')\n" +
                "        .replace('Æ', 'AE').replace('æ', 'ae')\n" +
                "        .replace('Œ', 'OE').replace('œ', 'oe')\n" +
                "        .replace('Đ', 'D').replace('đ', 'd')\n" +
                "        .replace('Ħ', 'H').replace('ħ', 'h')\n" +
                "        .replace('Ŋ', 'N').replace('ŋ', 'n')\n" +
                "        .replace('Ŧ', 'T').replace('ŧ', 't')\n" +
                "        .replace('ß', 'ss')\n" +
                "}",
                "Strip diacritical marks");
    }
}
