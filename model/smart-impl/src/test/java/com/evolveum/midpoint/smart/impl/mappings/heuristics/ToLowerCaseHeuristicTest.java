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
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class ToLowerCaseHeuristicTest extends MappingScriptTestBase {

    public ToLowerCaseHeuristicTest() throws SchemaException, IOException, SAXException {
    }

    @Test
    void inputContainsUppercaseLetters_heuristicsShouldBeApplicable() {
        final ToLowerCaseHeuristic heuristic = new ToLowerCaseHeuristic();
        final ValuesPairSample<String, String> valuesPairSample = inboundValuesPair("HELLO", "hello");

        assertTrue(heuristic.isApplicable(valuesPairSample));
    }

    @Test
    void inputDoesNotContainUppercaseLetters_heuristicsShouldNotBeApplicable() {
        final ToLowerCaseHeuristic heuristic = new ToLowerCaseHeuristic();
        final ValuesPairSample<String, String> valuesPairSample =
                inboundValuesPair("hello", "hello");

        assertFalse(heuristic.isApplicable(valuesPairSample));
    }

    @Test
    void inputContainsUppercaseLetters_inboundScriptIsEvaluated_outputShouldBeLowercase()
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
            ConfigurationException, ObjectNotFoundException {
        final ToLowerCaseHeuristic heuristic = new ToLowerCaseHeuristic();

        final ExpressionType expression = heuristic.inboundExpression(MappingScriptTestBase::createScriptExpression);
        final String output = this.evaluateExpression(expression, "input", "HELLO World!");

        assertEquals(output, "hello world!");
    }

    @Test
    void propertyContainsUppercaseLetters_outboundScriptIsEvaluated_outputShouldBeLowercase()
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
            ConfigurationException, ObjectNotFoundException {
        final String focusProperty = "name";
        final ToLowerCaseHeuristic heuristic = new ToLowerCaseHeuristic();

        final ExpressionType expression = heuristic.outboundExpression(focusProperty, MappingScriptTestBase::createScriptExpression);
        final String output = this.evaluateExpression(expression, focusProperty, "HELLO World!");

        assertEquals(output, "hello world!");
    }

}