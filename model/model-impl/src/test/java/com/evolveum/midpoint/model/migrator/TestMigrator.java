/**
 * Copyright (c) 2013 Evolveum
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
 * Portions Copyrighted 2013 [name of copyright owner]
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
