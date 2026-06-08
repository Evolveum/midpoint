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

public class StripDiacriticsHeuristicTest extends MappingScriptTestBase {

    public StripDiacriticsHeuristicTest() throws SchemaException, IOException, SAXException {
    }

    @Test
    void inputContainsDiacritics_heuristicsShouldBeApplicable() {
        final StripDiacriticsHeuristic heuristic = new StripDiacriticsHeuristic();
        final ValuesPairSample<String, String> valuesPairSample = inboundValuesPair("José", "Jose");

        assertTrue(heuristic.isApplicable(valuesPairSample));
    }

    @Test
    void inputDoesNotContainDiacritics_heuristicsShouldNotBeApplicable() {
        final StripDiacriticsHeuristic heuristic = new StripDiacriticsHeuristic();
        final ValuesPairSample<String, String> valuesPairSample = inboundValuesPair("Jose", "Jose");

        assertFalse(heuristic.isApplicable(valuesPairSample));
    }

    @Test
    void inputContainsDiacritics_inboundScriptIsEvaluated_outputShouldBeWithoutDiacritics()
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
            ConfigurationException, ObjectNotFoundException {
        final StripDiacriticsHeuristic heuristic = new StripDiacriticsHeuristic();

        final ExpressionType expression = heuristic.inboundExpression(MappingScriptTestBase::createScriptExpression);
        final String output = this.evaluateExpression(expression, "input", "José Müller!");

        assertEquals(output, "Jose Muller!");
    }

    @Test
    void propertyContainsDiacritics_outboundScriptIsEvaluated_outputShouldBeWithoutDiacritics()
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
            ConfigurationException, ObjectNotFoundException {
        final String focusProperty = "name";
        final StripDiacriticsHeuristic heuristic = new StripDiacriticsHeuristic();

        final ExpressionType expression = heuristic.outboundExpression(focusProperty, MappingScriptTestBase::createScriptExpression);
        final String output = this.evaluateExpression(expression, focusProperty, "José Müller!");

        assertEquals(output, "Jose Muller!");
    }

}