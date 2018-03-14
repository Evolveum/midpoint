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
package com.evolveum.midpoint.testing.rest;

import static org.testng.AssertJUnit.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.impl.rest.MidpointAbstractProvider;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.provisioning.impl.ProvisioningServiceImpl;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.sql.SqlRepositoryServiceImpl;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.test.DummyAuditService;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;


@ContextConfiguration(locations = { "classpath:ctx-rest-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public abstract class RestServiceInitializer {

	private static final Trace LOGGER = TraceManager.getTrace(RestServiceInitializer.class);

	protected static final File BASE_REPO_DIR = new File("src/test/resources/repo/");

	public static final File USER_ADMINISTRATOR_FILE = new File(BASE_REPO_DIR, "user-administrator.xml");
	public static final String USER_ADMINISTRATOR_USERNAME = "administrator";
	public static final String USER_ADMINISTRATOR_PASSWORD = "5ecr3t";

	// No authorization
	public static final File USER_NOBODY_FILE = new File(BASE_REPO_DIR, "user-nobody.xml");
	public static final String USER_NOBODY_OID = "ffb9729c-d48b-11e4-9720-001e8c717e5b";
	public static final String USER_NOBODY_USERNAME = "nobody";
	public static final String USER_NOBODY_PASSWORD = "nopassword";

	// REST authorization only
	public static final File USER_CYCLOPS_FILE = new File(BASE_REPO_DIR, "user-cyclops.xml");
	public static final String USER_CYCLOPS_OID = "6020bb52-d48e-11e4-9eaf-001e8c717e5b";
	public static final String USER_CYCLOPS_USERNAME = "cyclops";
	public static final String USER_CYCLOPS_PASSWORD = "cyclopassword";

	// REST and reader authorization
	public static final File USER_SOMEBODY_FILE = new File(BASE_REPO_DIR, "user-somebody.xml");
	public static final String USER_SOMEBODY_OID = "a5f3e3c8-d48b-11e4-8d88-001e8c717e5b";
	public static final String USER_SOMEBODY_USERNAME = "somebody";
	public static final String USER_SOMEBODY_PASSWORD = "somepassword";

	// other
	public static final File USER_JACK_FILE = new File(BASE_REPO_DIR, "user-jack.xml");
	public static final String USER_JACK_OID = "229487cb-59b6-490b-879d-7a6d925dd08c";

	public static final File ROLE_SUPERUSER_FILE = new File(BASE_REPO_DIR, "role-superuser.xml");
	public static final File ROLE_ENDUSER_FILE = new File(BASE_REPO_DIR, "role-enduser.xml");
	public static final File ROLE_REST_FILE = new File(BASE_REPO_DIR, "role-rest.xml");
	public static final File ROLE_READER_FILE = new File(BASE_REPO_DIR, "role-reader.xml");

	public static final File SYSTEM_CONFIGURATION_FILE = new File(BASE_REPO_DIR, "system-configuration.xml");

	public static final File VALUE_POLICY_GENERAL = new File(BASE_REPO_DIR, "value-policy-general.xml");
	public static final File VALUE_POLICY_NUMERIC = new File(BASE_REPO_DIR, "value-policy-numeric.xml");
	public static final File VALUE_POLICY_SIMPLE = new File(BASE_REPO_DIR, "value-policy-simple.xml");
	public static final File SECURITY_POLICY = new File(BASE_REPO_DIR, "security-policy.xml");

	ApplicationContext applicationContext = null;

	private PrismContext prismContext;
	private TaskManager taskManager;
	private ModelService modelService;

	private Server server;

	private RepositoryService repositoryService;
	private ProvisioningService provisioning;
	protected DummyAuditService dummyAuditService;

	protected TestXmlProvider xmlProvider;
	protected TestJsonProvider jsonProvider;
	protected TestYamlProvider yamlProvider;

	protected abstract String getAcceptHeader();
	protected abstract String getContentType();
	protected abstract MidpointAbstractProvider getProvider();

	protected final static String ENDPOINT_ADDRESS = "http://localhost:18080/rest";

	@BeforeClass
	public void initialize() throws Exception {
		startServer();
	}

	@AfterClass
	public void shutDown() {
		((ClassPathXmlApplicationContext) applicationContext).close();
	}

	public void startServer() throws Exception {
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

		InternalsConfig.encryptionChecks = false;

		prismContext = (PrismContext) applicationContext.getBean("prismContext");

		Task initTask = getTaskManager().createTaskInstance(TestAbstractRestService.class.getName() + ".startServer");
		OperationResult result = initTask.getResult();

		addObject(ROLE_SUPERUSER_FILE, result);
		addObject(ROLE_ENDUSER_FILE, result);
		addObject(ROLE_REST_FILE, result);
		addObject(ROLE_READER_FILE, result);
		addObject(USER_ADMINISTRATOR_FILE, result);
		addObject(USER_NOBODY_FILE, result);
		addObject(USER_CYCLOPS_FILE, result);
		addObject(USER_SOMEBODY_FILE, result);
		addObject(USER_JACK_FILE, result);
		addObject(VALUE_POLICY_GENERAL, result);
		addObject(VALUE_POLICY_NUMERIC, result);
		addObject(VALUE_POLICY_SIMPLE, result);
		addObject(SECURITY_POLICY, result);
		addObject(SYSTEM_CONFIGURATION_FILE, result);

		dummyAuditService = getDummyAuditService().getInstance();

		InternalMonitor.reset();

		getModelService().postInit(result);

		result.computeStatus();
		TestUtil.assertSuccessOrWarning("startServer failed (result)", result, 1);

	}

	protected <O extends ObjectType> PrismObject<O> addObject(File file, OperationResult result) throws SchemaException, IOException, ObjectAlreadyExistsException {
		PrismObject<O> object = getPrismContext().parseObject(file);
		String oid = getRepositoryService().addObject(object, null, result);
		object.setOid(oid);
		return object;
	}

	protected WebClient prepareClient(String username, String password) {

		List providers = new ArrayList<>();
		providers.add(getProvider());
		WebClient client = WebClient.create(ENDPOINT_ADDRESS, providers);// ,
																			// provider);

		ClientConfiguration clientConfig = WebClient.getConfig(client);

		clientConfig.getRequestContext().put(LocalConduit.DIRECT_DISPATCH, Boolean.TRUE);

		client.accept(getAcceptHeader());
		client.type(getContentType());

		createAuthorizationHeader(client, username, password);
		return client;

	}

	protected void createAuthorizationHeader(WebClient client, String username, String password) {
		if (username != null) {
			String authorizationHeader = "Basic " + org.apache.cxf.common.util.Base64Utility
					.encode((username + ":" + (password == null ? "" : password)).getBytes());
			client.header("Authorization", authorizationHeader);
		}
	}

	protected void assertStatus(Response response, int expStatus) {
		assertEquals("Expected " + expStatus + " but got " + response.getStatus(), expStatus,
				response.getStatus());
	}

	public PrismContext getPrismContext() {
		return prismContext;
	}

	public TaskManager getTaskManager() {
		return taskManager;
	}

	public ModelService getModelService() {
		return modelService;
	}

	public Server getServer() {
		return server;
	}

	public RepositoryService getRepositoryService() {
		return repositoryService;
	}

	public ProvisioningService getProvisioning() {
		return provisioning;
	}

	public DummyAuditService getDummyAuditService() {
		return dummyAuditService;
	}

	public TestXmlProvider getXmlProvider() {
		return xmlProvider;
	}

	public TestJsonProvider getJsonProvider() {
		return jsonProvider;
	}

	public TestYamlProvider getYamlProvider() {
		return yamlProvider;
	}

}
