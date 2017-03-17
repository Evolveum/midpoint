/*
 * Copyright (c) 2010-2017 Evolveum
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

import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.util.exception.SchemaException;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import java.io.IOException;

/**
 * @author Radovan Semancik
 */
public class TestMappingStatic {

	private MappingTestEvaluator evaluator;
	    
    @BeforeClass
    public void setupFactory() throws SAXException, IOException, SchemaException {
    	evaluator = new MappingTestEvaluator();
    	evaluator.init();
    }
    
    @Test
    public void testValueSingleDeep() throws Exception {
        // WHEN
    	PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = evaluator.evaluateMapping(
    			"mapping-value-single-deep.xml",
    			"testValue",
    			"costCenter");				// target
    	
        // THEN
    	PrismAsserts.assertTripleZero(outputTriple, "foobar");
    	PrismAsserts.assertTripleNoPlus(outputTriple);
    	PrismAsserts.assertTripleNoMinus(outputTriple);
    }
    
    @Test
    public void testValueSingleShallow() throws Exception {
        // WHEN
    	PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = evaluator.evaluateMapping(
    			"mapping-value-single-shallow.xml",
    			"testValue",
    			"costCenter");				// target
    	
        // THEN
    	PrismAsserts.assertTripleZero(outputTriple, "foobar");
    	PrismAsserts.assertTripleNoPlus(outputTriple);
    	PrismAsserts.assertTripleNoMinus(outputTriple);
    }
    
    @Test
    public void testValueMultiDeep() throws Exception {
    	// WHEN
    	PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = evaluator.evaluateMapping(
    			"mapping-value-multi-deep.xml",
    			"testValueMulti",
    			"employeeType");				// target
    	
    	// THEN
    	PrismAsserts.assertTripleZero(outputTriple, "12345", "67890");
    	PrismAsserts.assertTripleNoPlus(outputTriple);
    	PrismAsserts.assertTripleNoMinus(outputTriple);    	
    }

    @Test
    public void testValueMultiShallow() throws Exception {
    	// WHEN
    	PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = evaluator.evaluateMapping(
    			"mapping-value-multi-shallow.xml",
    			"testValueMulti",
    			"employeeType");				// target
    	
    	// THEN
    	PrismAsserts.assertTripleZero(outputTriple, "12345", "67890");
    	PrismAsserts.assertTripleNoPlus(outputTriple);
    	PrismAsserts.assertTripleNoMinus(outputTriple);    	
    }
    
    @Test
    public void testPathNoSource() throws Exception {
    	// WHEN
    	PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = evaluator.evaluateMapping(
    			"mapping-path-system-variables-nosource.xml",
    			"testPathNoSource",
    			"employeeType");				// target
    	
    	// THEN
    	PrismAsserts.assertTripleZero(outputTriple, "jack");
    	PrismAsserts.assertTripleNoPlus(outputTriple);
    	PrismAsserts.assertTripleNoMinus(outputTriple);    	
    }
    
}
