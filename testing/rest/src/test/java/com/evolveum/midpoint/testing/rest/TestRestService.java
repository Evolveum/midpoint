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

import com.evolveum.midpoint.common.monitor.InternalMonitor;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.provisioning.impl.ProvisioningServiceImpl;
import com.evolveum.midpoint.repo.api.RepoAddOptions;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.sql.SqlRepositoryServiceImpl;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

@ContextConfiguration(locations = { "classpath:ctx-rest-test.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestRestService {

	private static final File REPO_DIR = new File("src/test/resources/repo/");

	public static final File USER_ADMINISTRATOR_FILE = new File(REPO_DIR, "user-administrator.xml");
	public static final File ROLE_SUPERUSER_FILE = new File(REPO_DIR, "role-superuser.xml");

	public static final File RESOURCE_OPENDJ_FILE = new File(REPO_DIR, "reosurce-opendj.xml");
	public static final String RESOURCE_OPENDJ_OID = "ef2bc95b-76e0-59e2-86d6-3d4f02d3ffff";

	public static final File USER_TEMPLATE_FILE = new File(REPO_DIR, "user-template.xml");
	public static final String USER_TEMPLATE_OID = "c0c010c0-d34d-b33f-f00d-777111111111";

	public static final File ACCOUT_CHUCK_FILE = new File(REPO_DIR, "account-chuck.xml");
	public static final String ACCOUT_CHUCK_OID = REPO_DIR + "a0c010c0-d34d-b33f-f00d-111111111666";

	public static final File SYSTEM_CONFIGURATION_FILE = new File(REPO_DIR, "system-configuration.xml");

	private static final Trace LOGGER = TraceManager.getTrace(TestRestService.class);

	private final static String ENDPOINT_ADDRESS = "http://localhost:8080/rest";

	private static TaskManager taskManager;
	private static ModelService modelService;

	private static Server server;

	private static RepositoryService repositoryService;
	private static ProvisioningService provisioning;

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

		PrismContext prismContext = (PrismContext) applicationContext.getBean("prismContext");
		PrismObject<RoleType> superuser = prismContext.parseObject(ROLE_SUPERUSER_FILE);
		repositoryService.addObject(superuser, null, result);
		PrismObject<UserType> admin = prismContext.parseObject(USER_ADMINISTRATOR_FILE);
		repositoryService.addObject(admin, RepoAddOptions.createAllowUnencryptedValues(), result);

		PrismObject<UserType> sysConfig = prismContext.parseObject(SYSTEM_CONFIGURATION_FILE);
		repositoryService.addObject(sysConfig, RepoAddOptions.createAllowUnencryptedValues(), result);

		InternalMonitor.reset();

		modelService.postInit(result);

		result.computeStatus();
		TestUtil.assertSuccessOrWarning("startServer failed (result)", result, 1);
	}

	@AfterClass
	public static void destroy() throws Exception {
		server.stop();
		server.destroy();
	}

	public TestRestService() {
		super();
	}

	// TODO: authenticate as user that: has no authorization
	// TODO: authenticate as user that: has only authorization for REST
	// TODO: authenticate as user that: has only authorization for some REST operations
	
	// TODO: check audit
	
	@Test
	public void test001GetUserAdministrator() {
		final String TEST_NAME = "test001GetUserAdministrator";
		displayTestTile(this, TEST_NAME);

		WebClient client = prepareClient(true);

		client.path("/users/" + SystemObjectsType.USER_ADMINISTRATOR.value());

		TestUtil.displayWhen(TEST_NAME);
		Response response = client.get();

		TestUtil.displayThen(TEST_NAME);
		assertEquals("Expected 200 but got " + response.getStatus(), 200, response.getStatus());
		UserType userType = response.readEntity(UserType.class);
		assertNotNull("Returned entity in body must not be null.", userType);
		LOGGER.info("Returned entity: {}", userType.asPrismObject().debugDump());
	}

	@Test
	public void test002GetNonExistingUser() {
		final String TEST_NAME = "test002GetNonExistingUser";
		displayTestTile(this, TEST_NAME);

		WebClient client = prepareClient(true);

		client.path("/users/12345");

		TestUtil.displayWhen(TEST_NAME);
		Response response = client.get();

		TestUtil.displayThen(TEST_NAME);
		assertEquals("Expected 404 but got " + response.getStatus(), 404, response.getStatus());

	}

	@Test
	public void test003GetNonAuthneticated() {
		final String TEST_NAME = "test003GetNonAuthneticated";
		displayTestTile(this, TEST_NAME);

		WebClient client = prepareClient(false);

		client.path("/users/12345");

		TestUtil.displayWhen(TEST_NAME);
		Response response = client.get();

		TestUtil.displayThen(TEST_NAME);
		assertEquals("Expected 401 but got " + response.getStatus(), 401, response.getStatus());

	}

	@Test
	public void test102AddUserTemplate() throws Exception {
		final String TEST_NAME = "test102AddUserTemplate";
		displayTestTile(this, TEST_NAME);

		WebClient client = prepareClient(true);

		client.path("/objectTemplates");

		TestUtil.displayWhen(TEST_NAME);
		Response response = client.post(USER_TEMPLATE_FILE);

		TestUtil.displayThen(TEST_NAME);
		LOGGER.info("response : {} ", response.getStatus());
		LOGGER.info("response : {} ", response.getStatusInfo().getReasonPhrase());

		assertEquals("Expected 201 but got " + response.getStatus(), 201, response.getStatus());

	}

	@Test
	public void test103AddUserBadTargetCollection() throws Exception {
		final String TEST_NAME = "test103AddUserBadTargetCollection";
		displayTestTile(this, TEST_NAME);

		WebClient client = prepareClient(true);

		client.path("/objectTemplates");

		TestUtil.displayWhen(TEST_NAME);
		Response response = client.post(USER_ADMINISTRATOR_FILE);

		TestUtil.displayThen(TEST_NAME);
		LOGGER.info("response : {} ", response.getStatus());
		LOGGER.info("response : {} ", response.getStatusInfo().getReasonPhrase());

		assertEquals("Expected 400 but got " + response.getStatus(), 400, response.getStatus());

	}

	@Test
	public void test104AddAccountRaw() throws Exception {
		final String TEST_NAME = "test104AddAccountRaw";
		displayTestTile(this, TEST_NAME);

		WebClient client = prepareClient(true);

		client.path("/shadows");
		client.query("options", "raw");

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

	}

	@Test
	public void test401AddSystemConfigurationOverwrite() throws Exception {
		final String TEST_NAME = "test401AddSystemConfigurationOverwrite";
		displayTestTile(this, TEST_NAME);

		WebClient client = prepareClient(true);
		client.path("/systemConfigurations");

		client.query("options", "overwrite");

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

	}

	private WebClient prepareClient(boolean authenticate) {
		WebClient client = WebClient.create(ENDPOINT_ADDRESS);

		ClientConfiguration clientConfig = WebClient.getConfig(client);

		clientConfig.getRequestContext().put(LocalConduit.DIRECT_DISPATCH, Boolean.TRUE);

		client.accept("application/xml");

		if (authenticate) {
			String authorizationHeader = "Basic "
					+ org.apache.cxf.common.util.Base64Utility.encode("administrator:5ecr3t".getBytes());
			client.header("Authorization", authorizationHeader);
		}
		return client;

	}
}
