/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.util;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.impl.xml.XmlTypeConverterInternal;
import org.testng.annotations.Test;
import org.w3c.dom.Document;

import com.evolveum.midpoint.tools.testng.AbstractUnitTest;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

/**
 * @author Radovan Semancik
 */
public class XsdTypeConverterTest extends AbstractUnitTest {

    // TODO: convert this test to create a Protected string structure in prism and then check it in the DOM view

    private static final String FOO_NAMESPACE = "http://foo.com/";
    private static final QName FOO_QNAME = new QName(FOO_NAMESPACE,"foo");

    @Test(enabled=false)
    public void testConvertToProtectedString() throws JAXBException, SchemaException {
        ProtectedStringType ps = new ProtectedStringType();
        ps.setClearValue("abra kadabra");
        Document doc = DOMUtil.getDocument();

        Object xsdElement = XmlTypeConverterInternal.toXsdElement(ps, FOO_QNAME, doc, true);

        System.out.println("ProtectedStringType -> XML");
        System.out.println(xsdElement);

        assertTrue(xsdElement instanceof JAXBElement);
        Object value = ((JAXBElement)xsdElement).getValue();

        System.out.println(value);
        assertTrue(value instanceof ProtectedStringType);
        assertEquals("abra kadabra",((ProtectedStringType)value).getClearValue());
    }
}
