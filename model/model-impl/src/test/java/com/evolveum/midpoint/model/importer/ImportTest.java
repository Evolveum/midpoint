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

import static com.evolveum.midpoint.schema.util.MiscSchemaUtil.getDefaultImportOptions;
import static com.evolveum.midpoint.test.IntegrationTestTools.assertSuccess;
import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static com.evolveum.midpoint.test.IntegrationTestTools.displayTestTile;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.derby.impl.sql.compile.TestConstraintNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.evolveum.midpoint.common.QueryUtil;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.ResultList;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.SchemaTestConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.JAXBUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ImportOptionsType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ProtectedStringType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.QueryType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;

/**
 * @author Radovan Semancik
 * 
 */
@ContextConfiguration(locations = { "classpath:application-context-model.xml",
		"classpath:application-context-repository.xml",
		"classpath:application-context-repo-cache.xml",
		"classpath:application-context-configuration-test.xml",
		"classpath:application-context-provisioning.xml",
		"classpath:application-context-task.xml",
		"classpath:application-context-audit.xml" })
@DirtiesContext(classMode=ClassMode.AFTER_CLASS)
public class ImportTest extends AbstractTestNGSpringContextTests {

	private static final String TEST_FILE_DIRECTORY = "src/test/resources/importer/";
	private static final File TEST_FOLDER_COMMON = new File("./src/test/resources/common");
	private static final File IMPORT_USERS_FILE = new File(TEST_FILE_DIRECTORY, "import-users.xml");
	private static final File IMPORT_USERS_OVERWRITE_FILE = new File(TEST_FILE_DIRECTORY, "import-users-overwrite.xml");
	private static final String USER_JACK_OID = "c0c010c0-d34d-b33f-f00d-111111111111";
	private static final String USER_WILL_OID = "c0c010c0-d34d-b33f-f00d-111111111112";
	private static final File IMPORT_CONNECTOR_FILE = new File(TEST_FOLDER_COMMON, "connector-dbtable.xml");
	private static final String CONNECOTR_LDAP_OID = "7d3ebd6f-6113-4833-8a6a-596b73a5e434";
	private static final String CONNECTOR_NAMESPACE = "http://midpoint.evolveum.com/xml/ns/public/connector/icf-1/bundle/org.forgerock.openicf.connectors.db.databasetable/org.identityconnectors.databasetable.DatabaseTableConnector";
	private static final File IMPORT_RESOURCE_FILE = new File(TEST_FOLDER_COMMON, "resource-derby.xml");
	private static final String RESOURCE_DERBY_OID = "ef2bc95b-76e0-59e2-86d6-999902d3abab";
	
	@Autowired(required = true)
	ModelService modelService;
	@Autowired(required = true)
	private RepositoryService repositoryService;
	@Autowired(required = true)
	private TaskManager taskManager;
	
	private static String guybrushOid;
	private static String hermanOid;

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
	public void test001ImportConnector() throws FileNotFoundException, ObjectNotFoundException, SchemaException {
		displayTestTile(this,"test001ImportConnector");
		// GIVEN
		Task task = taskManager.createTaskInstance();
		OperationResult result = new OperationResult(ImportTest.class.getName() + "test001ImportConnector");
		FileInputStream stream = new FileInputStream(IMPORT_CONNECTOR_FILE);
		
		// WHEN
		modelService.importObjectsFromStream(stream, getDefaultImportOptions(), task, result);

		// THEN
		result.computeStatus();
		display("Result after good import", result);
		assertSuccess("Import has failed (result)", result);

		// Check import with fixed OID
		ConnectorType connector = repositoryService.getObject(ConnectorType.class, CONNECOTR_LDAP_OID, null, result).asObjectable();
		assertNotNull(connector);
		assertEquals("ICF org.identityconnectors.databasetable.DatabaseTableConnector", connector.getName());
		assertEquals(CONNECTOR_NAMESPACE, connector.getNamespace());
		assertEquals("org.identityconnectors.databasetable.DatabaseTableConnector", connector.getConnectorType());
	}

