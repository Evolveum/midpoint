/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.smart.impl.mappings.heuristics;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.io.IOException;

import com.evolveum.midpoint.util.exception.*;

import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.smart.impl.mappings.MappingScriptTestBase;
import com.evolveum.midpoint.smart.impl.mappings.ValuesPairSample;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;

public class TrimAndStripDiacriticsHeuristicTest extends MappingScriptTestBase {

    public TrimAndStripDiacriticsHeuristicTest() throws SchemaException, IOException, SAXException {
    }

    @Test
    void inputContainsWhitespaceAndDiacritics_heuristicsShouldBeApplicable() {
        final TrimAndStripDiacriticsHeuristic heuristic = new TrimAndStripDiacriticsHeuristic();
        final ValuesPairSample<String, String> valuesPairSample = inboundValuesPair("  José  ", "Jose");

        assertTrue(heuristic.isApplicable(valuesPairSample));
    }

    @Test
    void inputContainsWhitespace_heuristicsShouldBeApplicable() {
        final TrimAndStripDiacriticsHeuristic heuristic = new TrimAndStripDiacriticsHeuristic();
        final ValuesPairSample<String, String> valuesPairSample = inboundValuesPair(" Jose  ", "Jose");

        assertTrue(heuristic.isApplicable(valuesPairSample));
    }

    @Test
    void inputContainsDiacritics_heuristicsShouldBeApplicable() {
        final TrimAndStripDiacriticsHeuristic heuristic = new TrimAndStripDiacriticsHeuristic();
        final ValuesPairSample<String, String> valuesPairSample = inboundValuesPair("José  ", "Jose");

        assertTrue(heuristic.isApplicable(valuesPairSample));
    }

    @Test
    void inputDoesNotContainDiacriticsNorWhitespace_heuristicsShouldNotBeApplicable() {
        final TrimAndStripDiacriticsHeuristic heuristic = new TrimAndStripDiacriticsHeuristic();
        final ValuesPairSample<String, String> valuesPairSample = inboundValuesPair("Jose", "Jose");

        assertFalse(heuristic.isApplicable(valuesPairSample));
    }

    @Test
    void inputContainsWhitespaceAndDiacritics_inboundScriptIsEvaluated_outputShouldBeTrimmedAndWithoutDiacritics()
            throws CommonException {
        final TrimAndStripDiacriticsHeuristic heuristic = new TrimAndStripDiacriticsHeuristic();

        final ExpressionType expression = heuristic.inboundExpression(MappingScriptTestBase::createScriptExpression);
        final String output = this.evaluateExpression(expression, "input", "  José Müller!  ");

        assertEquals(output, "Jose Muller!");
    }

    @Test
    void propertyContainsWhitespaceAndDiacritics_outboundScriptIsEvaluated_outputShouldBeTrimmedAndWithoutDiacritics()
            throws CommonException {
        final String focusProperty = "name";
        final TrimAndStripDiacriticsHeuristic heuristic = new TrimAndStripDiacriticsHeuristic();

        final ExpressionType expression = heuristic.outboundExpression(focusProperty, MappingScriptTestBase::createScriptExpression);
        final String output = this.evaluateExpression(expression, focusProperty, "  José Müller!  ");

        assertEquals(output, "Jose Muller!");
    }

}
