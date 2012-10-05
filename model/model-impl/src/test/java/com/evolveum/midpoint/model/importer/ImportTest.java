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

import com.evolveum.midpoint.common.QueryUtil;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.EqualsFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.schema.util.SchemaTestConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.task.quartzimpl.handlers.NoOpTaskHandler;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.ImportOptionsType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.*;
import com.evolveum.prism.xml.ns._public.query_2.QueryType;
import com.evolveum.prism.xml.ns._public.types_2.PolyStringType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.AssertJUnit;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.namespace.QName;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import static com.evolveum.midpoint.schema.util.MiscSchemaUtil.getDefaultImportOptions;
import static com.evolveum.midpoint.test.IntegrationTestTools.*;
import static org.testng.AssertJUnit.*;

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
	
	private static final File IMPORT_TASK_FILE = new File(TEST_FILE_DIRECTORY, "import-task.xml");
	private static final String TASK1_OID = "00000000-0000-0000-0000-123450000001";
	private static final String TASK1_OWNER_OID = "c0c010c0-d34d-b33f-f00d-111111111111";
	
	@Autowired(required = true)
	ModelService modelService;
	@Autowired(required = true)
	private RepositoryService repositoryService;
	@Autowired(required = true)
	private TaskManager taskManager;
	
	private static String guybrushOid;
	private static String hermanOid;
	
	@BeforeSuite
	public void setup() throws SchemaException, SAXException, IOException {
		DebugUtil.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
		PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
	}

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
		ConnectorType connector = repositoryService.getObject(ConnectorType.class, CONNECOTR_LDAP_OID, result).asObjectable();
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
		PrismObject<ResourceType> resource = repositoryService.getObject(ResourceType.class, RESOURCE_DERBY_OID, result);
		ResourceType resourceType = resource.asObjectable();
		assertNotNull(resourceType);
		display("Imported resource",resourceType);
		assertEquals("Embedded Test Derby", resourceType.getName());
		assertEquals("http://midpoint.evolveum.com/xml/ns/public/resource/instance/ef2bc95b-76e0-59e2-86d6-999902d3abab", 
				ResourceTypeUtil.getResourceNamespace(resourceType));
		assertEquals(CONNECOTR_LDAP_OID,resourceType.getConnectorRef().getOid());
		
		// The password in the resource configuration should be encrypted after import
		PrismContainer<Containerable> configurationContainer = resource.findContainer(ResourceType.F_CONNECTOR_CONFIGURATION);
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
		UserType jack = repositoryService.getObject(UserType.class, USER_JACK_OID, result).asObjectable();
		display("Jack",jack);
		assertNotNull(jack);
		PrismAsserts.assertEqualsPolyString("wrong givenName", "Jack", jack.getGivenName());
		PrismAsserts.assertEqualsPolyString("wrong familyName", "Sparrow", jack.getFamilyName());
		PrismAsserts.assertEqualsPolyString("wrong fullName", "Cpt. Jack Sparrow", jack.getFullName());
		PrismAsserts.assertEquals("wrong costCenter", "<No 'cost' & no \"center\">", jack.getCostCenter());
		// Jack has a password. Check if it was encrypted
		ProtectedStringType protectedString = jack.getCredentials().getPassword().getValue();
		assertNull("Arrgh! Pirate sectrets were revealed!",protectedString.getClearValue());
		assertNotNull("Er? The pirate sectrets were lost!",protectedString.getEncryptedData());

		// Check import with generated OID
