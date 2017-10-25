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
package com.evolveum.midpoint.common;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;

import com.evolveum.prism.xml.ns._public.types_3.RawType;
import org.testng.AssertJUnit;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.List;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import static org.testng.AssertJUnit.*;

/**
 * @author semancik
 */
public class TestStaticValues {

    private static final QName PROP_NAME = new QName("http://whatever.com/", "foo");

	@BeforeSuite
	public void setup() throws SchemaException, SAXException, IOException {
		PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
		PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
	}

    @Test
    public void testValueElementsRoundtripString() throws Exception {
    	final String TEST_NAME = "testValueElementsRoundtripString";
    	TestUtil.displayTestTitle(TEST_NAME);

    	// GIVEN
    	PrismContext prismContext = PrismTestUtil.getPrismContext();
    	PrismPropertyDefinitionImpl propDef = new PrismPropertyDefinitionImpl(PROP_NAME, DOMUtil.XSD_STRING, prismContext);
    	propDef.setMaxOccurs(-1);
    	PrismProperty<String> origProperty = propDef.instantiate();
    	origProperty.addRealValue("FOO");
    	origProperty.addRealValue("BAR");

    	doRoundtrip(origProperty, propDef, prismContext);
    }

    @Test
    public void testValueElementsRoundtripInt() throws Exception {
    	final String TEST_NAME = "testValueElementsRoundtripInt";
    	TestUtil.displayTestTitle(TEST_NAME);

    	// GIVEN
    	PrismContext prismContext = PrismTestUtil.getPrismContext();
    	PrismPropertyDefinitionImpl propDef = new PrismPropertyDefinitionImpl(PROP_NAME, DOMUtil.XSD_INT, prismContext);
    	propDef.setMaxOccurs(-1);
    	PrismProperty<Integer> origProperty = propDef.instantiate();
    	origProperty.addRealValue(42);
    	origProperty.addRealValue(123);
    	origProperty.addRealValue(321);

    	doRoundtrip(origProperty, propDef, prismContext);
    }

    private void doRoundtrip(PrismProperty<?> origProperty, ItemDefinition propDef, PrismContext prismContext) throws SchemaException, JAXBException {
    	// WHEN
    	List<JAXBElement<RawType>> valueElements = StaticExpressionUtil.serializeValueElements(origProperty, "here somewhere");

    	for (Object element: valueElements) {
    		if (element instanceof JAXBElement) {
    			System.out.println(PrismTestUtil.serializeJaxbElementToString((JAXBElement) element));
    		} else {
    			AssertJUnit.fail("Unexpected element type "+element.getClass());
    		}
    	}

    	PrismProperty<String> parsedProperty = (PrismProperty<String>)(Item) StaticExpressionUtil.parseValueElements(valueElements, propDef, "here again");

        // THEN
    	assertEquals("Roundtrip failed", origProperty, parsedProperty);
    }

}
