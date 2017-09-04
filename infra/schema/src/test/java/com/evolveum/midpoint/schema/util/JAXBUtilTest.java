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
package com.evolveum.midpoint.schema.util;

import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.xml.bind.JAXBException;

import com.evolveum.midpoint.prism.util.JaxbTestUtil;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author Pavol Mederly
 *
 */
@Deprecated
public class JAXBUtilTest {

	@BeforeSuite
	public void setup() throws SchemaException, SAXException, IOException {
		PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
		PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
	}

	@Test
	public void testUnmarshallerUtf() throws JAXBException, SchemaException, FileNotFoundException {
		// GIVEN

		UserType user = JaxbTestUtil.getInstance().unmarshalElement(new File("src/test/resources/util/user-utf8.xml"), UserType.class)
				.getValue();

		// WHEN

        PolyStringType fullName = user.getFullName();

		// THEN

		assertTrue("National characters incorrectly decoded", "Jožko Nováčik".equals(fullName.getOrig()));
	}

	@Test
	public void testUnmarshallerIso88592() throws JAXBException, SchemaException, FileNotFoundException {
		// GIVEN

		UserType user = JaxbTestUtil.getInstance().unmarshalElement(new File("src/test/resources/util/user-8859-2.xml"),UserType.class)
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
                "      xmlns:t='http://prism.evolveum.com/xml/ns/public/types-3'" +
				"      xmlns='http://midpoint.evolveum.com/xml/ns/public/common/common-3'>" +
				"	<fullName><t:orig>Jožko Nováčik</t:orig><t:norm>jozko novacik</t:norm></fullName>" +
				"</user>";

		UserType user = JaxbTestUtil.getInstance().unmarshalElement(s, UserType.class).getValue();

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
                "      xmlns:t='http://prism.evolveum.com/xml/ns/public/types-3'" +
				"      xmlns='http://midpoint.evolveum.com/xml/ns/public/common/common-3'>" +
				"	<fullName><t:orig>Jožko Nováčik</t:orig><t:norm>jozko novacik</t:norm></fullName>" +
				"</user>";

		UserType user = JaxbTestUtil.getInstance().unmarshalElement(s, UserType.class).getValue();

		// WHEN

        PolyStringType fullname = user.getFullName();

		// THEN

		assertTrue("Diacritics correctly decoded", "Jožko Nováčik".equals(fullname.getOrig()));
	}

}
