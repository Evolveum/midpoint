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

/**
 *
 */
package com.evolveum.midpoint.provisioning.impl;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.provisioning.impl.mock.SynchornizationServiceMock;
import com.evolveum.midpoint.schema.CapabilityUtil;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.AbstractIntegrationTest;
import com.evolveum.midpoint.test.util.DerbyController;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CredentialsCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.PasswordCapabilityType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

import static org.testng.AssertJUnit.*;

/**
 *
 * @author Radovan Semancik
 *
 */
@ContextConfiguration(locations = "classpath:ctx-provisioning-test-main.xml")
@DirtiesContext
public class TestDBTable extends AbstractIntegrationTest {

	protected static final File TEST_DIR = new File("src/test/resources/db");

	private static final File RESOURCE_DERBY_FILE = new File(TEST_DIR, "resource-derby.xml");
	private static final String RESOURCE_DERBY_OID = "ef2bc95b-76e0-59e2-86d6-999902d3abab";
	private static final File ACCOUNT_WILL_FILE = new File(TEST_DIR, "account-derby.xml");
	private static final String ACCOUNT_WILL_OID = "c0c010c0-d34d-b44f-f11d-333222123456";
	private static final String ACCOUNT_WILL_USERNAME = "will";
	private static final String ACCOUNT_WILL_FULLNAME = "Will Turner";
	private static final String ACCOUNT_WILL_PASSWORD = "3lizab3th";
	private static final String DB_TABLE_CONNECTOR_TYPE = "org.identityconnectors.databasetable.DatabaseTableConnector";

	private static final Trace LOGGER = TraceManager.getTrace(TestDBTable.class);

	private static DerbyController derbyController = new DerbyController();

	@Autowired
	private ProvisioningService provisioningService;

//	@Autowired
//	private TaskManager taskManager;

	@Autowired
	private SynchornizationServiceMock syncServiceMock;


	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.test.AbstractIntegrationTest#initSystem()
	 */

	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		// We need to switch off the encryption checks. Some values cannot be encrypted as we do
		// not have a definition here
		InternalsConfig.encryptionChecks = false;
		provisioningService.postInit(initResult);
		addResourceFromFile(RESOURCE_DERBY_FILE, DB_TABLE_CONNECTOR_TYPE, initResult);
	}

	@BeforeClass
	public static void startDb() throws Exception {
		LOGGER.info("------------------------------------------------------------------------------");
		LOGGER.info("START:  ProvisioningServiceImplDBTest");
		LOGGER.info("------------------------------------------------------------------------------");
		derbyController.startCleanServer();
	}

	@AfterClass
	public static void stopDb() throws Exception {
		derbyController.stop();
		LOGGER.info("------------------------------------------------------------------------------");
		LOGGER.info("STOP:  ProvisioningServiceImplDBTest");
		LOGGER.info("------------------------------------------------------------------------------");
	}


	@Test
	public void test000Integrity() throws ObjectNotFoundException, SchemaException {
		TestUtil.displayTestTitle("test000Integrity");

		OperationResult result = new OperationResult(TestDBTable.class.getName()+".test000Integrity");

		ResourceType resource = repositoryService.getObject(ResourceType.class, RESOURCE_DERBY_OID, null, result).asObjectable();
		String connectorOid = resource.getConnectorRef().getOid();
		ConnectorType connector = repositoryService.getObject(ConnectorType.class, connectorOid, null, result).asObjectable();
		assertNotNull(connector);
		display("DB Connector",connector);
	}

	@Test
	public void test001Connection() throws Exception {
		final String TEST_NAME = "test001Connection";
		TestUtil.displayTestTitle(TEST_NAME);
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		// WHEN
		OperationResult testResult = provisioningService.testResource(RESOURCE_DERBY_OID, task);

		display("Test result",testResult);
		TestUtil.assertSuccess("Test resource failed (result)", testResult);

		ResourceType resource = repositoryService.getObject(ResourceType.class, RESOURCE_DERBY_OID, null, result).asObjectable();
		display("Resource after test",resource);
		display("Resource after test (XML)", PrismTestUtil.serializeObjectToString(resource.asPrismObject(), PrismContext.LANG_XML));

		List<Object> nativeCapabilities = resource.getCapabilities().getNative().getAny();
		CredentialsCapabilityType credentialsCapabilityType = CapabilityUtil.getCapability(nativeCapabilities, CredentialsCapabilityType.class);
		assertNotNull("No credentials capability", credentialsCapabilityType);
		PasswordCapabilityType passwordCapabilityType = credentialsCapabilityType.getPassword();
		assertNotNull("No password in credentials capability", passwordCapabilityType);
		assertEquals("Wrong password capability ReturnedByDefault", Boolean.FALSE, passwordCapabilityType.isReturnedByDefault());
	}

	@Test
	public void test002AddAccount() throws Exception {
		final String TEST_NAME = "test002AddAccount";
		TestUtil.displayTestTitle(TEST_NAME);
		// GIVEN
		OperationResult result = new OperationResult(TestDBTable.class.getName()
				+ "." + TEST_NAME);

		ShadowType account = parseObjectType(ACCOUNT_WILL_FILE, ShadowType.class);

		System.out.println(SchemaDebugUtil.prettyPrint(account));
		System.out.println(account.asPrismObject().debugDump());

		Task task = taskManager.createTaskInstance();
		// WHEN
		String addedObjectOid = provisioningService.addObject(account.asPrismObject(), null, null, task, result);

		// THEN
		result.computeStatus();
		display("add object result",result);
		TestUtil.assertSuccess("addObject has failed (result)",result);
		assertEquals(ACCOUNT_WILL_OID, addedObjectOid);

		ShadowType accountType =  repositoryService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null, result).asObjectable();
		PrismAsserts.assertEqualsPolyString("Name not equal.", ACCOUNT_WILL_USERNAME, accountType.getName());
