/*
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.common.expression;

import com.evolveum.midpoint.common.expression.evaluator.LiteralExpressionEvaluatorFactory;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.JAXBUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;

import org.testng.AssertJUnit;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

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
    	System.out.println("\n===[ testValueElementsRoundtripString ]===\n");
    	
    	// GIVEN
    	PrismContext prismContext = PrismTestUtil.getPrismContext();
    	PrismPropertyDefinition propDef = new PrismPropertyDefinition(PROP_NAME, PROP_NAME, DOMUtil.XSD_STRING, prismContext);
    	propDef.setMaxOccurs(-1);
    	PrismProperty<String> origProperty = propDef.instantiate();
    	origProperty.addRealValue("FOO");
    	origProperty.addRealValue("BAR");
    
    	doRoundtrip(origProperty, propDef, prismContext);
    }
    
    @Test
    public void testValueElementsRoundtripInt() throws Exception {
    	System.out.println("\n===[ testValueElementsRoundtripInt ]===\n");
    	
    	// GIVEN
    	PrismContext prismContext = PrismTestUtil.getPrismContext();
    	PrismPropertyDefinition propDef = new PrismPropertyDefinition(PROP_NAME, PROP_NAME, DOMUtil.XSD_INT, prismContext);
    	propDef.setMaxOccurs(-1);
    	PrismProperty<Integer> origProperty = propDef.instantiate();
    	origProperty.addRealValue(42);
    	origProperty.addRealValue(123);
    	origProperty.addRealValue(321);
    
    	doRoundtrip(origProperty, propDef, prismContext);
    }

    private void doRoundtrip(PrismProperty<?> origProperty, ItemDefinition propDef, PrismContext prismContext) throws SchemaException {
    	// WHEN
    	List<?> valueElements = LiteralExpressionEvaluatorFactory.serializeValueElements(origProperty, "here somewhere");
    	
    	for (Object element: valueElements) {
    		if (element instanceof Element) {
    			System.out.println(DOMUtil.serializeDOMToString((Element)element));
    		} else {
    			AssertJUnit.fail("Unexpected element type "+element.getClass());
    		}
    	}

    	PrismProperty<String> parsedPropery = (PrismProperty<String>)(Item) LiteralExpressionEvaluatorFactory.parseValueElements(valueElements, propDef, "here again", prismContext);

        // THEN
    	assertEquals("Roundtrip failed", origProperty, parsedPropery);
    }

}
