/*
 * Copyright (c) 2016 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema;

import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

import java.io.IOException;

import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.util.SchemaTestUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author semancik
 *
 */
public class TestObjectConstruction {

	@BeforeSuite
	public void setup() throws SchemaException, SAXException, IOException {
		PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
		PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
	}

	@Test
	public void testUserConstruction() throws Exception {
		System.out.println("\n\n ===[ testUserConstruction ]===\n");

		// GIVEN
		PrismContext prismContext = PrismTestUtil.getPrismContext();

		// WHEN
		PrismObject<UserType> user = prismContext.createObject(UserType.class);

		// THEN
		assertNotNull(user);
		SchemaTestUtil.assertUserDefinition(user.getDefinition());
	}

	@Test
	public void testObjectTypeConstruction() throws Exception {
		System.out.println("\n\n ===[ testObjectTypeConstruction ]===\n");

		// GIVEN
		PrismContext prismContext = PrismTestUtil.getPrismContext();

		try {
			// WHEN
			PrismObject<ObjectType> object = prismContext.createObject(ObjectType.class);

			fail("unexpected success");
		} catch (SchemaException e) {
			// This is expected, abstract object types cannot be instantiated
			assertTrue(e.getMessage().contains("abstract"));
		}

	}


}
