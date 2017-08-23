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
package com.evolveum.midpoint.model.impl.migrator;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.model.impl.migrator.Migrator;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import difflib.DiffUtils;
import difflib.Patch;

/**
 * @author semancik
 *
 */
public class TestMigrator {
	
	public static final File TEST_DIR = new File("src/test/resources/migrator");
	private static final File TEST_DIR_BEFORE = new File(TEST_DIR, "before");
	private static final File TEST_DIR_AFTER = new File(TEST_DIR, "after");
	
	@BeforeSuite
	public void setup() throws SchemaException, SAXException, IOException {
		PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
		PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
	}
	
	
	@Test
	public void testMigrateUserTemplate() throws Exception {
		TestUtil.displayTestTitle("testMigrateUserTemplate");

		for (File beforeFile: TEST_DIR_BEFORE.listFiles()) {
			String beforeName = beforeFile.getName();
			if (!beforeName.endsWith(".xml")) {
				continue;
			}
			File afterFile = new File(TEST_DIR_AFTER, beforeName);
			
			assertSimpleMigration(beforeFile, afterFile);
		}
	}
	
	@Test
	public void testUserCredentials() throws Exception{
		Migrator migrator = createMigrator();
		
		PrismContext prismContext = PrismTestUtil.getPrismContext();
		PrismObject<UserType> oldUser = prismContext.parseObject(new File(TEST_DIR + "/user-migrate-credentials.xml"));
		
		PrismObject<UserType> newUser = migrator.migrate(oldUser);
		
		UserType newUserType = newUser.asObjectable();
		
		assertNull("Credentials in migrated object must be null.", newUserType.getCredentials());
		assertNotNull("Migrated user must contain assignment.", newUserType.getAssignment());
		assertEquals("Migrated user must contain 1 assignment.", newUserType.getAssignment().size(), 1);
		
		AssignmentType superUserRole = newUserType.getAssignment().get(0);
		
		assertNotNull("Target ref in the user's assignment must not be null.", superUserRole.getTargetRef());
		assertEquals(superUserRole.getTargetRef().getOid(), SystemObjectsType.ROLE_SUPERUSER.value());
	}
	
	private <O extends ObjectType> void assertSimpleMigration(File fileOld, File fileNew) throws SchemaException, IOException {
		// GIVEN
		Migrator migrator = createMigrator();
		
		PrismContext prismContext = PrismTestUtil.getPrismContext();
		PrismObject<O> objectOld = prismContext.parseObject(fileOld);
		
		// WHEN
		PrismObject<O> objectNew = migrator.migrate(objectOld);
		
		// THEN
		
		IntegrationTestTools.display("Migrated object "+fileOld.getName(), objectNew);
		assertNotNull("No migrated object "+fileOld.getName(), objectNew);

		IntegrationTestTools.display("Migrated object "+fileOld.getName(), objectNew);
		String migratedXml = PrismTestUtil.serializeObjectToString(objectNew, PrismContext.LANG_XML);
		IntegrationTestTools.display("Migrated object XML "+fileOld.getName(), migratedXml);
		
		PrismObject<O> expectedObject = prismContext.parseObject(fileNew);
		IntegrationTestTools.display("Expected object "+fileOld.getName(), expectedObject);
		String expectedXml = PrismTestUtil.serializeObjectToString(expectedObject, PrismContext.LANG_XML);
		IntegrationTestTools.display("Expected object XML "+fileOld.getName(), expectedXml);
		
		List<String> expectedXmlLines = MiscUtil.splitLines(expectedXml);
		Patch patch = DiffUtils.diff(expectedXmlLines, MiscUtil.splitLines(migratedXml));
		List<String> diffLines = DiffUtils.generateUnifiedDiff(fileOld.getPath(), fileNew.getPath(), expectedXmlLines, patch, 3);
		IntegrationTestTools.display("XML textual diff", StringUtils.join(diffLines, '\n'));

		PrismAsserts.assertEquivalent("Unexpected migration result for "+fileOld.getName(), expectedObject, objectNew);
		assertEquals("Unexpected element name for "+fileOld.getName(), expectedObject.getElementName(), objectNew.getElementName());
	}
	
	private Migrator createMigrator() {
		return new Migrator();
	}

}
