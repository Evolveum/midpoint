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

import com.evolveum.midpoint.smart.impl.mappings.MappingScriptTestBase;
import com.evolveum.midpoint.smart.impl.mappings.ValuesPairSample;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;

public class TrimAndLowerCaseHeuristicTest extends MappingScriptTestBase {

    public TrimAndLowerCaseHeuristicTest() throws SchemaException, IOException, SAXException {
    }

    @Test
    void inputContainsWhitespaceAndUppercase_heuristicsShouldBeApplicable() {
        final TrimAndLowerCaseHeuristic heuristic = new TrimAndLowerCaseHeuristic();
        final ValuesPairSample<String, String> valuesPairSample = inboundValuesPair("  HELLO  ", "hello");

        assertTrue(heuristic.isApplicable(valuesPairSample));
    }

    @Test
    void inputContainsWhitespace_heuristicsShouldBeApplicable() {
        final TrimAndLowerCaseHeuristic heuristic = new TrimAndLowerCaseHeuristic();
        final ValuesPairSample<String, String> valuesPairSample = inboundValuesPair(" hello  ", "hello");

        assertTrue(heuristic.isApplicable(valuesPairSample));
    }

    @Test
    void inputContainsUppercase_heuristicsShouldBeApplicable() {
        final TrimAndLowerCaseHeuristic heuristic = new TrimAndLowerCaseHeuristic();
        final ValuesPairSample<String, String> valuesPairSample = inboundValuesPair("HELLO", "hello");

        assertTrue(heuristic.isApplicable(valuesPairSample));
    }

    @Test
    void inputDoesNotContainUppercaseOrWhitespace_heuristicsShouldNotBeApplicable() {
        final TrimAndLowerCaseHeuristic heuristic = new TrimAndLowerCaseHeuristic();
        final ValuesPairSample<String, String> valuesPairSample = inboundValuesPair("hello", "hello");

        assertFalse(heuristic.isApplicable(valuesPairSample));
    }

    @Test
    void inputContainsWhitespaceAndUppercase_inboundScriptIsEvaluated_outputShouldBeTrimmedAndLowercase()
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
            ConfigurationException, ObjectNotFoundException {
        final TrimAndLowerCaseHeuristic heuristic = new TrimAndLowerCaseHeuristic();

        final ExpressionType expression = heuristic.inboundExpression(MappingScriptTestBase::createScriptExpression);
        final String output = this.evaluateExpression(expression, "input", "  HELLO World!  ");

        assertEquals(output, "hello world!");
    }

    @Test
    void propertyContainsWhitespaceAndUppercase_outboundScriptIsEvaluated_outputShouldBeTrimmedAndLowercase()
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
            ConfigurationException, ObjectNotFoundException {
        final String focusProperty = "name";
        final TrimAndLowerCaseHeuristic heuristic = new TrimAndLowerCaseHeuristic();

        final ExpressionType expression = heuristic.outboundExpression(focusProperty, MappingScriptTestBase::createScriptExpression);
        final String output = this.evaluateExpression(expression, focusProperty, "  HELLO World!  ");

        assertEquals(output, "hello world!");
    }

}