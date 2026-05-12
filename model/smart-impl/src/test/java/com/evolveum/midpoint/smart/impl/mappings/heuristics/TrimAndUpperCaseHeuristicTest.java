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

import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.smart.impl.mappings.ValuesPairSample;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class TrimAndUpperCaseHeuristicTest extends HeuristicsRuleTest {

    public TrimAndUpperCaseHeuristicTest() throws SchemaException, IOException, SAXException {
    }

    @Test
    void inputContainsWhitespaceAndLowercase_heuristicsShouldBeApplicable() {
        final TrimAndUpperCaseHeuristic heuristic = new TrimAndUpperCaseHeuristic();
        final ValuesPairSample<String, String> valuesPairSample = inboundValuesPair("  hello  ", "HELLO");

        assertTrue(heuristic.isApplicable(valuesPairSample));
    }

    @Test
    void inputContainsWhitespace_heuristicsShouldBeApplicable() {
        final TrimAndUpperCaseHeuristic heuristic = new TrimAndUpperCaseHeuristic();
        final ValuesPairSample<String, String> valuesPairSample = inboundValuesPair("  HELLO  ", "HELLO");

        assertTrue(heuristic.isApplicable(valuesPairSample));
    }

    @Test
    void inputContainsLowercase_heuristicsShouldBeApplicable() {
        final TrimAndUpperCaseHeuristic heuristic = new TrimAndUpperCaseHeuristic();
        final ValuesPairSample<String, String> valuesPairSample = inboundValuesPair("hello  ", "HELLO");

        assertTrue(heuristic.isApplicable(valuesPairSample));
    }

    @Test
    void inputDoesNotContainWhitespaceNorLowercase_heuristicsShouldNotBeApplicable() {
        final TrimAndStripDiacriticsHeuristic heuristic = new TrimAndStripDiacriticsHeuristic();
        final ValuesPairSample<String, String> valuesPairSample = inboundValuesPair("HELLO", "HELLO");

        assertFalse(heuristic.isApplicable(valuesPairSample));
    }
    @Test
    void inputContainsWhitespaceAndLowercase_inboundScriptIsEvaluated_outputShouldBeTrimmedAndUppercase()
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
            ConfigurationException, ObjectNotFoundException {
        final TrimAndUpperCaseHeuristic heuristic = new TrimAndUpperCaseHeuristic();

        final ExpressionType expression = heuristic.inboundExpression(HeuristicsRuleTest::createScriptExpression);
        final String output = this.evaluateExpression(expression, "input", "  hello World!  ");

        assertEquals(output, "HELLO WORLD!");
    }

    @Test
    void propertyContainsWhitespaceAndLowercase_outboundScriptIsEvaluated_outputShouldBeTrimmedAndUppercase()
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
            ConfigurationException, ObjectNotFoundException {
        final String focusProperty = "name";
        final TrimAndUpperCaseHeuristic heuristic = new TrimAndUpperCaseHeuristic();

        final ExpressionType expression = heuristic.outboundExpression(focusProperty, HeuristicsRuleTest::createScriptExpression);
        final String output = this.evaluateExpression(expression, focusProperty, "  hello World!  ");

        assertEquals(output, "HELLO WORLD!");
    }

}