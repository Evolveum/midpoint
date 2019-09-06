/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.util;

import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.xml.bind.JAXBException;

import com.evolveum.midpoint.prism.impl.util.JaxbTestUtil;
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
