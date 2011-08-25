/**
 * 
 */
package com.evolveum.midpoint.schema.util;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;
import java.io.IOException;
import java.util.Set;

import javax.xml.bind.JAXBException;
import javax.xml.bind.JAXBIntrospector;
import javax.xml.namespace.QName;

import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.evolveum.midpoint.schema.XsdTypeConverter;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_1.Objects;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ProtectedStringType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;

/**
 * @author Radovan Semancik
 *
 */
public class XsdTypeConverterTest {
		
	@Test
	public void testXsdMappingInitialization() {
		assertTrue(XsdTypeConverter.canConvert(ProtectedStringType.class));
		QName xsdType = XsdTypeConverter.toXsdType(ProtectedStringType.class);
		System.out.println("ProtectedStringType QName: "+xsdType);
	}
	
	@Test
	public void testConvertProtectedString() throws JAXBException {
		Document document = DOMUtil.parseDocument(
				"<password xmlns=\"http://foo.com/bar\" "+
				"xmlns:c=\"http://midpoint.evolveum.com/xml/ns/public/common/common-1.xsd\" "+
				"xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "+
				"xsi:type=\"c:ProtectedStringType\">"+
				"<c:clearValue>3lizab3th</c:clearValue></password>");
		Element element = DOMUtil.getFirstChildElement(document);
		
		Object value = XsdTypeConverter.toJavaValue(element);
		
		System.out.println("Converted ProtectedStringType: "+value);
		assertNotNull(value);
		assertTrue(value instanceof ProtectedStringType);
		assertEquals("3lizab3th",((ProtectedStringType)value).getClearValue());
	}
	
}
