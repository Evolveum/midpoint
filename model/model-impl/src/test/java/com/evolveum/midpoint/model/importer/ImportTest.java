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

import static com.evolveum.midpoint.test.IntegrationTestTools.assertSuccess;
import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static com.evolveum.midpoint.test.IntegrationTestTools.displayTestTile;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.evolveum.midpoint.common.QueryUtil;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.xml.XmlRepositoryServiceFactory;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.exception.ObjectNotFoundException;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_1.QueryType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;

/**
 * @author Radovan Semancik
 * 
 */
@ContextConfiguration(locations = { "classpath:application-context-model.xml",
		"classpath:application-context-repository.xml",
		"classpath:application-context-configuration-test.xml",
		"classpath:application-context-provisioning.xml",
		"classpath:application-context-task.xml" })
//@DirtiesContext(classMode=ClassMode.AFTER_CLASS)
public class ImportTest extends AbstractTestNGSpringContextTests {

	private static final File IMPORT_FILE_NAME = new File("src/test/resources/importer/import.xml");
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
		assertNotNull(modelService);
		assertNotNull(repositoryService);

	}

	@Test
	public void test001GoodImport() throws FileNotFoundException, ObjectNotFoundException, SchemaException {
		displayTestTile(this,"test001GoodImport");
		// GIVEN
		Task task = taskManager.createTaskInstance();
		OperationResult result = new OperationResult(ImportTest.class.getName() + "test001GoodImport");
		FileInputStream stream = new FileInputStream(IMPORT_FILE_NAME);

		// WHEN
		modelService.importObjectsFromStream(stream, task, false, result);

		// THEN
		result.computeStatus("Failed import.");
		display("Result after good import", result);
		assertSuccess("Import has failed (result)", result);

		// Check import with fixed OID
		UserType jack = repositoryService.getObject(UserType.class, USER_JACK_OID, null, result);
		assertNotNull(jack);
		assertEquals("Jack", jack.getGivenName());
		assertEquals("Sparrow", jack.getFamilyName());
		assertEquals("Cpt. Jack Sparrow", jack.getFullName());

		// Check import with generated OID
		Document doc = DOMUtil.getDocument();
		Element filter = QueryUtil.createAndFilter(doc,
				QueryUtil.createTypeFilter(doc, ObjectTypes.USER.getObjectTypeUri()),
				QueryUtil.createEqualFilter(doc, null, SchemaConstants.C_NAME, "guybrush"));

		QueryType query = new QueryType();
		query.setFilter(filter);

		List<UserType> users = repositoryService.searchObjects(UserType.class, query, null, result);

		assertNotNull(users);
		assertEquals("Search retuned unexpected results", 1, users.size());
		UserType guybrush = users.get(0);
		assertNotNull(guybrush);
		assertEquals("Guybrush", guybrush.getGivenName());
		assertEquals("Threepwood", guybrush.getFamilyName());
		assertEquals("Guybrush Threepwood", guybrush.getFullName());

	}

	// Import the same thing again. Watch how it burns :-)
	@Test
	public void test002DuplicateImport() throws FileNotFoundException, ObjectNotFoundException,
			SchemaException {
		displayTestTile(this,"test002DupicateImport");
		// GIVEN
		Task task = taskManager.createTaskInstance();
		OperationResult result = new OperationResult(ImportTest.class.getName() + "test002DupicateImport");
		FileInputStream stream = new FileInputStream(IMPORT_FILE_NAME);

		// WHEN
		modelService.importObjectsFromStream(stream, task, false, result);

		// THEN
		result.computeStatus("Failed import.");
		display("Result after dupicate import", result);
		assertFalse("Unexpected success", result.isSuccess());

		// All three users should fail. First two because of OID conflict,
		// guybrush because of name conflict
		// (nobody else could have such a stupid name)
		for (OperationResult subresult : result.getSubresults().get(0).getSubresults()) {
			assertFalse("Unexpected success in subresult", subresult.isSuccess());
		}

	}

}
