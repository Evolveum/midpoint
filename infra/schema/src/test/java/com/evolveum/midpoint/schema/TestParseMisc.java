/*
 * Copyright (c) 2010-2017 Evolveum
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
package com.evolveum.midpoint.schema;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.xml.namespace.QName;

import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * @author semancik
 *
 */
public class TestParseMisc {

	public static final File TEST_DIR = new File("src/test/resources/misc");
	
	protected static final File ROLE_FILTERS_FILE = new File(TEST_DIR, "role-filters.xml");
	protected static final String ROLE_FILTERS_OID = "aad19b9a-d511-11e7-8bf7-cfecde275e59";

	@BeforeSuite
	public void setup() throws SchemaException, SAXException, IOException {
		PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
		PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
		SchemaDebugUtil.initialize(); // Make sure the pretty printer is activated
	}


	@Test
	public void testParseRoleFilters() throws Exception {
		System.out.println("\n\n===[ testParseRoleFilters ]===\n");

		// GIVEN
		PrismContext prismContext = PrismTestUtil.getPrismContext();

		// WHEN
		PrismObject<RoleType> object = prismContext.parseObject(ROLE_FILTERS_FILE);

		// THEN
		System.out.println("Parsed object:");
		System.out.println(object.debugDump());

		assertEquals("Wrong oid", ROLE_FILTERS_OID, object.getOid());
		PrismObjectDefinition<RoleType> objectDefinition = object.getDefinition();
		assertNotNull("No object definition", objectDefinition);
		QName elementName = new QName(SchemaConstantsGenerated.NS_COMMON, "role");
		PrismAsserts.assertObjectDefinition(objectDefinition, elementName,
				RoleType.COMPLEX_TYPE, RoleType.class);
		assertEquals("Wrong class", RoleType.class, object.getCompileTimeClass());
		assertEquals("Wrong object item name", elementName, object.getElementName());
		RoleType objectType = object.asObjectable();
		assertNotNull("asObjectable resulted in null", objectType);

		assertPropertyValue(object, "name", PrismTestUtil.createPolyString("Role with import filters"));
		assertPropertyDefinition(object, "name", PolyStringType.COMPLEX_TYPE, 0, 1);
		
		List<AssignmentType> inducements = objectType.getInducement();
		assertEquals("Wrong number of inducements", 2, inducements.size());
	}


	private void assertPropertyDefinition(PrismContainer<?> container, String propName, QName xsdType, int minOccurs,
			int maxOccurs) {
		QName propQName = new QName(SchemaConstantsGenerated.NS_COMMON, propName);
		PrismAsserts.assertPropertyDefinition(container, propQName, xsdType, minOccurs, maxOccurs);
	}

	public static void assertPropertyValue(PrismContainer<?> container, String propName, Object propValue) {
		QName propQName = new QName(SchemaConstantsGenerated.NS_COMMON, propName);
		PrismAsserts.assertPropertyValue(container, propQName, propValue);
	}

	public static <T> void assertPropertyValues(PrismContainer<?> container, String propName, T... expectedValues) {
		QName propQName = new QName(SchemaConstantsGenerated.NS_COMMON, propName);
		PrismAsserts.assertPropertyValue(container, propQName, expectedValues);
	}


}
