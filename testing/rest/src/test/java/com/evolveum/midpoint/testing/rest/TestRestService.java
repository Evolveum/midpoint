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
import static org.testng.AssertJUnit.fail;

import java.io.File;
import java.io.IOException;

import javax.ws.rs.core.Response;

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
import org.testng.AssertJUnit;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.evolveum.midpoint.common.InternalsConfig;
import com.evolveum.midpoint.common.monitor.InternalMonitor;
import com.evolveum.midpoint.model.api.ModelService;
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
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.test.DummyAuditService;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTemplateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

@ContextConfiguration(locations = { "classpath:ctx-rest-test.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestRestService {

	private static final File REPO_DIR = new File("src/test/resources/repo/");

	public static final File USER_ADMINISTRATOR_FILE = new File(REPO_DIR, "user-administrator.xml");
	public static final String USER_ADMINISTRATOR_USERNAME = "administrator";
	public static final String USER_ADMINISTRATOR_PASSWORD = "5ecr3t";

	// No authorization
	public static final File USER_NOBODY_FILE = new File(REPO_DIR, "user-nobody.xml");
	public static final String USER_NOBODY_USERNAME = "nobody";
	public static final String USER_NOBODY_PASSWORD = "nopassword";

	// REST authorization only
	public static final File USER_CYCLOPS_FILE = new File(REPO_DIR, "user-cyclops.xml");
	public static final String USER_CYCLOPS_USERNAME = "cyclops";
	public static final String USER_CYCLOPS_PASSWORD = "cyclopassword";
	
	// REST and reader authorization
	public static final File USER_SOMEBODY_FILE = new File(REPO_DIR, "user-somebody.xml");
	public static final String USER_SOMEBODY_USERNAME = "somebody";
	public static final String USER_SOMEBODY_PASSWORD = "somepassword";

	public static final File ROLE_SUPERUSER_FILE = new File(REPO_DIR, "role-superuser.xml");
	public static final File ROLE_REST_FILE = new File(REPO_DIR, "role-rest.xml");
	public static final File ROLE_READER_FILE = new File(REPO_DIR, "role-reader.xml");

	public static final File RESOURCE_OPENDJ_FILE = new File(REPO_DIR, "reosurce-opendj.xml");
	public static final String RESOURCE_OPENDJ_OID = "ef2bc95b-76e0-59e2-86d6-3d4f02d3ffff";

	public static final File USER_TEMPLATE_FILE = new File(REPO_DIR, "user-template.xml");
	public static final String USER_TEMPLATE_OID = "c0c010c0-d34d-b33f-f00d-777111111111";

	public static final File ACCOUT_CHUCK_FILE = new File(REPO_DIR, "account-chuck.xml");
	public static final String ACCOUT_CHUCK_OID = REPO_DIR + "a0c010c0-d34d-b33f-f00d-111111111666";

	public static final File SYSTEM_CONFIGURATION_FILE = new File(REPO_DIR, "system-configuration.xml");

	private static final Trace LOGGER = TraceManager.getTrace(TestRestService.class);

	private final static String ENDPOINT_ADDRESS = "http://localhost:18080/rest";

	private static PrismContext prismContext;
	private static TaskManager taskManager;
	private static ModelService modelService;

	private static Server server;

	private static RepositoryService repositoryService;
	private static ProvisioningService provisioning;
	private static DummyAuditService dummyAuditService;

	@BeforeClass
	public static void initialize() throws Exception {
		startServer();
	}

	private static void startServer() throws Exception {
		ApplicationContext applicationContext = new ClassPathXmlApplicationContext("ctx-rest-test-main.xml");
		LOGGER.info("Spring context initialized.");

		JAXRSServerFactoryBean sf = (JAXRSServerFactoryBean) applicationContext.getBean("restService");

		sf.setAddress(ENDPOINT_ADDRESS);

		server = sf.create();

		repositoryService = (SqlRepositoryServiceImpl) applicationContext.getBean("repositoryService");
		provisioning = (ProvisioningServiceImpl) applicationContext.getBean("provisioningService");
		taskManager = (TaskManager) applicationContext.getBean("taskManager");
		modelService = (ModelService) applicationContext.getBean("modelController");

		Task initTask = taskManager.createTaskInstance(TestRestService.class.getName() + ".startServer");
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
	
	private static <O extends ObjectType> PrismObject<O> addObject(File file, OperationResult result) throws SchemaException, IOException, ObjectAlreadyExistsException {
		PrismObject<O> object = prismContext.parseObject(file);
		String oid = repositoryService.addObject(object, null, result);
		object.setOid(oid);
		return object;
	}

	@AfterClass
	public static void destroy() throws Exception {
		server.stop();
		server.destroy();
	}

	public TestRestService() {
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
		assertEquals("Expected 200 but got " + response.getStatus(), 200, response.getStatus());
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
		assertEquals("Expected 404 but got " + response.getStatus(), 404, response.getStatus());

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
		assertEquals("Expected 401 but got " + response.getStatus(), 401, response.getStatus());

		IntegrationTestTools.display("Audit", dummyAuditService);
		// No records. There are no auth headers so this is not considered to be a login attempt
		dummyAuditService.assertRecords(0);
	}

	@Test
	public void test004GetAuthBadUsername() {
		final String TEST_NAME = "test004GetAuthBadUsername";
		displayTestTile(this, TEST_NAME);

		WebClient client = prepareClient("NoSUCHuser", null);
		client.path("/users/" + SystemObjectsType.USER_ADMINISTRATOR.value());
		
		dummyAuditService.clear();

		TestUtil.displayWhen(TEST_NAME);
		Response response = client.get();

		TestUtil.displayThen(TEST_NAME);
		assertEquals("Expected 401 but got " + response.getStatus(), 401, response.getStatus());

		IntegrationTestTools.display("Audit", dummyAuditService);
		dummyAuditService.assertRecords(1);
		dummyAuditService.assertFailedLogin(SchemaConstants.CHANNEL_REST_URI);
	}
	
	@Test
	public void test005GetAuthNoPassword() {
		final String TEST_NAME = "test005GetAuthNoPassword";
		displayTestTile(this, TEST_NAME);

		WebClient client = prepareClient(USER_ADMINISTRATOR_USERNAME, null);
		client.path("/users/" + SystemObjectsType.USER_ADMINISTRATOR.value());
		
		dummyAuditService.clear();

		TestUtil.displayWhen(TEST_NAME);
		Response response = client.get();

		TestUtil.displayThen(TEST_NAME);
		assertEquals("Expected 401 but got " + response.getStatus(), 401, response.getStatus());

		IntegrationTestTools.display("Audit", dummyAuditService);
		dummyAuditService.assertRecords(1);
		dummyAuditService.assertFailedLogin(SchemaConstants.CHANNEL_REST_URI);
	}

	@Test
	public void test006GetAuthBadPassword() {
		final String TEST_NAME = "test006GetAuthBadPassword";
		displayTestTile(this, TEST_NAME);

		WebClient client = prepareClient(USER_ADMINISTRATOR_USERNAME, "forgot");
		client.path("/users/" + SystemObjectsType.USER_ADMINISTRATOR.value());
		
		dummyAuditService.clear();

		TestUtil.displayWhen(TEST_NAME);
		Response response = client.get();

		TestUtil.displayThen(TEST_NAME);
		assertEquals("Expected 401 but got " + response.getStatus(), 401, response.getStatus());

		IntegrationTestTools.display("Audit", dummyAuditService);
		dummyAuditService.assertRecords(1);
		dummyAuditService.assertFailedLogin(SchemaConstants.CHANNEL_REST_URI);
	}
	
	@Test
	public void test007GetUnauthorizedUser() {
		final String TEST_NAME = "test007GetUnauthorizedUser";
		displayTestTile(this, TEST_NAME);

		WebClient client = prepareClient(USER_NOBODY_USERNAME, USER_NOBODY_PASSWORD);
		client.path("/users/" + SystemObjectsType.USER_ADMINISTRATOR.value());
		
		dummyAuditService.clear();

		TestUtil.displayWhen(TEST_NAME);
		Response response = client.get();

		TestUtil.displayThen(TEST_NAME);
		assertEquals("Expected 403 but got " + response.getStatus(), 403, response.getStatus());

		IntegrationTestTools.display("Audit", dummyAuditService);
		dummyAuditService.assertRecords(1);
		dummyAuditService.assertFailedLogin(SchemaConstants.CHANNEL_REST_URI);
	}
	
	@Test
	public void test008GetUserAdministratorByCyclops() {
		final String TEST_NAME = "test008GetUserAdministratorByCyclops";
		displayTestTile(this, TEST_NAME);

		WebClient client = prepareClient(USER_CYCLOPS_USERNAME, USER_CYCLOPS_PASSWORD);
		client.path("/users/" + SystemObjectsType.USER_ADMINISTRATOR.value());
		
		dummyAuditService.clear();

		TestUtil.displayWhen(TEST_NAME);
		Response response = client.get();

		TestUtil.displayThen(TEST_NAME);
		assertEquals("Expected 403 but got " + response.getStatus(), 403, response.getStatus());

		IntegrationTestTools.display("Audit", dummyAuditService);
		dummyAuditService.assertRecords(2);
		dummyAuditService.assertLoginLogout(SchemaConstants.CHANNEL_REST_URI);
	}
	
	@Test
	public void test009GetUserAdministratorBySomebody() {
		final String TEST_NAME = "test009GetUserAdministratorBySomebody";
		displayTestTile(this, TEST_NAME);

		WebClient client = prepareClient(USER_SOMEBODY_USERNAME, USER_SOMEBODY_PASSWORD);
		client.path("/users/" + SystemObjectsType.USER_ADMINISTRATOR.value());
		
		dummyAuditService.clear();

		TestUtil.displayWhen(TEST_NAME);
		Response response = client.get();

		TestUtil.displayThen(TEST_NAME);
		assertEquals("Expected 200 but got " + response.getStatus(), 200, response.getStatus());
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
		client.path("/objectTemplates");
		
		dummyAuditService.clear();

		TestUtil.displayWhen(TEST_NAME);
		Response response = client.post(USER_TEMPLATE_FILE);

		TestUtil.displayThen(TEST_NAME);
		LOGGER.info("response : {} ", response.getStatus());
		LOGGER.info("response : {} ", response.getStatusInfo().getReasonPhrase());

		assertEquals("Expected 201 but got " + response.getStatus(), 201, response.getStatus());

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
		Response response = client.post(USER_ADMINISTRATOR_FILE);

		TestUtil.displayThen(TEST_NAME);
		LOGGER.info("response : {} ", response.getStatus());
		LOGGER.info("response : {} ", response.getStatusInfo().getReasonPhrase());

		assertEquals("Expected 400 but got " + response.getStatus(), 400, response.getStatus());

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
		Response response = client.post(ACCOUT_CHUCK_FILE);

		TestUtil.displayThen(TEST_NAME);
		LOGGER.info("response : {} ", response.getStatus());
		LOGGER.info("response : {} ", response.getStatusInfo().getReasonPhrase());

		assertEquals("Expected 201 but got " + response.getStatus(), 201, response.getStatus());

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
	public void test401AddSystemConfigurationOverwrite() throws Exception {
		final String TEST_NAME = "test401AddSystemConfigurationOverwrite";
		displayTestTile(this, TEST_NAME);

		WebClient client = prepareClient();
		client.path("/systemConfigurations");
		client.query("options", "overwrite");
		
		dummyAuditService.clear();

		TestUtil.displayWhen(TEST_NAME);
		Response response = client.post(SYSTEM_CONFIGURATION_FILE);

		TestUtil.displayThen(TEST_NAME);
		LOGGER.info("response : {} ", response.getStatus());
		LOGGER.info("response : {} ", response.getStatusInfo().getReasonPhrase());

		assertEquals("Expected 201 but got " + response.getStatus(), 201, response.getStatus());
		String location = response.getHeaderString("Location");
		assertEquals(
				ENDPOINT_ADDRESS + "/systemConfigurations/" + SystemObjectsType.SYSTEM_CONFIGURATION.value(),
				location);
		
		IntegrationTestTools.display("Audit", dummyAuditService);
		dummyAuditService.assertRecords(4);
		dummyAuditService.assertLoginLogout(SchemaConstants.CHANNEL_REST_URI);
		dummyAuditService.assertHasDelta(1, ChangeType.ADD, SystemConfigurationType.class);

	}

	private WebClient prepareClient() {
		return prepareClient(USER_ADMINISTRATOR_USERNAME, USER_ADMINISTRATOR_PASSWORD);
	}
	
	private WebClient prepareClient(String username, String password) {
		WebClient client = WebClient.create(ENDPOINT_ADDRESS);

		ClientConfiguration clientConfig = WebClient.getConfig(client);

		clientConfig.getRequestContext().put(LocalConduit.DIRECT_DISPATCH, Boolean.TRUE);

		client.accept("application/xml");

		if (username != null) {
			String authorizationHeader = "Basic "
					+ org.apache.cxf.common.util.Base64Utility.encode((username+":"+(password==null?"":password)).getBytes());
			client.header("Authorization", authorizationHeader);
		}
		return client;

	}
}
