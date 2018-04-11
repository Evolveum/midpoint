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
package com.evolveum.midpoint.model.intest;

import com.evolveum.midpoint.notifications.api.events.CustomEvent;
import com.evolveum.midpoint.notifications.api.events.Event;
import com.evolveum.midpoint.notifications.api.transports.Message;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.LightweightIdentifierGenerator;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import javax.xml.datatype.XMLGregorianCalendar;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static java.util.Collections.singletonList;
import static org.testng.AssertJUnit.*;

/**
 * @author mederly
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestNotifications extends AbstractInitializedModelIntegrationTest {

	public static final File TEST_DIR = new File("src/test/resources/notifications");
	private static final File SYSTEM_CONFIGURATION_FILE = new File(TEST_DIR, "system-configuration.xml");

	@Autowired private LightweightIdentifierGenerator lightweightIdentifierGenerator;

	private String accountJackOid;
	private HttpServer httpServer;
	private MyHttpHandler httpHandler;

	@Override
	public void initSystem(Task initTask, OperationResult initResult)
			throws Exception {
		super.initSystem(initTask, initResult);
		InternalMonitor.reset();
	}

	@Override
	protected File getSystemConfigurationFile() {
		return null;
	}

	@Override
	protected void addSystemConfigurationObject(OperationResult initResult) throws IOException, CommonException,
			EncryptionException {
		List<String> configLines = IOUtils.readLines(new FileReader(SYSTEM_CONFIGURATION_FILE));
		String configString = StringUtils.join(configLines, '\n');
		int port = startHttpServer();
		configString = configString.replaceAll("\\$\\$port\\$\\$", Integer.toString(port));
		repoAddObject(prismContext.parseObject(configString), initResult);
	}

	private int startHttpServer() throws IOException {
		int freePort = findFreePort();
		httpServer = HttpServer.create(new InetSocketAddress(freePort), 0);
		httpHandler = new MyHttpHandler();
		httpServer.createContext("/send", httpHandler);
		httpServer.start();
		System.out.println("Embedded http server started at port " + freePort);
		return freePort;
	}

	@AfterClass
	public void stopHttpServer() {
		if (httpServer != null) {
			System.out.println("Stopping the embedded http server");
			httpServer.stop(0);
			httpServer = null;
		}
	}

	private int findFreePort() throws IOException {
		try (ServerSocket socket = new ServerSocket(0)) {
			socket.setReuseAddress(true);
			return socket.getLocalPort();
		}
	}

	@Test
	public void test100ModifyUserAddAccount() throws Exception {
		final String TEST_NAME = "test100ModifyUserAddAccount";
		TestUtil.displayTestTitle(this, TEST_NAME);

		// GIVEN
		Task task = taskManager.createTaskInstance(TestNotifications.class.getName() + ".test100ModifyUserAddAccount");
		task.setChannel(SchemaConstants.CHANNEL_GUI_USER_URI);
		OperationResult result = task.getResult();
		preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);

		XMLGregorianCalendar startTime = clock.currentTimeXMLGregorianCalendar();

		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		modifyUserAddAccount(USER_JACK_OID, ACCOUNT_JACK_DUMMY_FILE, task, result);

		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
		TestUtil.assertSuccess(result);
		XMLGregorianCalendar endTime = clock.currentTimeXMLGregorianCalendar();
		assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);

		// Check accountRef
		PrismObject<UserType> userJack = modelService.getObject(UserType.class, USER_JACK_OID, null, task, result);
		assertUserJack(userJack);
		UserType userJackType = userJack.asObjectable();
		assertEquals("Unexpected number of accountRefs", 1, userJackType.getLinkRef().size());
		ObjectReferenceType accountRefType = userJackType.getLinkRef().get(0);
		accountJackOid = accountRefType.getOid();
		assertFalse("No accountRef oid", StringUtils.isBlank(accountJackOid));
		PrismReferenceValue accountRefValue = accountRefType.asReferenceValue();
		assertEquals("OID mismatch in accountRefValue", accountJackOid, accountRefValue.getOid());
		assertNull("Unexpected object in accountRefValue", accountRefValue.getObject());

		// Check shadow
		PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountJackOid, null, result);
		assertDummyAccountShadowRepo(accountShadow, accountJackOid, "jack");
		assertEnableTimestampShadow(accountShadow, startTime, endTime);

		// Check account
		PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountJackOid, null, task, result);
		assertDummyAccountShadowModel(accountModel, accountJackOid, "jack", "Jack Sparrow");

		// Check account in dummy resource
		assertDefaultDummyAccount("jack", "Jack Sparrow", true);

		notificationManager.setDisabled(true);

		// Check notifications
		display("Dummy transport messages", dummyTransport);

		checkDummyTransportMessages("accountPasswordNotifier", 1);
		checkDummyTransportMessages("userPasswordNotifier", 0);
		checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 1);
		checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
		checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 1);
		checkDummyTransportMessages("simpleUserNotifier", 0);
		checkDummyTransportMessages("simpleUserNotifier-ADD", 0);

		List<Message> pwdMessages = dummyTransport.getMessages("dummy:accountPasswordNotifier");
		Message pwdMessage = pwdMessages.get(0);          // number of messages was already checked
		assertEquals("Invalid list of recipients", singletonList("recipient@evolveum.com"), pwdMessage.getTo());
		assertEquals("Wrong message body", "Password for account jack on Dummy Resource is: deadmentellnotales", pwdMessage.getBody());

		List<Message> addMessages = dummyTransport.getMessages("dummy:simpleAccountNotifier-ADD-SUCCESS");
		Message addMessage = addMessages.get(0);          // number of messages was already checked
		assertEquals("Invalid list of recipients", singletonList("recipient@evolveum.com"), addMessage.getTo());
		assertEquals("Wrong message body", "Notification about account-related operation\n"
				+ "\n"
				+ "Owner: Jack Sparrow (jack, oid c0c010c0-d34d-b33f-f00d-111111111111)\n"
				+ "Resource: Dummy Resource (oid 10000000-0000-0000-0000-000000000004)\n"
				+ "\n"
				+ "An account has been successfully created on the resource with attributes:\n"
				+ " - UID: jack\n"
				+ " - Username: jack\n"
				+ " - Location: Caribbean\n"
				+ " - Quote: Arr!\n"
				+ " - Drink: rum\n"
				+ " - Weapon: rum\n"
				+ " - Full Name: Jack Sparrow\n"
				+ " - Password:\n"
				+ "    - Value: (protected string)\n"
				+ " - Administrative status: ENABLED\n"
				+ "\n"
				+ "Channel: http://midpoint.evolveum.com/xml/ns/public/gui/channels-3#user", addMessage.getBody());

		assertSteadyResources();
	}

	@Test
    public void test119ModifyUserDeleteAccount() throws Exception {
		final String TEST_NAME = "test119ModifyUserDeleteAccount";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestNotifications.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);

        PrismObject<ShadowType> account = PrismTestUtil.parseObject(ACCOUNT_JACK_DUMMY_FILE);
        account.setOid(accountJackOid);

		ObjectDelta<UserType> userDelta = ObjectDelta.createEmptyModifyDelta(UserType.class, USER_JACK_OID, prismContext);
		ReferenceDelta accountDelta = ReferenceDelta.createModificationDelete(UserType.F_LINK_REF, getUserDefinition(), account);
		userDelta.addModification(accountDelta);
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);

        // WHEN
		TestUtil.displayWhen(TEST_NAME);
		modelService.executeChanges(deltas, null, task, result);

		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
        TestUtil.assertSuccess("executeChanges result", result, 2);
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);

		// Check accountRef
		PrismObject<UserType> userJack = modelService.getObject(UserType.class, USER_JACK_OID, null, task, result);
        assertUserJack(userJack);
        UserType userJackType = userJack.asObjectable();
        assertEquals("Unexpected number of linkRefs", 0, userJackType.getLinkRef().size());

		// Check is shadow is gone
        try {
        	repositoryService.getObject(ShadowType.class, accountJackOid, null, result);
        	AssertJUnit.fail("Shadow "+accountJackOid+" still exists");
        } catch (ObjectNotFoundException e) {
        	// This is OK
        }

        // Check if dummy resource account is gone
        assertNoDummyAccount("jack");

        // Check notifications
		display("Notifications", dummyTransport);

        notificationManager.setDisabled(true);
        checkDummyTransportMessages("accountPasswordNotifier", 0);
        checkDummyTransportMessages("userPasswordNotifier", 0);
        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 1);
        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 0);
        checkDummyTransportMessages("simpleAccountNotifier-DELETE-SUCCESS", 1);
        checkDummyTransportMessages("simpleUserNotifier", 0);
        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);

		String expected = "Notification about account-related operation\n"
				+ "\n"
				+ "Owner: Jack Sparrow (jack, oid c0c010c0-d34d-b33f-f00d-111111111111)\n"
				+ "Resource: Dummy Resource (oid 10000000-0000-0000-0000-000000000004)\n"
				+ "Account: jack\n"
				+ "\n"
				+ "The account has been successfully removed from the resource.\n"
				+ "\n"
				+ "Channel: ";
		assertEquals("Wrong message body", expected, dummyTransport.getMessages("dummy:simpleAccountNotifier-DELETE-SUCCESS").get(0).getBody());

        assertSteadyResources();
    }

	@Test
    public void test131ModifyUserJackAssignAccount() throws Exception {
		final String TEST_NAME = "test131ModifyUserJackAssignAccount";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestNotifications.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.FULL);

        XMLGregorianCalendar startTime = clock.currentTimeXMLGregorianCalendar();

		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        assignAccount(USER_JACK_OID, RESOURCE_DUMMY_OID, null, task, result);

		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
        TestUtil.assertSuccess("executeChanges result", result);
        XMLGregorianCalendar endTime = clock.currentTimeXMLGregorianCalendar();
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);

		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack);
		assertAssignedAccount(userJack, RESOURCE_DUMMY_OID);
        assertAssignments(userJack, 1);

        accountJackOid = getSingleLinkOid(userJack);

		// Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountJackOid, null, result);
        assertDummyAccountShadowRepo(accountShadow, accountJackOid, "jack");
        assertEnableTimestampShadow(accountShadow, startTime, endTime);

        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountJackOid, null, task, result);
        assertDummyAccountShadowModel(accountModel, accountJackOid, "jack", "Jack Sparrow");
        assertEnableTimestampShadow(accountModel, startTime, endTime);

        // Check account in dummy resource
        assertDefaultDummyAccount("jack", "Jack Sparrow", true);

        // Check notifications
		display("Notifications", dummyTransport);

        notificationManager.setDisabled(true);
        checkDummyTransportMessages("accountPasswordNotifier", 1);
        checkDummyTransportMessages("userPasswordNotifier", 0);
        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 1);
        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 1);
        checkDummyTransportMessages("simpleAccountNotifier-DELETE-SUCCESS", 0);
        checkDummyTransportMessages("simpleUserNotifier", 1);
        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);

        assertSteadyResources();

		String expected = "Notification about user-related operation (status: SUCCESS)\n"
				+ "\n"
				+ "User: Jack Sparrow (jack, oid c0c010c0-d34d-b33f-f00d-111111111111)\n"
				+ "\n"
				+ "The user record was modified. Modified attributes are:\n"
				+ " - Assignment:\n"
				+ "   - ADD: \n"
				+ "      - Construction:\n"
				+ "         - Kind: ACCOUNT\n"
				+ "         - resourceRef: Dummy Resource (resource)\n"
				+ "\n"
				+ "Channel: ";
		assertEquals("Wrong message body", expected, dummyTransport.getMessages("dummy:simpleUserNotifier").get(0).getBody());

    }

	@Test
	public void test200SendSmsUsingGet() {
		final String TEST_NAME = "test200SendSmsUsingGet";
		TestUtil.displayTestTitle(this, TEST_NAME);

		// GIVEN
		Task task = taskManager.createTaskInstance(TestNotifications.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();

		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		Event event = new CustomEvent(lightweightIdentifierGenerator, "get", null,
				"hello world", EventOperationType.ADD, EventStatusType.SUCCESS, null);
		notificationManager.processEvent(event, task, result);

		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
		TestUtil.assertSuccess("processEvent result", result);

		assertNotNull("No http request found", httpHandler.lastRequest);
		assertEquals("Wrong HTTP method", "GET", httpHandler.lastRequest.method);
		assertEquals("Wrong URI", "/send?number=%2B421905123456&text=hello+world", httpHandler.lastRequest.uri.toString());
	}

	@Test
	public void test210SendSmsUsingPost() {
		final String TEST_NAME = "test210SendSmsUsingPost";
		TestUtil.displayTestTitle(this, TEST_NAME);

		// GIVEN
		Task task = taskManager.createTaskInstance(TestNotifications.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();

		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		Event event = new CustomEvent(lightweightIdentifierGenerator, "post", null,
				"hello world", EventOperationType.ADD, EventStatusType.SUCCESS, null);
		notificationManager.processEvent(event, task, result);

		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
		TestUtil.assertSuccess("processEvent result", result);

		assertNotNull("No http request found", httpHandler.lastRequest);
		assertEquals("Wrong HTTP method", "POST", httpHandler.lastRequest.method);
		assertEquals("Wrong URI", "/send", httpHandler.lastRequest.uri.toString());
		assertEquals("Wrong Content-Type header", singletonList("application/x-www-form-urlencoded"),
				httpHandler.lastRequest.headers.get("content-type"));
		assertEquals("Wrong X-Custom header", singletonList("test"), httpHandler.lastRequest.headers.get("x-custom"));
		String username = "a9038321";
		String password = "5ecr3t";
		String expectedAuthorization = "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes(StandardCharsets.ISO_8859_1));
		assertEquals("Wrong Authorization header", singletonList(expectedAuthorization), httpHandler.lastRequest.headers.get("authorization"));
		assertEquals("Wrong 1st line of body", "Body=\"hello+world\"&To=%2B421905123456&From=%2B421999000999", httpHandler.lastRequest.body.get(0));
	}

	@Test
	public void test215SendSmsUsingGeneralPost() {
		final String TEST_NAME = "test215SendSmsUsingGeneralPost";
		TestUtil.displayTestTitle(this, TEST_NAME);

		// GIVEN
		Task task = taskManager.createTaskInstance(TestNotifications.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();

		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		Event event = new CustomEvent(lightweightIdentifierGenerator, "general-post", null,
				"hello world", EventOperationType.ADD, EventStatusType.SUCCESS, null);
		notificationManager.processEvent(event, task, result);

		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
		TestUtil.assertSuccess("processEvent result", result);

		assertNotNull("No http request found", httpHandler.lastRequest);
		assertEquals("Wrong HTTP method", "POST", httpHandler.lastRequest.method);
		assertEquals("Wrong URI", "/send", httpHandler.lastRequest.uri.toString());
		assertEquals("Wrong Content-Type header", singletonList("application/x-www-form-urlencoded"),
				httpHandler.lastRequest.headers.get("content-type"));
		assertEquals("Wrong X-Custom header", singletonList("test"), httpHandler.lastRequest.headers.get("x-custom"));
		String username = "a9038321";
		String password = "5ecr3t";
		String expectedAuthorization = "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes(StandardCharsets.ISO_8859_1));
		assertEquals("Wrong Authorization header", singletonList(expectedAuthorization), httpHandler.lastRequest.headers.get("authorization"));
		assertEquals("Wrong 1st line of body", "Body=\"body\"&To=[%2B123, %2B456, %2B789]&From=from", httpHandler.lastRequest.body.get(0));
	}

	@SuppressWarnings("Duplicates")
	private void preTestCleanup(AssignmentPolicyEnforcementType enforcementPolicy) throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
		assumeAssignmentPolicy(enforcementPolicy);
        dummyAuditService.clear();
        prepareNotifications();
        purgeProvisioningScriptHistory();
        rememberCounter(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);
	}

	private static class MyHttpHandler implements HttpHandler {

		private class Request {
			URI uri;
			String method;
			Map<String, List<String>> headers;
			List<String> body;
		}

		private Request lastRequest;

		@Override
		public void handle(HttpExchange httpExchange) throws IOException {
			lastRequest = new Request();
			lastRequest.uri = httpExchange.getRequestURI();
			lastRequest.method = httpExchange.getRequestMethod();
			lastRequest.headers = new HashMap<>();
			// note that header names are case-insensitive in HTTP
			httpExchange.getRequestHeaders().forEach((key, value) -> lastRequest.headers.put(key.toLowerCase(), value));
			lastRequest.body = IOUtils.readLines(httpExchange.getRequestBody(), StandardCharsets.US_ASCII);
			System.out.println(lastRequest.headers);

			String response;
			int responseCode;
			if ("POST".equals(lastRequest.method) && !lastRequest.headers.containsKey("authorization")) {
				response = "Not authorized";
				httpExchange.getResponseHeaders().add("WWW-Authenticate", "Basic realm=\"abc\"");
				responseCode = 401;
			} else {
				response = "OK";
				responseCode = 200;
			}
			httpExchange.sendResponseHeaders(responseCode, response.length());
			OutputStream os = httpExchange.getResponseBody();
			os.write(response.getBytes());
			os.close();
		}
	}
}
