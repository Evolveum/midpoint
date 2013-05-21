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
package com.evolveum.midpoint.model.migrator;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import java.io.File;
import java.io.IOException;

import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectTemplateType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;

/**
 * @author semancik
 *
 */
public class TestMigrator {
	
	public static final File TEST_DIR = new File("src/test/resources/migrator");
	private static final File OBJECT_TEMPLATE_FILE = new File(TEST_DIR, "object-template.xml");
	private static final File USER_TEMPLATE_FILE = new File(TEST_DIR, "user-template.xml");
	
	@BeforeSuite
	public void setup() throws SchemaException, SAXException, IOException {
		PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
		PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
	}
	
	
	@Test
	public void testMigrateUserTemplate() throws Exception {
		IntegrationTestTools.displayTestTile("testMigrateUserTemplate");

		assertSimpleMigration(ObjectTemplateType.class, USER_TEMPLATE_FILE, OBJECT_TEMPLATE_FILE);
	}
	
	private <O extends ObjectType> void assertSimpleMigration(Class<O> type, File fileOld, File fileNew) throws SchemaException {
		// GIVEN
		Migrator migrator = createMigrator();
		
		PrismContext prismContext = PrismTestUtil.getPrismContext();
		PrismObject<O> objectOld = prismContext.parseObject(fileOld);
		TypedPrismObject<O> typedObjectOld = new TypedPrismObject<O>(type, objectOld);
		
		// WHEN
		TypedPrismObject<O> typedObjectNew = migrator.migrate(typedObjectOld);
		
		// THEN
		
		IntegrationTestTools.display("Migrated typed object", typedObjectNew);
		assertNotNull("No migrated typed object", typedObjectNew);
		assertEquals("Wrong output class", type, typedObjectNew.getType());

		PrismObject<O> objectNew = typedObjectNew.getObject();
		IntegrationTestTools.display("Migrated object", objectNew);
		
		PrismObject<O> expectedObject = prismContext.parseObject(fileNew);
		IntegrationTestTools.display("Expected object", expectedObject);

		PrismAsserts.assertEquivalent("Unexpected migration result", expectedObject, objectNew);
		assertEquals("Unexpected element name", expectedObject.getName(), objectNew.getName());
	}
	
	private Migrator createMigrator() {
		return new Migrator();
	}

}
