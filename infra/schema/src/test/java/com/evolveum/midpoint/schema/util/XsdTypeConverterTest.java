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

/**
 *
 */
package com.evolveum.midpoint.schema.util;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.io.IOException;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.util.JaxbTestUtil;

import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

/**
 * @author Radovan Semancik
 *
 */
public class XsdTypeConverterTest {

	// TODO: convert this test to create a Protected string structure in prism and then check it in the DOM view

	private static final String FOO_NAMESPACE = "http://foo.com/";
	private static final QName FOO_QNAME = new QName(FOO_NAMESPACE,"foo");
	private static final QName BAR_QNAME = new QName(FOO_NAMESPACE,"bar");


	@Test(enabled=false)
	public void testConvertFromProtectedString() throws SchemaException {
		Document document = DOMUtil.parseDocument(
				"<password xmlns=\""+FOO_NAMESPACE+"\" "+
				"xmlns:c=\"http://midpoint.evolveum.com/xml/ns/public/common/common-3\" "+
				"xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "+
				"xsi:type=\"c:ProtectedStringType\">"+
				"<c:clearValue>3lizab3th</c:clearValue></password>");
		Element element = DOMUtil.getFirstChildElement(document);

		Object value = XmlTypeConverter.toJavaValue(element);

		System.out.println("XML -> ProtectedStringType: "+value);
		assertNotNull(value);
		assertTrue(value instanceof ProtectedStringType);
		assertEquals("3lizab3th",((ProtectedStringType)value).getClearValue());
	}

	@Test(enabled=false)
	public void testConvertToProtectedString() throws JAXBException, SchemaException {
		ProtectedStringType ps = new ProtectedStringType();
		ps.setClearValue("abra kadabra");
		Document doc = DOMUtil.getDocument();

		Object xsdElement = XmlTypeConverter.toXsdElement(ps, FOO_QNAME, doc, true);

		System.out.println("ProtectedStringType -> XML");
		System.out.println(xsdElement);

		assertTrue(xsdElement instanceof JAXBElement);
		Object value = ((JAXBElement)xsdElement).getValue();

		System.out.println(value);
		assertTrue(value instanceof ProtectedStringType);
		assertEquals("abra kadabra",((ProtectedStringType)value).getClearValue());
	}

    @Deprecated // ... as it uses JAXB that is no more supported
	@Test(enabled=false)
	public void testAccountMarshall() throws JAXBException, SchemaException, IOException {
		System.out.println("===[ testAccountMarshall ]===");
		ShadowType shadow =
                PrismTestUtil.parseObjectable(new File("src/test/resources/converter/account-jack.xml"), ShadowType.class);
		System.out.println("Object: "+shadow);

		ProtectedStringType ps = new ProtectedStringType();
		ps.setClearValue("foo");
		JAXBElement<ProtectedStringType> pse = new JAXBElement<ProtectedStringType>(FOO_QNAME,ProtectedStringType.class,ps);
		shadow.getAttributes().getAny().add(pse);

		shadow.getAttributes().getAny().add(XmlTypeConverter.toXsdElement(42, BAR_QNAME, null, true));

		Document doc = DOMUtil.getDocument();
		JAXBElement<ShadowType> accountElement =
				new JAXBElement<ShadowType>(ObjectTypes.SHADOW.getQName(),
						ShadowType.class,shadow);
		JaxbTestUtil.getInstance().marshalElementToDom(accountElement, doc);

		System.out.println("marshalled shadow: "+DOMUtil.serializeDOMToString(doc));
		Element rootElement = DOMUtil.getFirstChildElement(doc);
		System.out.println("root element: "+rootElement);

		Element attrElement = (Element) rootElement.getElementsByTagNameNS(SchemaConstants.NS_C,"attributes").item(0);
		System.out.println("attrElement element: "+attrElement);

		Element fooElement = (Element) attrElement.getElementsByTagNameNS(FOO_QNAME.getNamespaceURI(), FOO_QNAME.getLocalPart()).item(0);
		System.out.println("fooElement element: "+fooElement);
		Element clearValue = DOMUtil.getFirstChildElement(fooElement);
		assertEquals("foo",clearValue.getTextContent());

		Element barElement = (Element) attrElement.getElementsByTagNameNS(BAR_QNAME.getNamespaceURI(), BAR_QNAME.getLocalPart()).item(0);
		System.out.println("barElement element: "+barElement);
		assertEquals(DOMUtil.XSD_INT,DOMUtil.resolveXsiType(barElement));
		assertEquals("42",barElement.getTextContent());
	}

}
