/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.common;

import static org.testng.AssertJUnit.assertEquals;

import java.io.IOException;
import java.util.List;

import com.evolveum.midpoint.schema.util.SchemaDebugUtil;

import jakarta.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import org.testng.AssertJUnit;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.tools.testng.AbstractUnitTest;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.prism.xml.ns._public.types_3.RawType;

/**
 * @author semancik
 */
public class TestStaticValues extends AbstractUnitTest {

    private static final QName PROP_NAME = new QName("http://whatever.com/", "foo");

    @BeforeSuite
    public void setup() throws SchemaException, SAXException, IOException {
        SchemaDebugUtil.initializePrettyPrinter();
        PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
    }

    @Test
    public void testValueElementsRoundtripString() throws Exception {
        PrismContext prismContext = PrismTestUtil.getPrismContext();
        PrismPropertyDefinition<String> propDef = prismContext.definitionFactory().newPropertyDefinition(PROP_NAME, DOMUtil.XSD_STRING);
        propDef.mutator().setMaxOccurs(-1);
        PrismProperty<String> origProperty = propDef.instantiate();
        origProperty.addRealValue("FOO");
        origProperty.addRealValue("BAR");

        doRoundtrip(origProperty, propDef);
    }

    @Test
    public void testValueElementsRoundtripInt() throws Exception {
        PrismContext prismContext = PrismTestUtil.getPrismContext();
        PrismPropertyDefinition<Integer> propDef = prismContext.definitionFactory().newPropertyDefinition(PROP_NAME, DOMUtil.XSD_INT);
        propDef.mutator().setMaxOccurs(-1);
        PrismProperty<Integer> origProperty = propDef.instantiate();
        origProperty.addRealValue(42);
        origProperty.addRealValue(123);
        origProperty.addRealValue(321);

        doRoundtrip(origProperty, propDef);
    }

    private void doRoundtrip(PrismProperty<?> origProperty, ItemDefinition propDef) throws SchemaException {
        // WHEN
        List<JAXBElement<RawType>> valueElements = StaticExpressionUtil.serializeValueElements(origProperty);

        for (Object element : valueElements) {
            if (element instanceof JAXBElement) {
                System.out.println(PrismTestUtil.serializeJaxbElementToString((JAXBElement) element));
            } else {
                AssertJUnit.fail("Unexpected element type " + element.getClass());
            }
        }

        PrismProperty<String> parsedProperty = (PrismProperty<String>) (Item) StaticExpressionUtil.parseValueElements(valueElements, propDef, "here again");

        // THEN
        assertEquals("Roundtrip failed", origProperty, parsedProperty);
    }

}
