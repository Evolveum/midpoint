/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.common.mapping;

import com.evolveum.midpoint.util.exception.SchemaException;
import org.testng.annotations.BeforeClass;
import org.xml.sax.SAXException;

import java.io.IOException;

/**
 * @author Radovan Semancik
 */
public class TestValueConstructionAbsolute {

	private MappingTestEvaluator evaluator;

    @BeforeClass
    public void setupFactory() throws SAXException, IOException, SchemaException {
    	evaluator = new MappingTestEvaluator();
    	evaluator.init();
    }


//    @Test
//    public void testConstructionExpressionInputMulti() throws JAXBException, ExpressionEvaluationException, ObjectNotFoundException, SchemaException, FileNotFoundException {
//    	// WHEN
//    	PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = evaluator.evaluateMappingDynamicAdd(String.class,
//    			"absolute/construction-expression-input-multi.xml", "employeeType", "rock", null, "testConstructionExpressionSimple",
//    			"apple", "orange");
//
//        // THEN
//    	PrismAsserts.assertTripleZero(outputTriple, "apple", "orange", "rock" );
//    	PrismAsserts.assertTripleNoPlus(outputTriple);
//    	PrismAsserts.assertTripleNoMinus(outputTriple);
//    }

}