//		assertEquals("will", accountType.getName());

		ShadowType provisioningAccountType = provisioningService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null, task, result).asObjectable();
		PrismAsserts.assertEqualsPolyString("Name not equal.", ACCOUNT_WILL_USERNAME, provisioningAccountType.getName());
//		assertEquals("will", provisioningAccountType.getName());

		// Check database content

		Connection conn = derbyController.getConnection();
		// Check if it empty
		Statement stmt = conn.createStatement();
		stmt.execute("select * from users");
		ResultSet rs = stmt.getResultSet();

		assertTrue("The \"users\" table is empty",rs.next());
		assertEquals(ACCOUNT_WILL_USERNAME,rs.getString(DerbyController.COLUMN_LOGIN));
		assertEquals(ACCOUNT_WILL_PASSWORD,rs.getString(DerbyController.COLUMN_PASSWORD));
		assertEquals(ACCOUNT_WILL_FULLNAME,rs.getString(DerbyController.COLUMN_FULL_NAME));

		assertFalse("The \"users\" table has more than one record",rs.next());
		rs.close();
		stmt.close();
	}

	// MID-1234
	@Test(enabled=false)
	public void test005GetAccount() throws Exception {
		final String TEST_NAME = "test005GetAccount";
		TestUtil.displayTestTitle(TEST_NAME);
		// GIVEN
		OperationResult result = new OperationResult(TestDBTable.class.getName()
				+ "." + TEST_NAME);

		Task task = taskManager.createTaskInstance();
		// WHEN
		PrismObject<ShadowType> account = provisioningService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null, task, result);

		// THEN
		result.computeStatus();
		display(result);
		TestUtil.assertSuccess(result);

		PrismAsserts.assertEqualsPolyString("Name not equal.", ACCOUNT_WILL_USERNAME, account.asObjectable().getName());

		assertNotNull("No credentials", account.asObjectable().getCredentials());
		assertNotNull("No password", account.asObjectable().getCredentials().getPassword());
		assertNotNull("No password value", account.asObjectable().getCredentials().getPassword().getValue());
		ProtectedStringType password = account.asObjectable().getCredentials().getPassword().getValue();
		display("Password", password);
		String clearPassword = protector.decryptString(password);
		assertEquals("Wrong password", ACCOUNT_WILL_PASSWORD, clearPassword);
	}


}
