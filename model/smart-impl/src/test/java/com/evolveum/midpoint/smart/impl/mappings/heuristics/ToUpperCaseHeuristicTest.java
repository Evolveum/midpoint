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

public class ToUpperCaseHeuristicTest extends MappingScriptTestBase {

    public ToUpperCaseHeuristicTest() throws SchemaException, IOException, SAXException {
    }

    @Test
    void inputContainsLowercaseLetters_heuristicsShouldBeApplicable() {
        final ToUpperCaseHeuristic heuristic = new ToUpperCaseHeuristic();
        final ValuesPairSample<String, String> valuesPairSample = inboundValuesPair("hello", "HELLO");

        assertTrue(heuristic.isApplicable(valuesPairSample));
    }

    @Test
    void inputDoesNotContainLowercaseLetters_heuristicsShouldNotBeApplicable() {
        final ToUpperCaseHeuristic heuristic = new ToUpperCaseHeuristic();
        final ValuesPairSample<String, String> valuesPairSample = inboundValuesPair("HELLO", "HELLO");

        assertFalse(heuristic.isApplicable(valuesPairSample));
    }

    @Test
    void inputContainsLowercaseLetters_inboundScriptIsEvaluated_outputShouldBeUppercase()
            throws CommonException {
        final ToUpperCaseHeuristic heuristic = new ToUpperCaseHeuristic();

        final ExpressionType expression = heuristic.inboundExpression(MappingScriptTestBase::createScriptExpression);
        final String output = this.evaluateExpression(expression, "input", "hello World!");

        assertEquals(output, "HELLO WORLD!");
    }

    @Test
    void propertyContainsLowercaseLetters_outboundScriptIsEvaluated_outputShouldBeUppercase()
            throws CommonException {
        final String focusProperty = "name";
        final ToUpperCaseHeuristic heuristic = new ToUpperCaseHeuristic();

        final ExpressionType expression = heuristic.outboundExpression(focusProperty, MappingScriptTestBase::createScriptExpression);
        final String output = this.evaluateExpression(expression, focusProperty, "hello World!");

        assertEquals(output, "HELLO WORLD!");
    }

}
