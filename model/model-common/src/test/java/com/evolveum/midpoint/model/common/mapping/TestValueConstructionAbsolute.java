/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
