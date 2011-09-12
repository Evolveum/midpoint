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
 * "Portions Copyrighted 2011 [name of copyright owner]"
 * 
 */
package com.evolveum.midpoint.model.importer;

import static com.evolveum.midpoint.schema.util.MiscUtil.getDefaultImportOptions;
import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static com.evolveum.midpoint.test.IntegrationTestTools.displayTestTile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.exception.ObjectNotFoundException;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;

/**
 * Test import with wrong data. Check if the import produces usable error messages.
 * 
 * @author Radovan Semancik
 *
 */
@ContextConfiguration(locations = { "classpath:application-context-model.xml",
		"classpath:application-context-repository.xml",
		"classpath:application-context-configuration-test.xml",
		"classpath:application-context-provisioning.xml",
		"classpath:application-context-task.xml" })
@DirtiesContext(classMode=ClassMode.AFTER_CLASS)
public class BadImportTest extends AbstractTestNGSpringContextTests {
	
	private static final File BAD_IMPORT_FILE_NAME = new File("src/test/resources/importer/import-bad.xml");
	private static final String USER_JACK_OID = "c0c010c0-d34d-b33f-f00d-111111111111";

	@Autowired(required = true)
	ModelService modelService;
	@Autowired(required = true)
	private RepositoryService repositoryService;
	@Autowired(required = true)
	private TaskManager taskManager;

	/**
	 * Test integrity of the test setup.
	 * 
	 */
	@Test
	public void test000Integrity() {
		displayTestTile(this,"test000Integrity");
		AssertJUnit.assertNotNull(modelService);
		AssertJUnit.assertNotNull(repositoryService);

	}

	@Test
	public void test001BadImport() throws FileNotFoundException, SchemaException {
		displayTestTile(this,"test001BadImport");
		// GIVEN
		Task task = taskManager.createTaskInstance();
		OperationResult result = new OperationResult(ImportTest.class.getName() + "test001GoodImport");
		FileInputStream stream = new FileInputStream(BAD_IMPORT_FILE_NAME);

		// WHEN
		modelService.importObjectsFromStream(stream, getDefaultImportOptions(), task, result);

		// THEN
		result.computeStatus("Failed import.");
		display("Result after bad import", result);

		// Jack is OK in the import file, he should be imported
		try {
			UserType jack = repositoryService.getObject(UserType.class, USER_JACK_OID, null, result);
			AssertJUnit.assertNotNull("Jack is null", jack);
		} catch (ObjectNotFoundException e) {
			AssertJUnit.fail("Jack was not imported");
		}
		
		List<UserType> users = repositoryService.listObjects(UserType.class, null, result);

		AssertJUnit.assertNotNull(users);
		AssertJUnit.assertEquals("Search retuned unexpected results", 2, users.size());

	}
	
}
