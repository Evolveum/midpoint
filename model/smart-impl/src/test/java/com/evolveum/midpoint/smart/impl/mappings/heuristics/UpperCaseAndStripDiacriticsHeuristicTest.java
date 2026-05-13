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

public class UpperCaseAndStripDiacriticsHeuristicTest extends MappingScriptTestBase {

    public UpperCaseAndStripDiacriticsHeuristicTest() throws SchemaException, IOException, SAXException {
    }

    @Test
    void inputContainsLowercaseAndDiacritics_heuristicsShouldBeApplicable() {
        final UpperCaseAndStripDiacriticsHeuristic heuristic = new UpperCaseAndStripDiacriticsHeuristic();
        final ValuesPairSample<String, String> valuesPairSample = inboundValuesPair("josé", "JOSE");

        assertTrue(heuristic.isApplicable(valuesPairSample));
    }

    @Test
    void inputContainsLowercase_heuristicsShouldBeApplicable() {
        final UpperCaseAndStripDiacriticsHeuristic heuristic = new UpperCaseAndStripDiacriticsHeuristic();
        final ValuesPairSample<String, String> valuesPairSample = inboundValuesPair("jose", "JOSE");

        assertTrue(heuristic.isApplicable(valuesPairSample));
    }

    @Test
    void inputContainsDiacritics_heuristicsShouldBeApplicable() {
        final UpperCaseAndStripDiacriticsHeuristic heuristic = new UpperCaseAndStripDiacriticsHeuristic();
        final ValuesPairSample<String, String> valuesPairSample = inboundValuesPair("JOSÉ", "JOSE");

        assertTrue(heuristic.isApplicable(valuesPairSample));
    }

    @Test
    void inputDoesNotContainDiacriticsNorLowercase_heuristicsShouldNotBeApplicable() {
        final UpperCaseAndStripDiacriticsHeuristic heuristic = new UpperCaseAndStripDiacriticsHeuristic();
        final ValuesPairSample<String, String> valuesPairSample = inboundValuesPair("JOSE", "JOSE");

        assertFalse(heuristic.isApplicable(valuesPairSample));
    }

    @Test
    void inputContainsLowercaseAndDiacritics_inboundScriptIsEvaluated_outputShouldBeUppercaseAndWithoutDiacritics()
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
            ConfigurationException, ObjectNotFoundException {
        final UpperCaseAndStripDiacriticsHeuristic heuristic = new UpperCaseAndStripDiacriticsHeuristic();

        final ExpressionType expression = heuristic.inboundExpression(MappingScriptTestBase::createScriptExpression);
        final String output = this.evaluateExpression(expression, "input", "josé Müller!");

        assertEquals(output, "JOSE MULLER!");
    }

    @Test
    void propertyContainsLowercaseAndDiacritics_outboundScriptIsEvaluated_outputShouldBeUppercaseAndWithoutDiacritics()
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
            ConfigurationException, ObjectNotFoundException {
        final String focusProperty = "name";
        final UpperCaseAndStripDiacriticsHeuristic heuristic = new UpperCaseAndStripDiacriticsHeuristic();

        final ExpressionType expression = heuristic.outboundExpression(focusProperty, MappingScriptTestBase::createScriptExpression);
        final String output = this.evaluateExpression(expression, focusProperty, "josé Müller!");

        assertEquals(output, "JOSE MULLER!");
    }

}