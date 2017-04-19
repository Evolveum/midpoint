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

import static com.evolveum.midpoint.test.util.TestUtil.displayTestTile;
import static org.testng.AssertJUnit.assertNotNull;

import java.io.File;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.impl.rest.MidpointAbstractProvider;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

public class TestRestServiceProxyAuthentication extends RestServiceInitializer {
	
	private static final Trace LOGGER = TraceManager.getTrace(TestRestServiceProxyAuthentication.class);

	// REST and end user authorization
	public static final File USER_EGOIST_FILE = new File(BASE_REPO_DIR, "user-egoist.xml");
	public static final String USER_EGOIST_OID = "b6f3e3c8-d48b-11e4-8d88-001e8c717e5b";
	public static final String USER_EGOIST_USERNAME = "egoist";
	public static final String USER_EGOIST_PASSWORD = "onlyMypassw0rd";

	// REST and full authorization but not switchable
	public static final File USER_HEAD_FILE = new File(BASE_REPO_DIR, "user-head.xml");
	public static final String USER_HEAD_OID = "c7f3e3c8-d48b-11e4-8d88-001e8c717e5b";
	public static final String USER_HEAD_USERNAME = "head";
	public static final String USER_HEAD_PASSWORD = "headPassw0rd";

	public static final File ROLE_PROXY_FILE = new File(BASE_REPO_DIR, "role-proxy.xml");
	
	// REST and end user authorization
		public static final File USER_PROXY_FILE = new File(BASE_REPO_DIR, "user-proxy.xml");
		public static final String USER_PROXY_OID = "d8f3e3c8-d48b-11e4-8d88-001e8c717e5b";
		public static final String USER_PROXY_USERNAME = "proxy";
		public static final String USER_PROXY_PASSWORD = "proxyPassword";
	
	@Override
	public void startServer() throws Exception {
		// TODO Auto-generated method stub
		super.startServer();
		
		OperationResult result = new OperationResult("Init config");
		addObject(ROLE_PROXY_FILE, result);
		addObject(USER_EGOIST_FILE, result);
		addObject(USER_HEAD_FILE, result);
		addObject(USER_PROXY_FILE, result);
		
		InternalMonitor.reset();
	}
				
	@Test
	public void test001getUserSelfBySomebody() {
		final String TEST_NAME = "test001getUserSelfBySomebody";
		displayTestTile(this, TEST_NAME);

		WebClient client = prepareClient(USER_SOMEBODY_OID);
		client.path("/self/");
		
		getDummyAuditService().clear();

		TestUtil.displayWhen(TEST_NAME);
		Response response = client.get();
		
		TestUtil.displayThen(TEST_NAME);
		assertStatus(response, 200);
		UserType userType = response.readEntity(UserType.class);
		assertNotNull("Returned entity in body must not be null.", userType);
		LOGGER.info("Returned entity: {}", userType.asPrismObject().debugDump());
		
		IntegrationTestTools.display("Audit", getDummyAuditService());
		getDummyAuditService().assertRecords(2);
		getDummyAuditService().assertLoginLogout(SchemaConstants.CHANNEL_REST_URI);
	}
	
	@Test
	public void test002getUserSelfByEgoist() {
		final String TEST_NAME = "test002getUserSelfByEgoist";
		displayTestTile(this, TEST_NAME);

		WebClient client = prepareClient(USER_EGOIST_OID);
		client.path("/self/");
		
		getDummyAuditService().clear();

		TestUtil.displayWhen(TEST_NAME);
		Response response = client.get();
		
		TestUtil.displayThen(TEST_NAME);
		assertStatus(response, 200);
		UserType userType = response.readEntity(UserType.class);
		assertNotNull("Returned entity in body must not be null.", userType);
		LOGGER.info("Returned entity: {}", userType.asPrismObject().debugDump());
		
		IntegrationTestTools.display("Audit", getDummyAuditService());
		getDummyAuditService().assertRecords(2);
		getDummyAuditService().assertLoginLogout(SchemaConstants.CHANNEL_REST_URI);
	}
	
	/**
	 * egoist doesn't have authorization to read other object. ot has only end user role, 
	 * so he is allowed to performed defined actions on his own.
	 */
	@Test
	public void test003getUserAdministratorByEgoist() {
		final String TEST_NAME = "test003getUserAdministratorByEgoist";
		displayTestTile(this, TEST_NAME);

		WebClient client = prepareClient(USER_EGOIST_OID);
		client.path("/users/" + SystemObjectsType.USER_ADMINISTRATOR.value());
		
		getDummyAuditService().clear();

		TestUtil.displayWhen(TEST_NAME);
		Response response = client.get();
		
		TestUtil.displayThen(TEST_NAME);
		assertStatus(response, 403);
		
		IntegrationTestTools.display("Audit", getDummyAuditService());
		getDummyAuditService().assertRecords(2);
		getDummyAuditService().assertLoginLogout(SchemaConstants.CHANNEL_REST_URI);
	}
	
	/**
	 * user head is a super user and has also rest authorization so he can perform any action
	 */
	@Test
	public void test004getUserSelfByHead() {
		final String TEST_NAME = "test004getUserSelfByHead";
		displayTestTile(this, TEST_NAME);

		WebClient client = prepareClient(null);
		client.path("/self");
		
		getDummyAuditService().clear();

		TestUtil.displayWhen(TEST_NAME);
		Response response = client.get();
		
		TestUtil.displayThen(TEST_NAME);
		assertStatus(response, 200);
		UserType userType = response.readEntity(UserType.class);
		assertNotNull("Returned entity in body must not be null.", userType);
		LOGGER.info("Returned entity: {}", userType.asPrismObject().debugDump());
		
		IntegrationTestTools.display("Audit", getDummyAuditService());
		getDummyAuditService().assertRecords(2);
		getDummyAuditService().assertLoginLogout(SchemaConstants.CHANNEL_REST_URI);
	}
	
	/**
	 * even though head is a superuser, it is not allowed for service application to switch to this user,
	 * therefore head is not allowed to read user administrator using inpersonation
	 */
	@Test
	public void test005getUserSelfByProxyHead() {
		final String TEST_NAME = "test005getUserSelfByProxyHead";
		displayTestTile(this, TEST_NAME);

		WebClient client = prepareClient(USER_HEAD_OID);
		client.path("/self");
		
		getDummyAuditService().clear();

		TestUtil.displayWhen(TEST_NAME);
		Response response = client.get();
		
		TestUtil.displayThen(TEST_NAME);
		assertStatus(response, 403);
		
		IntegrationTestTools.display("Audit", getDummyAuditService());
		getDummyAuditService().assertRecords(2);
		getDummyAuditService().assertFailedProxyLogin(SchemaConstants.CHANNEL_REST_URI);
	}

	@Override
	protected String getAcceptHeader() {
		return MediaType.APPLICATION_XML;
	}

	@Override
	protected String getContentType() {
		return MediaType.APPLICATION_XML;
	}

	@Override
	protected MidpointAbstractProvider getProvider() {
		return xmlProvider;
	}
	
	private WebClient prepareClient(String proxyUserOid) {
		WebClient client = prepareClient("proxy", "proxyPassword");
		if (StringUtils.isNotBlank(proxyUserOid)){
			client.header("Switch-To-Principal", proxyUserOid);
		}
		return client;
	}
	
	
}
