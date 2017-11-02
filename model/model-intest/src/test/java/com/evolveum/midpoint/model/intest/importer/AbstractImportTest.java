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
package com.evolveum.midpoint.model.intest.importer;

import com.evolveum.icf.dummy.resource.DummyResource;
import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.model.intest.AbstractConfiguredModelIntegrationTest;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xnode.MapXNode;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.schema.util.SchemaTestConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.MidPointAsserts;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ImportOptionsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import javax.xml.datatype.XMLGregorianCalendar;
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
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode=ClassMode.AFTER_CLASS)
public abstract class AbstractImportTest extends AbstractConfiguredModelIntegrationTest {

	private static final String TEST_FILE_DIRECTORY = "src/test/resources/importer/";
	private static final String TEST_FOLDER_COMMON = "src/test/resources/common/";

	private static final String IMPORT_USERS_FILE_NAME = "import-users";
	private static final String IMPORT_USERS_OVERWRITE_FILE_NAME = "import-users-overwrite";

	private static final String USER_JACK_OID = "c0c010c0-d34d-b33f-f00d-111111111111";
	private static final String USER_WILL_OID = "c0c010c0-d34d-b33f-f00d-111111111112";
	private static final String CONNECTOR_DBTABLE_FILE_NAME = "connector-dbtable";
	private static final String CONNECOTR_DBTABLE_OID = "7d3ebd6f-6113-4833-8a6a-596b73a5e434";
	private static final String CONNECTOR_NAMESPACE = "http://midpoint.evolveum.com/xml/ns/public/connector/icf-1/bundle/org.forgerock.openicf.connectors.db.databasetable/org.identityconnectors.databasetable.DatabaseTableConnector";

	private static final String RESOURCE_DERBY_FILE_NAME = "resource-derby";
	private static final String RESOURCE_DERBY_OID = "ef2bc95b-76e0-59e2-86d6-9119011311ab";
	private static final String RESOURCE_DERBY_NAMESPACE = "http://midpoint.evolveum.com/xml/ns/public/resource/instance/ef2bc95b-76e0-59e2-86d6-9119011311ab";

	private static final String RESOURCE_DUMMY_FILE_NAME = "resource-dummy";
	private static final String RESOURCE_DUMMY_RUNTIME_FILE_NAME = "resource-dummy-runtime-resolution";
	private static final String RESOURCE_DUMMY_RUNTIME_OID = "78fc521e-69f0-11e6-9ec5-130eb0c6fb6d";

	private static final String IMPORT_TASK_FILE_NAME = "import-task";
	private static final String TASK1_OID = "00000000-0000-0000-0000-123450000001";
	private static final String TASK1_OWNER_OID = "c0c010c0-d34d-b33f-f00d-111111111111";

	private static final String RESOURCE_DUMMY_CHANGED_FILE_NAME = "resource-dummy-changed";

	protected static final String USER_HERMAN_FILE_NAME = "user-herman";
	private static final String IMPORT_REF_FILE_NAME = "import-ref";
	private static final String BAD_IMPORT_FILE_NAME = "import-bad";

	private DummyResource dummyResource;
	private DummyResourceContoller dummyResourceCtl;

	private PrismObject<ConnectorType> dummyConnector;
	private PrismObject<ResourceType> importedResource;
	private PrismObject<ResourceType> importedRepoResource;

	private static String guybrushOid;
	private static String hermanOid;

	abstract String getSuffix();

	abstract String getLanguage();

	File getFile(String name, boolean common) {
		return new File(common ? TEST_FOLDER_COMMON : TEST_FILE_DIRECTORY, name + "." + getSuffix());
	}

	@Autowired
	private Clock clock;

	@BeforeSuite
	public void setup() throws SchemaException, SAXException, IOException {
		PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
		PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
	}

	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		LOGGER.trace("initSystem");
		super.initSystem(initTask, initResult);

		repoAddObjectFromFile(SECURITY_POLICY_FILE, initResult);

		// Just initialize the resource, do NOT import resource definition
		dummyResourceCtl = DummyResourceContoller.create(null);
		dummyResourceCtl.extendSchemaPirate();
		dummyResource = dummyResourceCtl.getDummyResource();

