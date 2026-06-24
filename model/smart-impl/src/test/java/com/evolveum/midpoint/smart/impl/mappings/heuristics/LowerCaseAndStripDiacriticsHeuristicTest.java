/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.smart.impl.mappings.heuristics;

import static org.testng.Assert.assertEquals;
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

public class LowerCaseAndStripDiacriticsHeuristicTest extends MappingScriptTestBase {

    LowerCaseAndStripDiacriticsHeuristicTest() throws SchemaException, IOException, SAXException {
    }

    @Test
    void inputContainsUppercaseLettersAndDiacritics_heuristicsShouldBeApplicable() {
        final LowerCaseAndStripDiacriticsHeuristic heuristic = new LowerCaseAndStripDiacriticsHeuristic();

        final ValuesPairSample<String, String> valuesPairSample = inboundValuesPair("ABCDÁČĎ", "abcdacd");
        assertTrue(heuristic.isApplicable(valuesPairSample));
    }

    @Test
    void inputContainsUppercaseLetters_heuristicsShouldBeApplicable() {
        final LowerCaseAndStripDiacriticsHeuristic heuristic = new LowerCaseAndStripDiacriticsHeuristic();
        final ValuesPairSample<String, String> valuesPairSample = inboundValuesPair("ABCD", "abcd");

        assertTrue(heuristic.isApplicable(valuesPairSample));
    }

    @Test
    void inputContainsDiacritics_heuristicsShouldBeApplicable() {
        final LowerCaseAndStripDiacriticsHeuristic heuristic = new LowerCaseAndStripDiacriticsHeuristic();

        final ValuesPairSample<String, String> valuesPairSample = inboundValuesPair("ÁČĎ", "acd");
        assertTrue(heuristic.isApplicable(valuesPairSample));
    }

    @Test
    void inputContainsUppercaseLettersAndDiacritics_inboundScriptIsEvaluated_outputShouldBeLowercaseWithoutDiacritics()
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
            ConfigurationException, ObjectNotFoundException {
        final LowerCaseAndStripDiacriticsHeuristic heuristic = new LowerCaseAndStripDiacriticsHeuristic();
        final ExpressionType expression = heuristic.inboundExpression(MappingScriptTestBase::createScriptExpression);

        final String output = this.evaluateExpression(expression, "input", "ABCDÁČĎ!");
        assertEquals(output, "abcdacd!");
    }

    @Test
    void propertyContainsUppercaseLettersAndDiacritics_outboundScriptIsEvaluated_outputShouldBeLowercaseWithoutDiacritics()
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
            ConfigurationException, ObjectNotFoundException {
        final String focusProperty = "name";
        final LowerCaseAndStripDiacriticsHeuristic heuristic = new LowerCaseAndStripDiacriticsHeuristic();
        final ExpressionType expression = heuristic.outboundExpression(focusProperty, MappingScriptTestBase::createScriptExpression);

        final String output = this.evaluateExpression(expression, focusProperty, "ABCDÁČĎ!");
        assertEquals(output, "abcdacd!");
    }

}