	@Test
	public void test002ImportResource() throws FileNotFoundException, ObjectNotFoundException, SchemaException {
		displayTestTile(this,"test002ImportResource");
		// GIVEN
		Task task = taskManager.createTaskInstance();
		OperationResult result = new OperationResult(ImportTest.class.getName() + "test002ImportResource");
		FileInputStream stream = new FileInputStream(IMPORT_RESOURCE_FILE);

		// WHEN
		modelService.importObjectsFromStream(stream, getDefaultImportOptions(), task, result);

		// THEN
		result.computeStatus();
		display("Result after good import", result);
		assertSuccess("Import has failed (result)", result, 2);

		// Check import with fixed OID
		PrismObject<ResourceType> resource = repositoryService.getObject(ResourceType.class, RESOURCE_DERBY_OID, null, result);
		ResourceType resourceType = resource.asObjectable();
		assertNotNull(resourceType);
		display("Imported resource",resourceType);
		assertEquals("Embedded Test Derby", resourceType.getName());
		assertEquals("http://midpoint.evolveum.com/xml/ns/public/resource/instance/ef2bc95b-76e0-59e2-86d6-999902d3abab", resourceType.getNamespace());
		assertEquals(CONNECOTR_LDAP_OID,resourceType.getConnectorRef().getOid());
		
		// The password in the resource configuration should be encrypted after import
		PrismContainer<Containerable> configurationContainer = resource.findContainer(ResourceType.F_CONFIGURATION);
		PrismContainer<Containerable> configurationPropertiesContainer = 
			configurationContainer.findContainer(SchemaTestConstants.ICFC_CONFIGURATION_PROPERTIES);
		assertNotNull("No configurationProperties in resource", configurationPropertiesContainer);
		PrismProperty<Object> passwordProperty = configurationPropertiesContainer.findProperty(new QName(CONNECTOR_NAMESPACE, "password"));
		// The resource was pulled from the repository. Therefore it does not have the right schema here. We should proceed with caution
		// and inspect the DOM elements there
		assertNotNull("No password property in configuration properties", passwordProperty);
		PrismPropertyValue<Object> passwordPVal = passwordProperty.getValue();
		Object passwordRawElement = passwordPVal.getRawElement();
		if (!(passwordRawElement instanceof Element)) {
			AssertJUnit.fail("Expected password value of type "+Element.class+" but got "+passwordRawElement.getClass());
		}
		Element passwordDomElement = (Element)passwordRawElement;
		assertTrue("Password was not encrypted (clearValue)",passwordDomElement.getElementsByTagNameNS(SchemaConstants.NS_C, "clearValue").getLength()==0);
        assertTrue("Password was not encrypted (no EncryptedData)",passwordDomElement.getElementsByTagNameNS(DOMUtil.NS_XML_ENC,"EncryptedData").getLength()==1);		
	}
	
	@Test
	public void test003ImportUsers() throws FileNotFoundException, ObjectNotFoundException, SchemaException {
		displayTestTile(this,"test003ImportUsers");
		// GIVEN
		Task task = taskManager.createTaskInstance();
		OperationResult result = new OperationResult(ImportTest.class.getName() + "test003ImportUsers");
		FileInputStream stream = new FileInputStream(IMPORT_USERS_FILE);

		// WHEN
		modelService.importObjectsFromStream(stream, getDefaultImportOptions(), task, result);

		// THEN
		result.computeStatus();
		display("Result after good import", result);
		assertSuccess("Import has failed (result)", result);

		// Check import with fixed OID
		UserType jack = repositoryService.getObject(UserType.class, USER_JACK_OID, null, result).asObjectable();
		display("Jack",jack);
		assertNotNull(jack);
		assertEquals("Jack", jack.getGivenName());
		assertEquals("Sparrow", jack.getFamilyName());
		assertEquals("Cpt. Jack Sparrow", jack.getFullName());
		// Jack has a password. Check if it was encrypted
		ProtectedStringType protectedString = jack.getCredentials().getPassword().getProtectedString();
		assertNull("Arrgh! Pirate sectrets were revealed!",protectedString.getClearValue());
		assertNotNull("Er? The pirate sectrets were lost!",protectedString.getEncryptedData());

		// Check import with generated OID
		Document doc = DOMUtil.getDocument();
		Element filter = QueryUtil.createEqualFilter(doc, null, SchemaConstants.C_NAME, "guybrush");

		QueryType query = new QueryType();
		query.setFilter(filter);

		ResultList<PrismObject<UserType>> users = repositoryService.searchObjects(UserType.class, query, null, result);

		assertNotNull(users);
		assertEquals("Search retuned unexpected results", 1, users.size());
		UserType guybrush = users.get(0).asObjectable();
		assertNotNull(guybrush);
		guybrushOid = guybrush.getOid();
		assertNotNull(guybrushOid);
		assertEquals("Guybrush", guybrush.getGivenName());
		assertEquals("Threepwood", guybrush.getFamilyName());
		assertEquals("Guybrush Threepwood", guybrush.getFullName());

	}

	// Import the same thing again. Watch how it burns :-)
	@Test
	public void test004DuplicateImportUsers() throws FileNotFoundException, ObjectNotFoundException,
			SchemaException {
		displayTestTile(this,"test004DuplicateImportUsers");
		// GIVEN
		Task task = taskManager.createTaskInstance();
		OperationResult result = new OperationResult(ImportTest.class.getName() + "test004DuplicateImportUsers");
		FileInputStream stream = new FileInputStream(IMPORT_USERS_FILE);

		// WHEN
		modelService.importObjectsFromStream(stream, getDefaultImportOptions(), task, result);

		// THEN
		result.computeStatus();
		display("Result after dupicate import", result);
		assertFalse("Unexpected success", result.isSuccess());

		// All three users should fail. First two because of OID conflict,
		// guybrush because of name conflict
		// (nobody else could have such a stupid name)
		for (OperationResult subresult : result.getSubresults().get(0).getSubresults()) {
			assertFalse("Unexpected success in subresult", subresult.isSuccess());
		}

	}
	