		dummyConnector = findConnectorByTypeAndVersion(CONNECTOR_DUMMY_TYPE, CONNECTOR_DUMMY_VERSION, initResult);
	}

	/**
	 * Test integrity of the test setup.
	 *
	 */
	@Test
	public void test000Integrity() {
		TestUtil.displayTestTitle(this,"test000Integrity");
		assertNotNull(modelService);
		assertNotNull(repositoryService);
	}

	@Test
	public void test001ImportConnector() throws FileNotFoundException, ObjectNotFoundException, SchemaException {
		TestUtil.displayTestTitle(this,"test001ImportConnector");
		// GIVEN
		Task task = taskManager.createTaskInstance();
		OperationResult result = new OperationResult(AbstractImportTest.class.getName() + "test001ImportConnector");
		FileInputStream stream = new FileInputStream(getFile(CONNECTOR_DBTABLE_FILE_NAME, true));

		dummyAuditService.clear();
		XMLGregorianCalendar startTime = clock.currentTimeXMLGregorianCalendar();

		// WHEN
		modelService.importObjectsFromStream(stream, getLanguage(), getDefaultImportOptions(), task, result);

		// THEN
		XMLGregorianCalendar endTime = clock.currentTimeXMLGregorianCalendar();
		result.computeStatus();
		display("Result after good import", result);
		TestUtil.assertSuccess("Import has failed (result)", result);

		// Check import with fixed OID
		ConnectorType connector = repositoryService.getObject(ConnectorType.class, CONNECOTR_DBTABLE_OID, null, result).asObjectable();
		assertNotNull(connector);
		PrismAsserts.assertEqualsPolyString("Wrong connector name.", "ICF org.identityconnectors.databasetable.DatabaseTableConnector", connector.getName());
//		assertEquals("ICF org.identityconnectors.databasetable.DatabaseTableConnector", connector.getName());
		assertEquals(CONNECTOR_NAMESPACE, connector.getNamespace());
		assertEquals("org.identityconnectors.databasetable.DatabaseTableConnector", connector.getConnectorType());

		assertMetadata(connector, startTime, endTime);

		// Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(1);
        dummyAuditService.assertHasDelta(ChangeType.ADD, ConnectorType.class);
        dummyAuditService.assertExecutionSuccess();
	}

	@Test
	public void test003ImportUsers() throws Exception {
		TestUtil.displayTestTitle(this,"test003ImportUsers");
		// GIVEN
		Task task = taskManager.createTaskInstance();
		OperationResult result = new OperationResult(AbstractImportTest.class.getName() + "test003ImportUsers");
		FileInputStream stream = new FileInputStream(getFile(IMPORT_USERS_FILE_NAME, false));

		dummyAuditService.clear();
		XMLGregorianCalendar startTime = clock.currentTimeXMLGregorianCalendar();

		// WHEN
		modelService.importObjectsFromStream(stream, getLanguage(), getDefaultImportOptions(), task, result);

		// THEN
		XMLGregorianCalendar endTime = clock.currentTimeXMLGregorianCalendar();
		result.computeStatus();
		display("Result after good import", result);
		TestUtil.assertSuccess("Import has failed (result)", result);

		// Check import with fixed OID
		UserType jack = repositoryService.getObject(UserType.class, USER_JACK_OID, null, result).asObjectable();
		display("Jack",jack);
		assertNotNull(jack);
		PrismAsserts.assertEqualsPolyString("wrong givenName", "Jack", jack.getGivenName());
		PrismAsserts.assertEqualsPolyString("wrong familyName", "Sparrow", jack.getFamilyName());
		PrismAsserts.assertEqualsPolyString("wrong fullName", "Cpt. Jack Sparrow", jack.getFullName());
		PrismAsserts.assertEquals("wrong costCenter", "<No 'cost' & no \"center\">", jack.getCostCenter());
		// Jack has a password. Check if it was encrypted
		ProtectedStringType protectedString = jack.getCredentials().getPassword().getValue();
		assertNull("Arrgh! Pirate sectrets were revealed!",protectedString.getClearValue());
		assertNotNull("Er? The pirate sectrets were lost!",protectedString.getEncryptedDataType());

		assertMetadata(jack, startTime, endTime);

		// Check import with generated OID
//		EqualsFilter equal = EqualsFilter.createEqual(UserType.class, PrismTestUtil.getPrismContext(), UserType.F_NAME, "guybrush");
//		ObjectQuery query = ObjectQuery.createObjectQuery(equal);
		ObjectQuery query = ObjectQueryUtil.createNameQuery("guybrush", PrismTestUtil.getPrismContext());

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
		assertMetadata(guybrush, startTime, endTime);

		assertUsers(4);

		// Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(6);
	}

	// Import the same thing again. Watch how it burns :-)
	@Test
	public void test004DuplicateImportUsers() throws Exception {
		TestUtil.displayTestTitle(this,"test004DuplicateImportUsers");
		// GIVEN
		Task task = taskManager.createTaskInstance();
		OperationResult result = new OperationResult(AbstractImportTest.class.getName() + "test004DuplicateImportUsers");
		FileInputStream stream = new FileInputStream(getFile(IMPORT_USERS_FILE_NAME, false));

		dummyAuditService.clear();

		// WHEN
		modelService.importObjectsFromStream(stream, getLanguage(), getDefaultImportOptions(), task, result);

		// THEN
		result.computeStatus();
		display("Result after duplicate import", result);
		assertFalse("Unexpected success", result.isSuccess());

		// All three users should fail. First two because of OID conflict,
		// guybrush because of name conflict
		// (nobody else could have such a stupid name)
		for (OperationResult subresult : result.getSubresults().get(0).getSubresults()) {
			assertFalse("Unexpected success in subresult", subresult.isSuccess());
		}

		assertUsers(4);

		// Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(6);		// 3 requests + 3 failed executions
	}

	// Import the same thing again, this time with overwrite option. This should go well.
	@Test
	public void test005ImportUsersWithOverwrite() throws Exception {
		TestUtil.displayTestTitle(this,"test005ImportUsersWithOverwrite");
		// GIVEN
		Task task = taskManager.createTaskInstance();
		OperationResult result = new OperationResult(AbstractImportTest.class.getName() + "test005ImportUsersWithOverwrite");
		FileInputStream stream = new FileInputStream(getFile(IMPORT_USERS_OVERWRITE_FILE_NAME, false));
		ImportOptionsType options = getDefaultImportOptions();
		options.setOverwrite(true);

		dummyAuditService.clear();

		// WHEN
		modelService.importObjectsFromStream(stream, getLanguage(), options, task, result);

		// THEN
		result.computeStatus();
		display("Result after import with overwrite", result);
		TestUtil.assertSuccess("Import failed (result)", result, 2);

		// list all users
		List<PrismObject<UserType>> users = modelService.searchObjects(UserType.class, new ObjectQuery(), null, task, result);
		// Three old users, one new
		assertEquals(5,users.size());

		for (PrismObject<UserType> user : users) {
			UserType userType = user.asObjectable();
			if (userType.getName().toString().equals("jack")) {
				// OID and all the attributes should be the same
				assertEquals(USER_JACK_OID,userType.getOid());
				PrismAsserts.assertEqualsPolyString("wrong givenName", "Jack", userType.getGivenName());
				PrismAsserts.assertEqualsPolyString("wrong familyName", "Sparrow", userType.getFamilyName());
				PrismAsserts.assertEqualsPolyString("wrong fullName", "Cpt. Jack Sparrow", userType.getFullName());
			}
			if (userType.getName().toString().equals("will")) {
				// OID should be the same, and there should be an employee type
				assertEquals(USER_WILL_OID,userType.getOid());
				assertTrue("Wrong Will's employee type", userType.getEmployeeType().contains("legendary"));
			}
			if (userType.getName().toString().equals("guybrush")) {
				// OID may be different, there should be a locality attribute
				guybrushOid = userType.getOid();
				assertNotNull(guybrushOid);
				PrismAsserts.assertEqualsPolyString("Guybrush is not in the Caribbean", "Deep in the Caribbean", userType.getLocality());
			}
			if (userType.getName().toString().equals("ht")) {
				// Herman should be here now
				hermanOid = userType.getOid();
				assertNotNull(hermanOid);
				PrismAsserts.assertEqualsPolyString("Herman is confused", "Herman Toothrot", userType.getFullName());
				PrismAsserts.assertEqualsPolyString("Herman is confused", "Herman", userType.getGivenName());
				PrismAsserts.assertEqualsPolyString("Herman is confused", "Toothrot", userType.getFamilyName());
			}
		}

		assertUsers(5);

		// Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(8);		// 1 failed, 7 succeeded
	}


	// Import the same thing again, with overwrite and also while keeping OIDs
	@Test
	public void test006ImportUsersWithOverwriteKeepOid() throws Exception {
		TestUtil.displayTestTitle(this,"test006ImportUsersWithOverwriteKeepOid");
		// GIVEN
		Task task = taskManager.createTaskInstance();
		OperationResult result = new OperationResult(AbstractImportTest.class.getName() + "test005ImportUsersWithOverwrite");
		FileInputStream stream = new FileInputStream(getFile(IMPORT_USERS_OVERWRITE_FILE_NAME, false));
		ImportOptionsType options = getDefaultImportOptions();
		options.setOverwrite(true);
		options.setKeepOid(true);

		dummyAuditService.clear();

		// WHEN
		modelService.importObjectsFromStream(stream, getLanguage(), options, task, result);

		// THEN
		result.computeStatus();
		display("Result after import with overwrite", result);
		TestUtil.assertSuccess("Import failed (result)", result,1);

		// list all users
		List<PrismObject<UserType>> users = modelService.searchObjects(UserType.class, new ObjectQuery(), null, task, result);
		// Three old users, one new
		assertEquals(5,users.size());

		for (PrismObject<UserType> user : users) {
			UserType userType = user.asObjectable();
			if (userType.getName().toString().equals("jack")) {
				// OID and all the attributes should be the same
				assertEquals(USER_JACK_OID,userType.getOid());
				PrismAsserts.assertEqualsPolyString("wrong givenName", "Jack", userType.getGivenName());
				PrismAsserts.assertEqualsPolyString("wrong familyName", "Sparrow", userType.getFamilyName());
				PrismAsserts.assertEqualsPolyString("wrong fullName", "Cpt. Jack Sparrow", userType.getFullName());
			}
			if (userType.getName().toString().equals("will")) {
				// OID should be the same, and there should be an employee type
				assertEquals(USER_WILL_OID,userType.getOid());
				assertTrue("Wrong Will's employee type", userType.getEmployeeType().contains("legendary"));
			}
			if (userType.getName().toString().equals("guybrush")) {
				// OID should be the same, there should be a locality attribute
				assertEquals("Guybrush's OID went leeway", guybrushOid, userType.getOid());
				PrismAsserts.assertEqualsPolyString("Guybrush is not in the Caribbean", "Deep in the Caribbean", userType.getLocality());
			}
			if (userType.getName().toString().equals("ht")) {
				// Herman should still be here
				assertEquals("Herman's OID went leeway", hermanOid, userType.getOid());
				PrismAsserts.assertEqualsPolyString("Herman is confused", "Herman Toothrot", userType.getFullName());
				PrismAsserts.assertEqualsPolyString("Herman is confused", "Herman", userType.getGivenName());
				PrismAsserts.assertEqualsPolyString("Herman is confused", "Toothrot", userType.getFamilyName());
			}
		}

		assertUsers(5);

		// Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(6);
	}

	@Test
	public void test020ImportTask() throws Exception {
		final String TEST_NAME = "test020ImportTask";
		TestUtil.displayTestTitle(this, TEST_NAME);
		// GIVEN
		Task task = taskManager.createTaskInstance(AbstractImportTest.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();
		FileInputStream stream = new FileInputStream(getFile(IMPORT_TASK_FILE_NAME, false));

		// well, let's check whether task owner really exists
		PrismObject<UserType> ownerPrism = repositoryService.getObject(UserType.class, TASK1_OWNER_OID, null, result);
		assertEquals("Task owner does not exist or has an unexpected OID", TASK1_OWNER_OID, ownerPrism.getOid());

		dummyAuditService.clear();

		// WHEN
		modelService.importObjectsFromStream(stream, getLanguage(), getDefaultImportOptions(), task, result);

		// THEN
		result.computeStatus();
		display("Result after good import", result);
		TestUtil.assertSuccess("Import has failed (result)", result);

		// Check import
		PrismObject<TaskType> task1AsPrism = repositoryService.getObject(TaskType.class, TASK1_OID, null, result);
		TaskType task1AsType = task1AsPrism.asObjectable();
		assertNotNull(task1AsType);
		PrismAsserts.assertEqualsPolyString("Task name not imported correctly", "Task1: basic single-run task (takes 180x1 sec)", task1AsType.getName());
//		assertEquals("Task name not imported correctly", "Task1: basic single-run task (takes 180x1 sec)", task1AsType.getName());

		Task task1 = taskManager.createTaskInstance(task1AsPrism, result);
        PrismProperty<Integer> delayProp = task1.getExtensionProperty(SchemaConstants.NOOP_DELAY_QNAME);
        assertEquals("xsi:type'd property has incorrect type", Integer.class, delayProp.getValues().get(0).getValue().getClass());
        assertEquals("xsi:type'd property not imported correctly", Integer.valueOf(1000), delayProp.getValues().get(0).getValue());

     // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(1);
        dummyAuditService.assertHasDelta(ChangeType.ADD, TaskType.class);
        dummyAuditService.assertExecutionSuccess();
	}

	@Test
	public void test030ImportResource() throws Exception {
		final String TEST_NAME = "test030ImportResource";
		TestUtil.displayTestTitle(this,TEST_NAME);
		// GIVEN
		Task task = taskManager.createTaskInstance(AbstractImportTest.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();
		FileInputStream stream = new FileInputStream(getFile(RESOURCE_DUMMY_FILE_NAME, true));

		IntegrationTestTools.assertNoRepoCache();
		dummyAuditService.clear();

		// WHEN
		modelService.importObjectsFromStream(stream, getLanguage(), getDefaultImportOptions(), task, result);

		// THEN
		result.computeStatus();
		display("Result after import", result);
		TestUtil.assertSuccess("Import of "+RESOURCE_DUMMY_FILE_NAME+" has failed (result)", result, 2);

		IntegrationTestTools.assertNoRepoCache();

		importedRepoResource = repositoryService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null, result);
		display("Imported resource (repo)", importedRepoResource);
		IntegrationTestTools.assertNoRepoCache();
		assertDummyResource(importedRepoResource, true);

		importedResource = modelService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null, task, result);
		display("Imported resource (model)", importedResource);
		IntegrationTestTools.assertNoRepoCache();
		assertDummyResource(importedResource, false);

		ResourceType importedResourceType = importedResource.asObjectable();
		assertNotNull("No synchronization", importedResourceType.getSynchronization());

		// Read it from repo again. The read from model triggers schema fetch which increases version
		importedRepoResource = repositoryService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null, result);
		display("Imported resource (repo2)", importedRepoResource);
		IntegrationTestTools.assertNoRepoCache();
		assertDummyResource(importedRepoResource, true);

		// Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(1);
        dummyAuditService.assertHasDelta(ChangeType.ADD, ResourceType.class);
        dummyAuditService.assertExecutionSuccess();
	}

	@Test
	public void test031ReimportResource() throws Exception {
		final String TEST_NAME = "test031ReimportResource";
		TestUtil.displayTestTitle(this,TEST_NAME);
		// GIVEN
		Task task = taskManager.createTaskInstance(AbstractImportTest.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();
		FileInputStream stream = new FileInputStream(getFile(RESOURCE_DUMMY_CHANGED_FILE_NAME, false));

		ImportOptionsType options = getDefaultImportOptions();
		options.setOverwrite(true);

		IntegrationTestTools.assertNoRepoCache();
		dummyAuditService.clear();

		// WHEN
		modelService.importObjectsFromStream(stream, getLanguage(), options, task, result);

		// THEN
		result.computeStatus();
		display("Result after import", result);
		TestUtil.assertSuccess("Import of "+RESOURCE_DUMMY_CHANGED_FILE_NAME+" has failed (result)", result, 2);

		IntegrationTestTools.assertNoRepoCache();

		PrismObject<ResourceType> repoResource = repositoryService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null, result);
		display("Reimported resource (repo)", repoResource);
		assertDummyResource(repoResource, true);

		IntegrationTestTools.assertNoRepoCache();

		MidPointAsserts.assertVersionIncrease(importedRepoResource, repoResource);

		PrismObject<ResourceType> resource = modelService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null, task, result);
		display("Reimported resource (model)", resource);

		IntegrationTestTools.assertNoRepoCache();

		assertDummyResource(resource, false);

		MidPointAsserts.assertVersionIncrease(importedResource, resource);

		ResourceType resourceType = resource.asObjectable();
		assertNull("Synchronization not gone", resourceType.getSynchronization());

		// Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(1);
        dummyAuditService.assertHasDelta(ChangeType.ADD, ResourceType.class);
        dummyAuditService.assertExecutionSuccess();
	}

	@Test
	public void test032ImportResourceOidAndFilter() throws Exception {
		final String TEST_NAME = "test032ImportResourceOidAndFilter";
		TestUtil.displayTestTitle(this,TEST_NAME);
		// GIVEN
		Task task = taskManager.createTaskInstance(AbstractImportTest.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();
		FileInputStream stream = new FileInputStream(getFile(RESOURCE_DERBY_FILE_NAME, false));

		IntegrationTestTools.assertNoRepoCache();
		dummyAuditService.clear();

		// WHEN
		modelService.importObjectsFromStream(stream, getLanguage(), getDefaultImportOptions(), task, result);

		// THEN
		result.computeStatus();
		display("Result after import", result);
		TestUtil.assertSuccess("Import of "+RESOURCE_DERBY_FILE_NAME+" has failed (result)", result, 2);

		IntegrationTestTools.assertNoRepoCache();

		importedRepoResource = repositoryService.getObject(ResourceType.class, RESOURCE_DERBY_OID, null, result);
		display("Imported resource (repo)", importedRepoResource);
		IntegrationTestTools.assertNoRepoCache();
		assertResource(importedRepoResource, "Embedded Test Derby: Import test", RESOURCE_DERBY_NAMESPACE,
				CONNECOTR_DBTABLE_OID, true);

		importedResource = modelService.getObject(ResourceType.class, RESOURCE_DERBY_OID, null, task, result);
		display("Imported resource (model)", importedResource);
		IntegrationTestTools.assertNoRepoCache();
		assertResource(importedResource, "Embedded Test Derby: Import test", RESOURCE_DERBY_NAMESPACE,
				CONNECOTR_DBTABLE_OID, false);

		// Read it from repo again. The read from model triggers schema fetch which increases version
		importedRepoResource = repositoryService.getObject(ResourceType.class, RESOURCE_DERBY_OID, null, result);
		display("Imported resource (repo2)", importedRepoResource);
		IntegrationTestTools.assertNoRepoCache();
		assertResource(importedRepoResource, "Embedded Test Derby: Import test", RESOURCE_DERBY_NAMESPACE,
				CONNECOTR_DBTABLE_OID, true);

		// Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(1);
        dummyAuditService.assertHasDelta(ChangeType.ADD, ResourceType.class);
        dummyAuditService.assertExecutionSuccess();
	}

	/**
	 * MID-3365
	 */
	@Test
	public void test033ImportResourceDummyRuntime() throws Exception {
		final String TEST_NAME = "test033ImportResourceDummyRuntime";
		TestUtil.displayTestTitle(this,TEST_NAME);
		// GIVEN
		Task task = taskManager.createTaskInstance(AbstractImportTest.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();
		FileInputStream stream = new FileInputStream(getFile(RESOURCE_DUMMY_RUNTIME_FILE_NAME, false));

		IntegrationTestTools.assertNoRepoCache();
		dummyAuditService.clear();

		// WHEN
		modelService.importObjectsFromStream(stream, getLanguage(), getDefaultImportOptions(), task, result);

		// THEN
		result.computeStatus();
		display("Result after import", result);
		TestUtil.assertSuccess("Import of "+RESOURCE_DUMMY_RUNTIME_FILE_NAME+" has failed (result)", result, 2);

		IntegrationTestTools.assertNoRepoCache();

		importedRepoResource = repositoryService.getObject(ResourceType.class, RESOURCE_DUMMY_RUNTIME_OID, null, result);
		display("Imported resource (repo)", importedRepoResource);
		IntegrationTestTools.assertNoRepoCache();
		assertResource(importedRepoResource, "Dummy Resource (runtime)", MidPointConstants.NS_RI, null, true);

		importedResource = modelService.getObject(ResourceType.class, RESOURCE_DUMMY_RUNTIME_OID, null, task, result);
		display("Imported resource (model)", importedResource);
		IntegrationTestTools.assertNoRepoCache();
		assertResource(importedRepoResource, "Dummy Resource (runtime)", MidPointConstants.NS_RI, null,false);

		// Read it from repo again. The read from model triggers schema fetch which increases version
		importedRepoResource = repositoryService.getObject(ResourceType.class, RESOURCE_DUMMY_RUNTIME_OID, null, result);
		display("Imported resource (repo2)", importedRepoResource);
		IntegrationTestTools.assertNoRepoCache();
		assertResource(importedRepoResource, "Dummy Resource (runtime)", MidPointConstants.NS_RI, null, true);
	}

	@Test
	public void test040ImportUserHermanNoEncryption() throws Exception {
		final String TEST_NAME = "test040ImportUserHermanNoEncryption";
		TestUtil.displayTestTitle(this,TEST_NAME);
		// GIVEN

		InternalsConfig.readEncryptionChecks = false;

		Task task = taskManager.createTaskInstance();
		OperationResult result = new OperationResult(AbstractImportTest.class.getName() + "." + TEST_NAME);
		FileInputStream stream = new FileInputStream(getFile(USER_HERMAN_FILE_NAME, true));

		ImportOptionsType importOptions = getDefaultImportOptions();
		importOptions.setEncryptProtectedValues(false);

		dummyAuditService.clear();

		// WHEN
		modelService.importObjectsFromStream(stream, getLanguage(), importOptions, task, result);

		// THEN
		result.computeStatus();
		display("Result after good import", result);
		TestUtil.assertSuccess("Import has failed (result)", result);

		// Check import with fixed OID
		PrismObject<UserType> userHerman = getUser(USER_HERMAN_OID);
		display("Herman", userHerman);
		assertUser(userHerman, USER_HERMAN_OID, USER_HERMAN_USERNAME, "Herman Toothrot", "Herman", "Toothrot");

		// Check if the password was NOT encrypted
		ProtectedStringType protectedString = userHerman.asObjectable().getCredentials().getPassword().getValue();
		assertEquals("Er? Pirate sectrets still hidden?", "m0nk3y", protectedString.getClearValue());
		assertNull("Er? Encrypted data together with clear value?", protectedString.getEncryptedDataType());

		assertUsers(6);

		// Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(1);
        dummyAuditService.assertHasDelta(ChangeType.ADD, UserType.class);
        dummyAuditService.assertExecutionSuccess();
	}

	@Test
	public void test050ImportUserHermanOverwriteFullProcessing() throws Exception {
		final String TEST_NAME = "test050ImportUserHermanOverwriteFullProcessing";
		TestUtil.displayTestTitle(this,TEST_NAME);
		// GIVEN

		Task task = taskManager.createTaskInstance();
		OperationResult result = new OperationResult(AbstractImportTest.class.getName() + "." + TEST_NAME);
		FileInputStream stream = new FileInputStream(getFile(USER_HERMAN_FILE_NAME, true));

		ImportOptionsType importOptions = getDefaultImportOptions();
		importOptions.setOverwrite(true);
		importOptions.setKeepOid(true);
		importOptions.setModelExecutionOptions(new ModelExecuteOptionsType().raw(false));

		dummyAuditService.clear();

		// WHEN
		modelService.importObjectsFromStream(stream, getLanguage(), importOptions, task, result);

		// THEN
		result.computeStatus();
		display("Result after good import", result);
		TestUtil.assertSuccess("Import has failed (result)", result);

		// Check import with fixed OID
		PrismObject<UserType> userHerman = getUser(USER_HERMAN_OID);
		display("Herman", userHerman);
		assertUser(userHerman, USER_HERMAN_OID, USER_HERMAN_USERNAME, "Herman Toothrot", "Herman", "Toothrot");

		assertUsers(6);

		// Check audit
		display("Audit", dummyAuditService);
		dummyAuditService.assertRecords(2);
		dummyAuditService.assertSimpleRecordSanity();
		dummyAuditService.assertAnyRequestDeltas();
		dummyAuditService.assertExecutionDeltas(1);
		dummyAuditService.assertHasDelta(ChangeType.ADD, UserType.class);
		dummyAuditService.assertExecutionSuccess();

		// checks whether the model execution was done
		assertEquals("Validity status not computed", TimeIntervalStatusType.IN, userHerman.asObjectable().getActivation().getValidityStatus());
	}

	@Test
	public void test100GoodRefImport() throws Exception {
		final String TEST_NAME = "test100GoodRefImport";
		TestUtil.displayTestTitle(this,TEST_NAME);
		// GIVEN
		Task task = taskManager.createTaskInstance();
		OperationResult result = new OperationResult(AbstractImportTest.class.getName() + "." +TEST_NAME);
		FileInputStream stream = new FileInputStream(getFile(IMPORT_REF_FILE_NAME, false));

		// WHEN
		modelService.importObjectsFromStream(stream, getLanguage(), getDefaultImportOptions(), task, result);

		// THEN
		result.computeStatus("Failed import.");
		display("Result after good import", result);
		TestUtil.assertSuccessOrWarning("Import has failed (result)", result, 2);

		//		EqualsFilter equal = EqualsFilter.createEqual(UserType.F_NAME, UserType.class, PrismTestUtil.getPrismContext(), null, "jack");
		//		ObjectQuery query = ObjectQuery.createObjectQuery(equal);
		ObjectQuery query = ObjectQueryUtil.createNameQuery("jack", PrismTestUtil.getPrismContext());

		List<PrismObject<UserType>> users = repositoryService.searchObjects(UserType.class, query, null, result);

		assertNotNull(users);
		assertEquals("Search returned unexpected results", 1, users.size());
		UserType jack = users.get(0).asObjectable();
		assertNotNull(jack);
		PrismAsserts.assertEqualsPolyString("wrong givenName", "Jack", jack.getGivenName());
		PrismAsserts.assertEqualsPolyString("wrong familyName", "Sparrow", jack.getFamilyName());
		PrismAsserts.assertEqualsPolyString("wrong fullName", "Cpt. Jack Sparrow", jack.getFullName());

	}

	@Test
	public void test200BadImport() throws FileNotFoundException, SchemaException, ObjectNotFoundException {
		TestUtil.displayTestTitle(this,"test200BadImport");
		// GIVEN
		Task task = taskManager.createTaskInstance();
		OperationResult result = new OperationResult(AbstractImportTest.class.getName() + "test001GoodImport");
		FileInputStream stream = new FileInputStream(getFile(BAD_IMPORT_FILE_NAME, false));

		repositoryService.deleteObject(UserType.class, USER_JACK_OID, result);

		// WHEN
		modelService.importObjectsFromStream(stream, getLanguage(), getDefaultImportOptions(), task, result);

		// THEN
		result.computeStatus("Failed import.");
		display("Result after bad import", result);

		// Jack is OK in the import file, he should be imported
		try {
			UserType jack = repositoryService.getObject(UserType.class, USER_JACK_OID, null, result).asObjectable();
			AssertJUnit.assertNotNull("Jack is null", jack);
		} catch (ObjectNotFoundException e) {
			AssertJUnit.fail("Jack was not imported");
		}

		List<PrismObject<UserType>> users = repositoryService.searchObjects(UserType.class, null, null, result);

		AssertJUnit.assertNotNull(users);
		AssertJUnit.assertEquals("Search returned unexpected results: "+users, 7, users.size());

	}


	private void assertDummyResource(PrismObject<ResourceType> resource, boolean fromRepo) {
		PrismContainer<Containerable> configurationPropertiesContainer = assertResource(resource, "Dummy Resource", RESOURCE_DUMMY_NAMESPACE,
				dummyConnector.getOid(), fromRepo);
		PrismProperty<ProtectedStringType> guardedProperty = configurationPropertiesContainer.findProperty(
				new QName(CONNECTOR_DUMMY_NAMESPACE, "uselessGuardedString"));
		// The resource was pulled from the repository. Therefore it does not have the right schema here. We should proceed with caution
		// and inspect the DOM elements there
		assertNotNull("No uselessGuardedString property in configuration properties", guardedProperty);
		PrismPropertyValue<ProtectedStringType> guardedPVal = guardedProperty.getValue();

		if (fromRepo) {
			Object passwordRawElement = guardedPVal.getRawElement();
			if (!(passwordRawElement instanceof MapXNode)) {
				AssertJUnit.fail("Expected password value of type "+MapXNode.class+" but got "+passwordRawElement.getClass());
			}
			MapXNode passwordXNode = (MapXNode) passwordRawElement;
			assertTrue("uselessGuardedString was not encrypted (clearValue)", passwordXNode.get(new QName("clearValue")) == null);
	        assertTrue("uselessGuardedString was not encrypted (no encryptedData)", passwordXNode.get(new QName("encryptedData")) != null);
		} else {
			ProtectedStringType psType = guardedPVal.getValue();
			assertNull("uselessGuardedString was not encrypted (clearValue)", psType.getClearValue());
			assertNotNull("uselessGuardedString was not encrypted (no EncryptedData)", psType.getEncryptedDataType());
		}
	}

	private PrismContainer<Containerable> assertResource(PrismObject<ResourceType> resource, String resourceName, String namespace,
			String connectorOid, boolean fromRepo) {
		ResourceType resourceType = resource.asObjectable();
		assertNotNull(resourceType);
		PrismAsserts.assertEqualsPolyString("Wrong resource name", resourceName, resourceType.getName());
		assertEquals("Wrong namespace of "+resource, namespace, ResourceTypeUtil.getResourceNamespace(resourceType));
		assertEquals("Wrong connector OID in "+resource, connectorOid, resourceType.getConnectorRef().getOid());

		// The password in the resource configuration should be encrypted after import
		PrismContainer<Containerable> configurationContainer = resource.findContainer(ResourceType.F_CONNECTOR_CONFIGURATION);
		PrismContainer<Containerable> configurationPropertiesContainer =
			configurationContainer.findContainer(SchemaTestConstants.ICFC_CONFIGURATION_PROPERTIES);
		assertNotNull("No configurationProperties in resource", configurationPropertiesContainer);

		return configurationPropertiesContainer;
	}

	private <O extends ObjectType> void assertMetadata(O objectType, XMLGregorianCalendar startTime, XMLGregorianCalendar endTime) {
		MetadataType metadata = objectType.getMetadata();
		assertNotNull("No metadata in "+objectType, metadata);
		XMLGregorianCalendar createTimestamp = metadata.getCreateTimestamp();
		assertNotNull("No createTimestamp in metadata of "+objectType, createTimestamp);
		TestUtil.assertBetween("Wrong createTimestamp in metadata of "+objectType, startTime, endTime, createTimestamp);
		assertEquals("Wrong channel in metadata of "+objectType, SchemaConstants.CHANNEL_OBJECT_IMPORT_URI, metadata.getCreateChannel());
	}

}
