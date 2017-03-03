/*
 * Copyright (c) 2013-2015 Evolveum
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
package com.evolveum.midpoint.testing.rest;

import static com.evolveum.midpoint.test.util.TestUtil.displayTestTile;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.client.ClientConfiguration;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.transport.local.LocalConduit;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.impl.rest.MidpointAbstractProvider;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.provisioning.impl.ProvisioningServiceImpl;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.sql.SqlRepositoryServiceImpl;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.test.DummyAuditService;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTemplateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

@ContextConfiguration(locations = { "classpath:ctx-rest-test.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public abstract class TestAbstractRestService {
	
//	protected static final File BASE_DIR = new File("src/test/resources");
	protected static final File BASE_REPO_DIR = new File("src/test/resources/repo/");
	protected static final File BASE_REQ_DIR = new File("src/test/resources/req/");
	
	public static final File USER_ADMINISTRATOR_FILE = new File(BASE_REPO_DIR, "user-administrator.xml");
	public static final String USER_ADMINISTRATOR_USERNAME = "administrator";
	public static final String USER_ADMINISTRATOR_PASSWORD = "5ecr3t";

	// No authorization
	public static final File USER_NOBODY_FILE = new File(BASE_REPO_DIR, "user-nobody.xml");
	public static final String USER_NOBODY_USERNAME = "nobody";
	public static final String USER_NOBODY_PASSWORD = "nopassword";

	// REST authorization only
	public static final File USER_CYCLOPS_FILE = new File(BASE_REPO_DIR, "user-cyclops.xml");
	public static final String USER_CYCLOPS_USERNAME = "cyclops";
	public static final String USER_CYCLOPS_PASSWORD = "cyclopassword";
	
	// REST and reader authorization
	public static final File USER_SOMEBODY_FILE = new File(BASE_REPO_DIR, "user-somebody.xml");
	public static final String USER_SOMEBODY_USERNAME = "somebody";
	public static final String USER_SOMEBODY_PASSWORD = "somepassword";
	
	// REST, reader and adder authorization
 	public static final String USER_DARTHADDER_FILE = "user-darthadder";//new File(REPO_DIR, "user-darthadder.xml");
 	public static final String USER_DARTHADDER_OID = "1696229e-d90a-11e4-9ce6-001e8c717e5b";
 	public static final String USER_DARTHADDER_USERNAME = "darthadder";
 	public static final String USER_DARTHADDER_PASSWORD = "iamyouruncle";
 	
 	// Authorizations, but no password
 	public static final String USER_NOPASSWORD_FILE = "user-nopassword"; //new File(REPO_DIR, "user-nopassword.xml");
 	public static final String USER_NOPASSWORD_USERNAME = "nopassword";

	public static final File ROLE_SUPERUSER_FILE = new File(BASE_REPO_DIR, "role-superuser.xml");
	public static final File ROLE_REST_FILE = new File(BASE_REPO_DIR, "role-rest.xml");
	public static final File ROLE_READER_FILE = new File(BASE_REPO_DIR, "role-reader.xml");
	public static final String ROLE_ADDER_FILE = "role-adder";//new File(REPO_DIR, "role-adder.xml");
	
	public static final String ROLE_MODIFIER_FILE = "role-modifier"; //new File(REPO_DIR, "role-modifier.xml");
	public static final String ROLE_MODIFIER_OID = "82005ae4-d90b-11e4-bdcc-001e8c717e5b";

	public static final File RESOURCE_OPENDJ_FILE = new File(BASE_REPO_DIR, "reosurce-opendj.xml");
	public static final String RESOURCE_OPENDJ_OID = "ef2bc95b-76e0-59e2-86d6-3d4f02d3ffff";

	public static final String USER_TEMPLATE_FILE = "user-template";//new File(REPO_DIR, "user-template.xml");
	public static final String USER_TEMPLATE_OID = "c0c010c0-d34d-b33f-f00d-777111111111";

	public static final String ACCOUT_CHUCK_FILE = "account-chuck"; //new File(BASE_REPO_DIR, "account-chuck.xml");
	public static final String ACCOUT_CHUCK_OID = BASE_REPO_DIR + "a0c010c0-d34d-b33f-f00d-111111111666";

	public static final File SYSTEM_CONFIGURATION_FILE = new File(BASE_REPO_DIR, "system-configuration.xml");

	private static final Trace LOGGER = TraceManager.getTrace(TestAbstractRestService.class);

	private final static String ENDPOINT_ADDRESS = "http://localhost:18080/rest";

	private static final String MODIFICATION_DISABLE = "modification-disable"; //new File(REQ_DIR, "modification-disable.xml");
	private static final String MODIFICATION_ENABLE = "modification-enable"; //new File(REQ_DIR, "modification-enable.xml");
	private static final String MODIFICATION_ASSIGN_ROLE_MODIFIER = "modification-assign-role-modifier"; //new File(REQ_DIR, "modification-assign-role-modifier.xml");

	private  PrismContext prismContext;
	private  TaskManager taskManager;
	private  ModelService modelService;

	private  Server server;

	private  RepositoryService repositoryService;
	private  ProvisioningService provisioning;
	private  DummyAuditService dummyAuditService;
	
	protected  TestXmlProvider xmlProvider;
	protected  TestJsonProvider jsonProvider;
	protected  TestYamlProvider yamlProvider;
	
	protected abstract String getAcceptHeader();
	protected abstract String getContentType();
	protected abstract MidpointAbstractProvider getProvider();
	
	protected abstract File getRepoFile(String fileBaseName);
	protected abstract File getRequestFile(String fileBaseName);

	ApplicationContext applicationContext = null;
	
	@BeforeClass
	public void initialize() throws Exception {
		startServer();
	}
	
	@AfterClass
	public void shutDown() {
		((ClassPathXmlApplicationContext)applicationContext).close();
	}
	
	
	private void startServer() throws Exception {
		applicationContext = new ClassPathXmlApplicationContext("ctx-rest-test-main.xml");
		LOGGER.info("Spring context initialized.");
		
		JAXRSServerFactoryBean sf = (JAXRSServerFactoryBean) applicationContext.getBean("restService");

		sf.setAddress(ENDPOINT_ADDRESS);

		server = sf.create();

		repositoryService = (SqlRepositoryServiceImpl) applicationContext.getBean("repositoryService");
		provisioning = (ProvisioningServiceImpl) applicationContext.getBean("provisioningService");
		taskManager = (TaskManager) applicationContext.getBean("taskManager");
		modelService = (ModelService) applicationContext.getBean("modelController");
		xmlProvider = (TestXmlProvider) applicationContext.getBean("testXmlProvider");
		jsonProvider = (TestJsonProvider) applicationContext.getBean("testJsonProvider");
		yamlProvider = (TestYamlProvider) applicationContext.getBean("testYamlProvider");

		Task initTask = taskManager.createTaskInstance(TestAbstractRestService.class.getName() + ".startServer");
		OperationResult result = initTask.getResult();

		InternalsConfig.encryptionChecks = false;
		
		prismContext = (PrismContext) applicationContext.getBean("prismContext");
		addObject(ROLE_SUPERUSER_FILE, result);
		addObject(ROLE_REST_FILE, result);
		addObject(ROLE_READER_FILE, result);
		addObject(USER_ADMINISTRATOR_FILE, result);
		addObject(USER_NOBODY_FILE, result);
		addObject(USER_CYCLOPS_FILE, result);
		addObject(USER_SOMEBODY_FILE, result);
		addObject(SYSTEM_CONFIGURATION_FILE, result);

		dummyAuditService = DummyAuditService.getInstance();
		
		InternalMonitor.reset();

		modelService.postInit(result);

		result.computeStatus();
		TestUtil.assertSuccessOrWarning("startServer failed (result)", result, 1);
	}
	
	private <O extends ObjectType> PrismObject<O> addObject(File file, OperationResult result) throws SchemaException, IOException, ObjectAlreadyExistsException {
		PrismObject<O> object = prismContext.parseObject(file);
		String oid = repositoryService.addObject(object, null, result);
		object.setOid(oid);
		return object;
	}

	
	public TestAbstractRestService() {
		super();
	}
	
	@Test
	public void test001GetUserAdministrator() {
		final String TEST_NAME = "test001GetUserAdministrator";
		displayTestTile(this, TEST_NAME);

		WebClient client = prepareClient();
		client.path("/users/" + SystemObjectsType.USER_ADMINISTRATOR.value());
		
		dummyAuditService.clear();

		TestUtil.displayWhen(TEST_NAME);
		Response response = client.get();
		
		TestUtil.displayThen(TEST_NAME);
		assertStatus(response, 200);
		UserType userType = response.readEntity(UserType.class);
		assertNotNull("Returned entity in body must not be null.", userType);
		LOGGER.info("Returned entity: {}", userType.asPrismObject().debugDump());
		
		IntegrationTestTools.display("Audit", dummyAuditService);
		dummyAuditService.assertRecords(2);
		dummyAuditService.assertLoginLogout(SchemaConstants.CHANNEL_REST_URI);
	}

	@Test
	public void test002GetNonExistingUser() {
		final String TEST_NAME = "test002GetNonExistingUser";
		displayTestTile(this, TEST_NAME);

		WebClient client = prepareClient();
		client.path("/users/12345");
		
		dummyAuditService.clear();

		TestUtil.displayWhen(TEST_NAME);
		Response response = client.get();

		TestUtil.displayThen(TEST_NAME);
		assertStatus(response, 404);
		OperationResultType result = response.readEntity(OperationResultType.class);
		assertNotNull("Error response must contain operation result", result);
		LOGGER.info("Returned result: {}", result);
		assertEquals("Unexpected operation result status", OperationResultStatusType.FATAL_ERROR, result.getStatus());

		IntegrationTestTools.display("Audit", dummyAuditService);
		dummyAuditService.assertRecords(2);
		dummyAuditService.assertLoginLogout(SchemaConstants.CHANNEL_REST_URI);
	}

	@Test
	public void test003GetNoAuthHeaders() {
		final String TEST_NAME = "test003GetNoAuthHeaders";
		displayTestTile(this, TEST_NAME);

		WebClient client = prepareClient(null, null);
		client.path("/users/" + SystemObjectsType.USER_ADMINISTRATOR.value());
		
		dummyAuditService.clear();

		TestUtil.displayWhen(TEST_NAME);
		Response response = client.get();

		TestUtil.displayThen(TEST_NAME);
		assertStatus(response, 401);

		IntegrationTestTools.display("Audit", dummyAuditService);
		// No records. There are no auth headers so this is not considered to be a login attempt
		dummyAuditService.assertRecords(0);
	}

	@Test
	public void test004GetAuthBadUsernameNullPassword() {
		final String TEST_NAME = "test004GetAuthBadUsernameNullPassword";
		displayTestTile(this, TEST_NAME);

		WebClient client = prepareClient("NoSUCHuser", null);
		client.path("/users/" + SystemObjectsType.USER_ADMINISTRATOR.value());
		
		dummyAuditService.clear();

		TestUtil.displayWhen(TEST_NAME);
		Response response = client.get();

		TestUtil.displayThen(TEST_NAME);
		assertStatus(response, 401);

		IntegrationTestTools.display("Audit", dummyAuditService);
		dummyAuditService.assertRecords(1);
		dummyAuditService.assertFailedLogin(SchemaConstants.CHANNEL_REST_URI);
	}

	@Test
	public void test005GetAuthBadUsernameEmptyPassword() {
		final String TEST_NAME = "test005GetAuthBadUsernameEmptyPassword";
		displayTestTile(this, TEST_NAME);

		WebClient client = prepareClient("NoSUCHuser", "");
		client.path("/users/" + SystemObjectsType.USER_ADMINISTRATOR.value());
		
		dummyAuditService.clear();

		TestUtil.displayWhen(TEST_NAME);
		Response response = client.get();

		TestUtil.displayThen(TEST_NAME);
		assertStatus(response, 401);

		IntegrationTestTools.display("Audit", dummyAuditService);
		dummyAuditService.assertRecords(1);
		dummyAuditService.assertFailedLogin(SchemaConstants.CHANNEL_REST_URI);
	}

	@Test
	public void test006GetAuthBadUsernameBadPassword() {
		final String TEST_NAME = "test006GetAuthBadUsernameBadPassword";
		displayTestTile(this, TEST_NAME);

		WebClient client = prepareClient("NoSUCHuser", "NoSuchPassword");
		client.path("/users/" + SystemObjectsType.USER_ADMINISTRATOR.value());
		
		dummyAuditService.clear();

		TestUtil.displayWhen(TEST_NAME);
		Response response = client.get();

		TestUtil.displayThen(TEST_NAME);
		assertStatus(response, 401);

		IntegrationTestTools.display("Audit", dummyAuditService);
		dummyAuditService.assertRecords(1);
		dummyAuditService.assertFailedLogin(SchemaConstants.CHANNEL_REST_URI);
	}

	@Test
	public void test007GetAuthNoPassword() {
		final String TEST_NAME = "test007GetAuthNoPassword";
		displayTestTile(this, TEST_NAME);

		WebClient client = prepareClient(USER_ADMINISTRATOR_USERNAME, null);
		client.path("/users/" + SystemObjectsType.USER_ADMINISTRATOR.value());
		
		dummyAuditService.clear();

		TestUtil.displayWhen(TEST_NAME);
		Response response = client.get();

		TestUtil.displayThen(TEST_NAME);
		assertStatus(response, 401);

		IntegrationTestTools.display("Audit", dummyAuditService);
		dummyAuditService.assertRecords(1);
		dummyAuditService.assertFailedLogin(SchemaConstants.CHANNEL_REST_URI);
	}

	@Test
	public void test016GetAuthBadPassword() {
		final String TEST_NAME = "test016GetAuthBadPassword";
		displayTestTile(this, TEST_NAME);

		WebClient client = prepareClient(USER_ADMINISTRATOR_USERNAME, "forgot");
		client.path("/users/" + SystemObjectsType.USER_ADMINISTRATOR.value());
		
		dummyAuditService.clear();

		TestUtil.displayWhen(TEST_NAME);
		Response response = client.get();

		TestUtil.displayThen(TEST_NAME);
		assertStatus(response, 401);

		IntegrationTestTools.display("Audit", dummyAuditService);
		dummyAuditService.assertRecords(1);
		dummyAuditService.assertFailedLogin(SchemaConstants.CHANNEL_REST_URI);
	}
	
	@Test
	public void test017GetUnauthorizedUser() {
		final String TEST_NAME = "test017GetUnauthorizedUser";
		displayTestTile(this, TEST_NAME);

		WebClient client = prepareClient(USER_NOBODY_USERNAME, USER_NOBODY_PASSWORD);
		client.path("/users/" + SystemObjectsType.USER_ADMINISTRATOR.value());
		
		dummyAuditService.clear();

		TestUtil.displayWhen(TEST_NAME);
		Response response = client.get();

		TestUtil.displayThen(TEST_NAME);
		assertStatus(response, 403);

		IntegrationTestTools.display("Audit", dummyAuditService);
		dummyAuditService.assertRecords(1);
		dummyAuditService.assertFailedLogin(SchemaConstants.CHANNEL_REST_URI);
	}
	
	@Test
	public void test018GetUserAdministratorByCyclops() {
		final String TEST_NAME = "test018GetUserAdministratorByCyclops";
		displayTestTile(this, TEST_NAME);

		WebClient client = prepareClient(USER_CYCLOPS_USERNAME, USER_CYCLOPS_PASSWORD);
		client.path("/users/" + SystemObjectsType.USER_ADMINISTRATOR.value());
		
		dummyAuditService.clear();

		TestUtil.displayWhen(TEST_NAME);
		Response response = client.get();

		TestUtil.displayThen(TEST_NAME);
		assertStatus(response, 403);

		IntegrationTestTools.display("Audit", dummyAuditService);
		dummyAuditService.assertRecords(2);
		dummyAuditService.assertLoginLogout(SchemaConstants.CHANNEL_REST_URI);
	}
	
	@Test
	public void test019GetUserAdministratorBySomebody() {
		final String TEST_NAME = "test019GetUserAdministratorBySomebody";
		displayTestTile(this, TEST_NAME);

		WebClient client = prepareClient(USER_SOMEBODY_USERNAME, USER_SOMEBODY_PASSWORD);
		client.path("/users/" + SystemObjectsType.USER_ADMINISTRATOR.value());
		
		dummyAuditService.clear();

		TestUtil.displayWhen(TEST_NAME);
		Response response = client.get();

		TestUtil.displayThen(TEST_NAME);
		
		assertStatus(response, 200);
		
		UserType userType = response.readEntity(UserType.class);
		assertNotNull("Returned entity in body must not be null.", userType);
		LOGGER.info("Returned entity: {}", userType.asPrismObject().debugDump());
		
		IntegrationTestTools.display("Audit", dummyAuditService);
		dummyAuditService.assertRecords(2);
		dummyAuditService.assertLoginLogout(SchemaConstants.CHANNEL_REST_URI);
	}

	@Test
	public void test102AddUserTemplate() throws Exception {
		final String TEST_NAME = "test102AddUserTemplate";
		displayTestTile(this, TEST_NAME);

		WebClient client = prepareClient();
		client.path("/objectTemplates/");
		
		dummyAuditService.clear();

		TestUtil.displayWhen(TEST_NAME);
		Response response = client.post(getRepoFile(USER_TEMPLATE_FILE));

		TestUtil.displayThen(TEST_NAME);
		displayResponse(response);

		assertStatus(response, 201);

		IntegrationTestTools.display("Audit", dummyAuditService);
		dummyAuditService.assertRecords(4);
		dummyAuditService.assertLoginLogout(SchemaConstants.CHANNEL_REST_URI);
		dummyAuditService.assertHasDelta(1, ChangeType.ADD, ObjectTemplateType.class);
	}

	@Test
	public void test103AddUserBadTargetCollection() throws Exception {
		final String TEST_NAME = "test103AddUserBadTargetCollection";
		displayTestTile(this, TEST_NAME);

		WebClient client = prepareClient();
		client.path("/objectTemplates");
		
		dummyAuditService.clear();

		TestUtil.displayWhen(TEST_NAME);
		Response response = client.post(getRepoFile(USER_DARTHADDER_FILE));

		TestUtil.displayThen(TEST_NAME);
		displayResponse(response);

		assertStatus(response, 400);
		OperationResultType result = response.readEntity(OperationResultType.class);
		assertNotNull("Error response must contain operation result", result);
		LOGGER.info("Returned result: {}", result);
		assertEquals("Unexpected operation result status", OperationResultStatusType.FATAL_ERROR, result.getStatus());

		IntegrationTestTools.display("Audit", dummyAuditService);
		dummyAuditService.assertRecords(2);
		dummyAuditService.assertLoginLogout(SchemaConstants.CHANNEL_REST_URI);
	}

	@Test
	public void test104AddAccountRaw() throws Exception {
		final String TEST_NAME = "test104AddAccountRaw";
		displayTestTile(this, TEST_NAME);

		WebClient client = prepareClient();
		client.path("/shadows");
		client.query("options", "raw");
		
		dummyAuditService.clear();

		TestUtil.displayWhen(TEST_NAME);
		Response response = client.post(getRepoFile(ACCOUT_CHUCK_FILE));

		TestUtil.displayThen(TEST_NAME);
		displayResponse(response);

		assertStatus(response, 201);

		OperationResult parentResult = new OperationResult("get");
		try {
			provisioning.getObject(ShadowType.class, ACCOUT_CHUCK_OID,
					SelectorOptions.createCollection(GetOperationOptions.createDoNotDiscovery()), null,
					parentResult);
			fail("expected object not found exception but haven't got one.");
		} catch (ObjectNotFoundException ex) {
			// this is OK..we expect objet not found, because accout was added
			// with the raw options which indicates, that it was created only in
			// the repository
		}

		IntegrationTestTools.display("Audit", dummyAuditService);
		dummyAuditService.assertRecords(4);
		dummyAuditService.assertLoginLogout(SchemaConstants.CHANNEL_REST_URI);
		dummyAuditService.assertHasDelta(1, ChangeType.ADD, ShadowType.class);
	}
	
	@Test
	public void test120AddRoleAdder() throws Exception {
		final String TEST_NAME = "test120AddRoleAdder";
		displayTestTile(this, TEST_NAME);

		WebClient client = prepareClient();
		client.path("/roles");
		
		dummyAuditService.clear();

		TestUtil.displayWhen(TEST_NAME);
		Response response = client.post(getRepoFile(ROLE_ADDER_FILE));

		TestUtil.displayThen(TEST_NAME);
		displayResponse(response);
		assertStatus(response, 201);

		IntegrationTestTools.display("Audit", dummyAuditService);
		dummyAuditService.assertRecords(4);
		dummyAuditService.assertLoginLogout(SchemaConstants.CHANNEL_REST_URI);
		dummyAuditService.assertHasDelta(1, ChangeType.ADD, RoleType.class);
	}

	@Test
	public void test121AddUserDarthAdder() throws Exception {
		final String TEST_NAME = "test121AddUserDarthAdder";
		displayTestTile(this, TEST_NAME);

		WebClient client = prepareClient();
		client.path("/users");
		
		dummyAuditService.clear();

		TestUtil.displayWhen(TEST_NAME);
		Response response = client.post(getRepoFile(USER_DARTHADDER_FILE));

		TestUtil.displayThen(TEST_NAME);
		displayResponse(response);
		assertStatus(response, 201);

		IntegrationTestTools.display("Audit", dummyAuditService);
		dummyAuditService.assertRecords(4);
		dummyAuditService.assertLoginLogout(SchemaConstants.CHANNEL_REST_URI);
		dummyAuditService.assertHasDelta(1, ChangeType.ADD, UserType.class);
	}

	
	@Test
	public void test122AddRoleModifierAsDarthAdder() throws Exception {
		final String TEST_NAME = "test122AddRoleModifierAsDarthAdder";
		displayTestTile(this, TEST_NAME);

		WebClient client = prepareClient(USER_DARTHADDER_USERNAME, USER_DARTHADDER_PASSWORD);
		client.path("/roles");
		
		dummyAuditService.clear();

		TestUtil.displayWhen(TEST_NAME);
		Response response = client.post(getRepoFile(ROLE_MODIFIER_FILE));

		TestUtil.displayThen(TEST_NAME);
		displayResponse(response);
		assertStatus(response, 201);

		IntegrationTestTools.display("Audit", dummyAuditService);
		dummyAuditService.assertRecords(4);
		dummyAuditService.assertLoginLogout(SchemaConstants.CHANNEL_REST_URI);
		dummyAuditService.assertHasDelta(1, ChangeType.ADD, RoleType.class);
	}
	
	@Test
	public void test123DarthAdderAssignModifierHimself() throws Exception {
		final String TEST_NAME = "test123DarthAdderAssignModifierHimself";
		displayTestTile(this, TEST_NAME);

		WebClient client = prepareClient(USER_DARTHADDER_USERNAME, USER_DARTHADDER_PASSWORD);
		client.path("/users/"+USER_DARTHADDER_OID);
		
		dummyAuditService.clear();

		TestUtil.displayWhen(TEST_NAME);
		Response response = client.post(MiscUtil.readFile(getRequestFile(MODIFICATION_ASSIGN_ROLE_MODIFIER)));

		TestUtil.displayThen(TEST_NAME);
		displayResponse(response);
		assertStatus(response, 403);
		OperationResultType result = response.readEntity(OperationResultType.class);
		assertNotNull("Error response must contain operation result", result);
		LOGGER.info("Returned result: {}", result);
		assertEquals("Unexpected operation result status", OperationResultStatusType.FATAL_ERROR, result.getStatus());

		IntegrationTestTools.display("Audit", dummyAuditService);
		dummyAuditService.assertRecords(4);
		dummyAuditService.assertLoginLogout(SchemaConstants.CHANNEL_REST_URI);
		dummyAuditService.assertExecutionOutcome(1, OperationResultStatus.FATAL_ERROR);
	}
	
	@Test
	public void test124DarthAdderAssignModifierByAdministrator() throws Exception {
		final String TEST_NAME = "test124DarthAdderAssignModifierByAdministrator";
		displayTestTile(this, TEST_NAME);

		WebClient client = prepareClient();
		client.path("/users/"+USER_DARTHADDER_OID);
		
		dummyAuditService.clear();

		TestUtil.displayWhen(TEST_NAME);
		Response response = client.post(MiscUtil.readFile(getRequestFile(MODIFICATION_ASSIGN_ROLE_MODIFIER)));

		TestUtil.displayThen(TEST_NAME);
		displayResponse(response);
		assertStatus(response, 204);

		IntegrationTestTools.display("Audit", dummyAuditService);
		dummyAuditService.assertRecords(4);
		dummyAuditService.assertLoginLogout(SchemaConstants.CHANNEL_REST_URI);
		dummyAuditService.assertHasDelta(1, ChangeType.MODIFY, UserType.class);
		
		OperationResult result = new OperationResult("test");
		PrismObject<UserType> user = repositoryService.getObject(UserType.class, USER_DARTHADDER_OID, null, result);
		assertEquals("Unexpected number of assignments", 4, user.asObjectable().getAssignment().size());
	}
	
	@Test
	public void test130DarthAdderDisableHimself() throws Exception {
		final String TEST_NAME = "test130DarthAdderDisableHimself";
		displayTestTile(this, TEST_NAME);

		WebClient client = prepareClient(USER_DARTHADDER_USERNAME, USER_DARTHADDER_PASSWORD);
		client.path("/users/"+USER_DARTHADDER_OID);
		
		dummyAuditService.clear();

		TestUtil.displayWhen(TEST_NAME);
		Response response = client.post(MiscUtil.readFile(getRequestFile(MODIFICATION_DISABLE)));

		TestUtil.displayThen(TEST_NAME);
		displayResponse(response);
		assertStatus(response, 204);

		IntegrationTestTools.display("Audit", dummyAuditService);
		dummyAuditService.assertRecords(4);
		dummyAuditService.assertLoginLogout(SchemaConstants.CHANNEL_REST_URI);
		dummyAuditService.assertHasDelta(1, ChangeType.MODIFY, UserType.class);
		
		OperationResult result = new OperationResult("test");
		PrismObject<UserType> user = repositoryService.getObject(UserType.class, USER_DARTHADDER_OID, null, result);
		assertEquals("Wrong administrativeStatus", ActivationStatusType.DISABLED, user.asObjectable().getActivation().getAdministrativeStatus());
	}
	
	@Test
	public void test131GetUserAdministratorByDarthAdder() {
		final String TEST_NAME = "test131GetUserAdministratorByDarthAdder";
		displayTestTile(this, TEST_NAME);

		WebClient client = prepareClient(USER_DARTHADDER_USERNAME, USER_DARTHADDER_PASSWORD);
		client.path("/users/" + SystemObjectsType.USER_ADMINISTRATOR.value());
		
		dummyAuditService.clear();

		TestUtil.displayWhen(TEST_NAME);
		Response response = client.get();

		TestUtil.displayThen(TEST_NAME);
		assertStatus(response, 403);
		assertNoEmptyResponse(response);
		
		IntegrationTestTools.display("Audit", dummyAuditService);
		dummyAuditService.assertRecords(1);
		dummyAuditService.assertFailedLogin(SchemaConstants.CHANNEL_REST_URI);
	}
	
	@Test
	public void test132DarthAdderEnableByAdministrator() throws Exception {
		final String TEST_NAME = "test132DarthAdderEnableByAdministrator";
		displayTestTile(this, TEST_NAME);

		WebClient client = prepareClient();
		client.path("/users/"+USER_DARTHADDER_OID);
		
		dummyAuditService.clear();

		TestUtil.displayWhen(TEST_NAME);
		Response response = client.post(MiscUtil.readFile(getRequestFile(MODIFICATION_ENABLE)));

		TestUtil.displayThen(TEST_NAME);
		displayResponse(response);
		assertStatus(response, 204);

		IntegrationTestTools.display("Audit", dummyAuditService);
		dummyAuditService.assertRecords(4);
		dummyAuditService.assertLoginLogout(SchemaConstants.CHANNEL_REST_URI);
		dummyAuditService.assertHasDelta(1, ChangeType.MODIFY, UserType.class);
		
		OperationResult result = new OperationResult("test");
		PrismObject<UserType> user = repositoryService.getObject(UserType.class, USER_DARTHADDER_OID, null, result);
		assertEquals("Wrong administrativeStatus", ActivationStatusType.ENABLED, user.asObjectable().getActivation().getAdministrativeStatus());
	}
	
	@Test
	public void test133GetUserAdministratorByDarthAdder() {
		final String TEST_NAME = "test133GetUserAdministratorByDarthAdder";
		displayTestTile(this, TEST_NAME);

		WebClient client = prepareClient(USER_DARTHADDER_USERNAME, USER_DARTHADDER_PASSWORD);
		client.path("/users/" + SystemObjectsType.USER_ADMINISTRATOR.value());
		
		dummyAuditService.clear();

		TestUtil.displayWhen(TEST_NAME);
		Response response = client.get();

		TestUtil.displayThen(TEST_NAME);
		assertStatus(response, 200);
		UserType userType = response.readEntity(UserType.class);
		assertNotNull("Returned entity in body must not be null.", userType);
		LOGGER.info("Returned entity: {}", userType.asPrismObject().debugDump());
		
		IntegrationTestTools.display("Audit", dummyAuditService);
		dummyAuditService.assertRecords(2);
		dummyAuditService.assertLoginLogout(SchemaConstants.CHANNEL_REST_URI);
	}
	
	@Test
	public void test135AddUserNopasswordAsDarthAdder() throws Exception {
		final String TEST_NAME = "test135AddUserNopasswordAsDarthAdder";
		displayTestTile(this, TEST_NAME);

		WebClient client = prepareClient(USER_DARTHADDER_USERNAME, USER_DARTHADDER_PASSWORD);
		client.path("/users");
		
		dummyAuditService.clear();

		TestUtil.displayWhen(TEST_NAME);
		Response response = client.post(getRepoFile(USER_NOPASSWORD_FILE));

		TestUtil.displayThen(TEST_NAME);
		displayResponse(response);
		assertStatus(response, 201);

		IntegrationTestTools.display("Audit", dummyAuditService);
		dummyAuditService.assertRecords(4);
		dummyAuditService.assertLoginLogout(SchemaConstants.CHANNEL_REST_URI);
		dummyAuditService.assertHasDelta(1, ChangeType.ADD, UserType.class);
	}
	
	@Test
	public void test140GetUserAdministratorByNopassword() {
		final String TEST_NAME = "test140GetUserAdministratorByNopassword";
		displayTestTile(this, TEST_NAME);

		WebClient client = prepareClient(USER_NOPASSWORD_USERNAME, null);
		client.path("/users/" + SystemObjectsType.USER_ADMINISTRATOR.value());
		
		dummyAuditService.clear();

		TestUtil.displayWhen(TEST_NAME);
		Response response = client.get();

		TestUtil.displayThen(TEST_NAME);
		assertStatus(response, 401);
		assertNoEmptyResponse(response);
		
		IntegrationTestTools.display("Audit", dummyAuditService);
		dummyAuditService.assertRecords(1);
		dummyAuditService.assertFailedLogin(SchemaConstants.CHANNEL_REST_URI);
	}
	
	@Test
	public void test141GetUserAdministratorByNopasswordBadPassword() {
		final String TEST_NAME = "test140GetUserAdministratorByNopassword";
		displayTestTile(this, TEST_NAME);

		WebClient client = prepareClient(USER_NOPASSWORD_USERNAME, "bad");
		client.path("/users/" + SystemObjectsType.USER_ADMINISTRATOR.value());
		
		dummyAuditService.clear();

		TestUtil.displayWhen(TEST_NAME);
		Response response = client.get();

		TestUtil.displayThen(TEST_NAME);
		assertStatus(response, 403);
		assertNoEmptyResponse(response);
		
		IntegrationTestTools.display("Audit", dummyAuditService);
		dummyAuditService.assertRecords(1);
		dummyAuditService.assertFailedLogin(SchemaConstants.CHANNEL_REST_URI);
	}
	

	@Test
	public void test401AddUserTemplateOverwrite() throws Exception {
		final String TEST_NAME = "test401AddUserTemplateOverwrite";
		displayTestTile(this, TEST_NAME);

		WebClient client = prepareClient();
		client.path("/objectTemplates");
		client.query("options", "overwrite");
		
		dummyAuditService.clear();

		TestUtil.displayWhen(TEST_NAME);
		Response response = client.post(getRepoFile(USER_TEMPLATE_FILE));

		TestUtil.displayThen(TEST_NAME);
		displayResponse(response);

		assertEquals("Expected 201 but got " + response.getStatus(), 201, response.getStatus());
		String location = response.getHeaderString("Location");
		String expected = ENDPOINT_ADDRESS + "/objectTemplates/" + USER_TEMPLATE_OID;
		assertEquals("Unexpected location, expected: " + expected + " but was " + location, 
				expected,
				location);
		
		IntegrationTestTools.display("Audit", dummyAuditService);
		dummyAuditService.assertRecords(4);
		dummyAuditService.assertLoginLogout(SchemaConstants.CHANNEL_REST_URI);
		dummyAuditService.assertHasDelta(1, ChangeType.ADD, ObjectTemplateType.class);

	}

	private WebClient prepareClient() {
		return prepareClient(USER_ADMINISTRATOR_USERNAME, USER_ADMINISTRATOR_PASSWORD);
	}
	
	private WebClient prepareClient(String username, String password) {
		
		List providers = new ArrayList<>();
		providers.add(getProvider());
		WebClient client = WebClient.create(ENDPOINT_ADDRESS, providers);//, provider);

		ClientConfiguration clientConfig = WebClient.getConfig(client);

		clientConfig.getRequestContext().put(LocalConduit.DIRECT_DISPATCH, Boolean.TRUE);
		
		client.accept(getAcceptHeader());
		client.type(getContentType());

		if (username != null) {
			String authorizationHeader = "Basic "
					+ org.apache.cxf.common.util.Base64Utility.encode((username+":"+(password==null?"":password)).getBytes());
			client.header("Authorization", authorizationHeader);
		}
		return client;

	}
	
	private void assertStatus(Response response, int expStatus) {
		assertEquals("Expected "+expStatus+" but got " + response.getStatus(), expStatus, response.getStatus());
	}

	private void assertNoEmptyResponse(Response response) {
		String respBody = response.readEntity(String.class);
		assertTrue("Unexpected reposponse: "+respBody, StringUtils.isBlank(respBody));
	}
	
	private void displayResponse(Response response) {
		LOGGER.info("response : {} ", response.getStatus());
		LOGGER.info("response : {} ", response.getStatusInfo().getReasonPhrase());
	}
	
	
	
}