	// Import the same thing again, this time with overwrite option. This should go well.
	@Test
	public void test005ImportUsersWithOverwrite() throws FileNotFoundException, ObjectNotFoundException,
			SchemaException {
		displayTestTile(this,"test005ImportUsersWithOverwrite");
		// GIVEN
		Task task = taskManager.createTaskInstance();
		OperationResult result = new OperationResult(ImportTest.class.getName() + "test005ImportUsersWithOverwrite");
		FileInputStream stream = new FileInputStream(IMPORT_USERS_OVERWRITE_FILE);
		ImportOptionsType options = getDefaultImportOptions();
		options.setOverwrite(true);

		// WHEN
		modelService.importObjectsFromStream(stream, options, task, result);

		// THEN
		result.computeStatus();
		display("Result after import with overwrite", result);
		assertSuccess("Import failed (result)", result,1);

		// list all users
		ResultList<PrismObject<UserType>> users = modelService.listObjects(UserType.class, null, result);
		// Three old users, one new
		assertEquals(4,users.size());
		
		for (PrismObject<UserType> user : users) {
			UserType userType = user.asObjectable();
			if (userType.getName().equals("jack")) {
				// OID and all the attributes should be the same
				assertEquals(USER_JACK_OID,userType.getOid());
				assertEquals("Jack", userType.getGivenName());
				assertEquals("Sparrow", userType.getFamilyName());
				assertEquals("Cpt. Jack Sparrow", userType.getFullName());
			}
			if (userType.getName().equals("will")) {
				// OID should be the same, and there should be an employee type
				assertEquals(USER_WILL_OID,userType.getOid());
				assertTrue("Wrong Will's employee type", userType.getEmployeeType().contains("legendary"));
			}			
			if (userType.getName().equals("guybrush")) {
				// OID may be different, there should be a locality attribute
				guybrushOid = userType.getOid();
				assertNotNull(guybrushOid);
				assertEquals("Guybrush is not in the Caribbean", "Deep in the Caribbean", userType.getLocality());
			}			
			if (userType.getName().equals("ht")) {
				// Herman should be here now
				hermanOid = userType.getOid();
				assertNotNull(hermanOid);
				assertEquals("Herman is confused", "Herman Toothrot", userType.getFullName());
				assertEquals("Herman is confused", "Herman", userType.getGivenName());
				assertEquals("Herman is confused", "Toothrot", userType.getFamilyName());
			}	
		}
	}

	
	// Import the same thing again, with overwrite and also while keeping OIDs
	@Test
	public void test006ImportUsersWithOverwriteKeepOid() throws FileNotFoundException, ObjectNotFoundException,
			SchemaException {
		displayTestTile(this,"test006ImportUsersWithOverwriteKeepOid");
		// GIVEN
		Task task = taskManager.createTaskInstance();
		OperationResult result = new OperationResult(ImportTest.class.getName() + "test005ImportUsersWithOverwrite");
		FileInputStream stream = new FileInputStream(IMPORT_USERS_OVERWRITE_FILE);
		ImportOptionsType options = getDefaultImportOptions();
		options.setOverwrite(true);
		options.setKeepOid(true);

		// WHEN
		modelService.importObjectsFromStream(stream, options, task, result);

		// THEN
		result.computeStatus();
		display("Result after import with overwrite", result);
		assertSuccess("Import failed (result)", result,1);

		// list all users
		ResultList<PrismObject<UserType>> users = modelService.listObjects(UserType.class, null, result);
		// Three old users, one new
		assertEquals(4,users.size());
		
		for (PrismObject<UserType> user : users) {
			UserType userType = user.asObjectable();
			if (userType.getName().equals("jack")) {
				// OID and all the attributes should be the same
				assertEquals(USER_JACK_OID,userType.getOid());
				assertEquals("Jack", userType.getGivenName());
				assertEquals("Sparrow", userType.getFamilyName());
				assertEquals("Cpt. Jack Sparrow", userType.getFullName());
			}
			if (userType.getName().equals("will")) {
				// OID should be the same, and there should be an employee type
				assertEquals(USER_WILL_OID,userType.getOid());
				assertTrue("Wrong Will's employee type", userType.getEmployeeType().contains("legendary"));
			}			
			if (userType.getName().equals("guybrush")) {
				// OID should be the same, there should be a locality attribute
				assertEquals("Guybrush's OID went leeway", guybrushOid, userType.getOid());
				assertEquals("Guybrush is not in the Caribbean", "Deep in the Caribbean", userType.getLocality());
			}
			if (userType.getName().equals("ht")) {
				// Herman should still be here
				assertEquals("Herman's OID went leeway", hermanOid, userType.getOid());
				assertEquals("Herman is confused", "Herman Toothrot", userType.getFullName());
				assertEquals("Herman is confused", "Herman", userType.getGivenName());
				assertEquals("Herman is confused", "Toothrot", userType.getFamilyName());
			}	
		}
	}

}
