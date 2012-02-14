/**
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
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.schema.util;

import static org.testng.AssertJUnit.assertTrue;

import java.io.File;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import org.testng.annotations.Test;

import com.evolveum.midpoint.schema.JaxbTestUtil;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.util.JAXBUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;

/**
 * @author Pavol Mederly
 *
 */
public class JAXBUtilTest {

	@Test
	public void testUnmarshallerUtf() throws JAXBException, SchemaException {
		// GIVEN
		
		UserType user = (UserType) ((JAXBElement) JaxbTestUtil.unmarshalElement(new File("src/test/resources/util/user-utf8.xml")))
				.getValue();
		
		// WHEN

		String fullname = user.getFullName();
		
		// THEN
		
		assertTrue("National characters incorrectly decoded", "Jožko Nováčik".equals(fullname));
	}

	@Test
	public void testUnmarshallerIso88592() throws JAXBException, SchemaException {
		// GIVEN
		
		UserType user = (UserType) ((JAXBElement) JaxbTestUtil.unmarshalElement(new File("src/test/resources/util/user-8859-2.xml")))
				.getValue();
		
		// WHEN

		String fullname = user.getFullName();
		
		// THEN
		
		assertTrue("National characters incorrectly decoded", "Jožko Nováčik".equals(fullname));
	}

	@Test
	public void testUnmarshallerStringUtf8() throws JAXBException, SchemaException {
		// GIVEN

		String s = "<?xml version='1.0' encoding='utf-8'?> " +
				"<user oid='deadbeef-c001-f00d-1111-222233330001'" +
				"      xmlns='http://midpoint.evolveum.com/xml/ns/public/common/common-1.xsd'>" +
				"	<fullName>Jožko Nováčik</fullName>" +
				"</user>";

		UserType user = (UserType) ((JAXBElement) JaxbTestUtil.unmarshalElement(s)).getValue();
		
		// WHEN

		String fullname = user.getFullName();
		
		// THEN
		
		assertTrue("Diacritics correctly decoded", "Jožko Nováčik".equals(fullname));
	}
	
	@Test
	public void testUnmarshallerStringIso88592() throws JAXBException, SchemaException {
		// GIVEN

		String s = "<?xml version='1.0' encoding='iso-8859-2'?> " +
				"<user oid='deadbeef-c001-f00d-1111-222233330001'" +
				"      xmlns='http://midpoint.evolveum.com/xml/ns/public/common/common-1.xsd'>" +
				"	<fullName>Jožko Nováčik</fullName>" +
				"</user>";

		UserType user = (UserType) ((JAXBElement) JaxbTestUtil.unmarshalElement(s)).getValue();
		
		// WHEN

		String fullname = user.getFullName();
		
		// THEN
		
		assertTrue("Diacritics correctly decoded", "Jožko Nováčik".equals(fullname));
	}

}