//		Document doc = DOMUtil.getDocument();
//		Element filter = QueryUtil.createEqualFilter(doc, null, SchemaConstants.C_NAME, "guybrush");
//
//		QueryType query = new QueryType();
//		query.setFilter(filter);

		EqualsFilter equal = EqualsFilter.createEqual(UserType.class, PrismTestUtil.getPrismContext(), UserType.F_NAME, "guybrush");
		ObjectQuery query = ObjectQuery.createObjectQuery(equal);
		
		List<PrismObject<UserType>> users = repositoryService.searchObjects(UserType.class, query, null, result);

		assertNotNull(users);
		assertEquals("Search retuned unexpected results", 1, users.size());
		UserType guybrush = users.get(0).asObjectable();
		assertNotNull(guybrush);
		guybrushOid = guybrush.getOid();
		assertNotNull(guybrushOid);
		PrismAsserts.assertEqualsPolyString("wrong givenName", "Guybrush", guybrush.getGivenName());
		PrismAsserts.assertEqualsPolyString("wrong familyName", "Threepwood", guybrush.getFamilyName());
		PrismAsserts.assertEqualsPolyString("wrong fullName", "Guybrush Threepwood", guybrush.getFullName());
		
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
			SchemaException, SecurityViolationException {
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
		assertSuccess("Import failed (result)", result);

		// list all users
		List<PrismObject<UserType>> users = modelService.searchObjects(UserType.class, new ObjectQuery(), null, task, result);
		// Three old users, one new
		assertEquals(4,users.size());
		
		for (PrismObject<UserType> user : users) {
			UserType userType = user.asObjectable();
			if (userType.getName().equals("jack")) {
				// OID and all the attributes should be the same
				assertEquals(USER_JACK_OID,userType.getOid());
				PrismAsserts.assertEqualsPolyString("wrong givenName", "Jack", userType.getGivenName());
				PrismAsserts.assertEqualsPolyString("wrong familyName", "Sparrow", userType.getFamilyName());
				PrismAsserts.assertEqualsPolyString("wrong fullName", "Cpt. Jack Sparrow", userType.getFullName());
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
				PrismAsserts.assertEqualsPolyString("Guybrush is not in the Caribbean", "Deep in the Caribbean", userType.getLocality());
			}			
			if (userType.getName().equals("ht")) {
				// Herman should be here now
				hermanOid = userType.getOid();
				assertNotNull(hermanOid);
				PrismAsserts.assertEqualsPolyString("Herman is confused", "Herman Toothrot", userType.getFullName());
				PrismAsserts.assertEqualsPolyString("Herman is confused", "Herman", userType.getGivenName());
				PrismAsserts.assertEqualsPolyString("Herman is confused", "Toothrot", userType.getFamilyName());
			}	
		}
	}

	
	// Import the same thing again, with overwrite and also while keeping OIDs
	@Test
	public void test006ImportUsersWithOverwriteKeepOid() throws FileNotFoundException, ObjectNotFoundException,
			SchemaException, SecurityViolationException {
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
		List<PrismObject<UserType>> users = modelService.searchObjects(UserType.class, new ObjectQuery(), null, task, result);
		// Three old users, one new
		assertEquals(4,users.size());
		
		for (PrismObject<UserType> user : users) {
			UserType userType = user.asObjectable();
			if (userType.getName().equals("jack")) {
				// OID and all the attributes should be the same
				assertEquals(USER_JACK_OID,userType.getOid());
				PrismAsserts.assertEqualsPolyString("wrong givenName", "Jack", userType.getGivenName());
				PrismAsserts.assertEqualsPolyString("wrong familyName", "Sparrow", userType.getFamilyName());
				PrismAsserts.assertEqualsPolyString("wrong fullName", "Cpt. Jack Sparrow", userType.getFullName());
			}
			if (userType.getName().equals("will")) {
				// OID should be the same, and there should be an employee type
				assertEquals(USER_WILL_OID,userType.getOid());
				assertTrue("Wrong Will's employee type", userType.getEmployeeType().contains("legendary"));
			}			
			if (userType.getName().equals("guybrush")) {
				// OID should be the same, there should be a locality attribute
				assertEquals("Guybrush's OID went leeway", guybrushOid, userType.getOid());
				PrismAsserts.assertEqualsPolyString("Guybrush is not in the Caribbean", "Deep in the Caribbean", userType.getLocality());
			}
			if (userType.getName().equals("ht")) {
				// Herman should still be here
				assertEquals("Herman's OID went leeway", hermanOid, userType.getOid());
				PrismAsserts.assertEqualsPolyString("Herman is confused", "Herman Toothrot", userType.getFullName());
				PrismAsserts.assertEqualsPolyString("Herman is confused", "Herman", userType.getGivenName());
				PrismAsserts.assertEqualsPolyString("Herman is confused", "Toothrot", userType.getFamilyName());
			}	
		}
	}

	@Test
	public void test007ImportTask() throws FileNotFoundException, ObjectNotFoundException, SchemaException {
		displayTestTile(this, "test007ImportTask");
		// GIVEN
		Task task = taskManager.createTaskInstance();
		OperationResult result = new OperationResult(ImportTest.class.getName() + "test007ImportTask");
		FileInputStream stream = new FileInputStream(IMPORT_TASK_FILE);
		
		// well, let's check whether task owner really exists
		PrismObject<UserType> ownerPrism = repositoryService.getObject(UserType.class, TASK1_OWNER_OID, result);
		assertEquals("Task owner does not exist or has an unexpected OID", TASK1_OWNER_OID, ownerPrism.getOid());
		
		// WHEN
		modelService.importObjectsFromStream(stream, getDefaultImportOptions(), task, result);

		// THEN
		result.computeStatus();
		display("Result after good import", result);
		assertSuccess("Import has failed (result)", result);

		// Check import
		PrismObject<TaskType> task1AsPrism = repositoryService.getObject(TaskType.class, TASK1_OID, result);
		TaskType task1AsType = task1AsPrism.asObjectable();
		assertNotNull(task1AsType);
		assertEquals("Task name not imported correctly", "Task1: basic single-run task (takes 3x60 sec)", task1AsType.getName());
		
		Task task1 = taskManager.createTaskInstance(task1AsPrism, result);
        PrismProperty<Integer> delayProp = (PrismProperty<Integer>) task1.getExtension(new QName(NoOpTaskHandler.EXT_SCHEMA_URI, "delay"));
        assertEquals("xsi:type'd property has incorrect type", Integer.class, delayProp.getValues().get(0).getValue().getClass());
        assertEquals("xsi:type'd property not imported correctly", Integer.valueOf(60000), delayProp.getValues().get(0).getValue());
	}

}
