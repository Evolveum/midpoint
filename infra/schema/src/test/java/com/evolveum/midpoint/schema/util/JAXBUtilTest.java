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
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import com.evolveum.prism.xml.ns._public.types_2.PolyStringType;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.JAXBUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_2.UserType;

/**
 * @author Pavol Mederly
 *
 */
public class JAXBUtilTest {
	
	@BeforeSuite
	public void setup() throws SchemaException, SAXException, IOException {
		DebugUtil.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
		PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
	}

	@Test
	public void testUnmarshallerUtf() throws JAXBException, SchemaException, FileNotFoundException {
		// GIVEN
		
		UserType user = PrismTestUtil.unmarshalElement(new File("src/test/resources/util/user-utf8.xml"), UserType.class)
				.getValue();
		
		// WHEN

        PolyStringType fullName = user.getFullName();

		// THEN

		assertTrue("National characters incorrectly decoded", "Jožko Nováčik".equals(fullName.getOrig()));
	}

	@Test
	public void testUnmarshallerIso88592() throws JAXBException, SchemaException, FileNotFoundException {
		// GIVEN
		
		UserType user = PrismTestUtil.unmarshalElement(new File("src/test/resources/util/user-8859-2.xml"),UserType.class)
				.getValue();
		
		// WHEN

        PolyStringType fullname = user.getFullName();

		// THEN

		assertTrue("National characters incorrectly decoded", "Jožko Nováčik".equals(fullname.getOrig()));
	}

	@Test
	public void testUnmarshallerStringUtf8() throws JAXBException, SchemaException {
		// GIVEN

		String s = "<?xml version='1.0' encoding='utf-8'?> " +
				"<user oid='deadbeef-c001-f00d-1111-222233330001'" +
                "      xmlns:t='http://prism.evolveum.com/xml/ns/public/types-2'" +
				"      xmlns='http://midpoint.evolveum.com/xml/ns/public/common/common-2'>" +
				"	<fullName><t:orig>Jožko Nováčik</t:orig><t:norm>jozko novacik</t:norm></fullName>" +
				"</user>";

		UserType user = PrismTestUtil.unmarshalElement(s, UserType.class).getValue();
		
		// WHEN

        PolyStringType fullname = user.getFullName();

		// THEN

		assertTrue("Diacritics correctly decoded", "Jožko Nováčik".equals(fullname.getOrig()));
	}
	
	@Test
	public void testUnmarshallerStringIso88592() throws JAXBException, SchemaException {
		// GIVEN

		String s = "<?xml version='1.0' encoding='iso-8859-2'?> " +
				"<user oid='deadbeef-c001-f00d-1111-222233330001'" +
                "      xmlns:t='http://prism.evolveum.com/xml/ns/public/types-2'" +
				"      xmlns='http://midpoint.evolveum.com/xml/ns/public/common/common-2'>" +
				"	<fullName><t:orig>Jožko Nováčik</t:orig><t:norm>jozko novacik</t:norm></fullName>" +
				"</user>";

		UserType user = PrismTestUtil.unmarshalElement(s,UserType.class).getValue();
		
		// WHEN

        PolyStringType fullname = user.getFullName();

		// THEN

		assertTrue("Diacritics correctly decoded", "Jožko Nováčik".equals(fullname.getOrig()));
	}

}
