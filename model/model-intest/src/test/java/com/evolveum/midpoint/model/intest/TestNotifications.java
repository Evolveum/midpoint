/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest;

import com.evolveum.midpoint.notifications.api.events.Event;
import com.evolveum.midpoint.notifications.api.transports.Message;
import com.evolveum.midpoint.notifications.impl.events.CustomEventImpl;
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
import com.evolveum.midpoint.test.TestResource;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.RawType;
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

@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestNotifications extends AbstractInitializedModelIntegrationTest {

    public static final File TEST_DIR = new File("src/test/resources/notifications");
    private static final File SYSTEM_CONFIGURATION_FILE = new File(TEST_DIR, "system-configuration.xml");

    private static final TestResource ARCHETYPE_DUMMY = new TestResource(TEST_DIR, "archetype-dummy.xml", "c97780b7-6b07-4a25-be95-60125af6f650");
    private static final TestResource ROLE_DUMMY = new TestResource(TEST_DIR, "role-dummy.xml", "8bc6d827-6ea6-4671-a506-a8388f117880");

    @Autowired private LightweightIdentifierGenerator lightweightIdentifierGenerator;

    private String accountJackOid;
    private HttpServer httpServer;
    private MyHttpHandler httpHandler;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        repoAdd(ARCHETYPE_DUMMY, initResult);
        repoAdd(ROLE_DUMMY, initResult);
        InternalMonitor.reset();
    }

    @Override
    protected File getSystemConfigurationFile() {
        return null;
    }

    @Override
    protected PrismObject<SystemConfigurationType> addSystemConfigurationObject(OperationResult initResult) throws IOException, CommonException,
            EncryptionException {
        List<String> configLines = IOUtils.readLines(new FileReader(SYSTEM_CONFIGURATION_FILE));
        String configString = StringUtils.join(configLines, '\n');
        int port = startHttpServer();
        configString = configString.replaceAll("\\$\\$port\\$\\$", Integer.toString(port));
        PrismObject<SystemConfigurationType> sysconfigObject = prismContext.parseObject(configString);
        repoAddObject(sysconfigObject, initResult);
        return sysconfigObject;
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
        // GIVEN
        Task task = taskManager.createTaskInstance(TestNotifications.class.getName() + ".test100ModifyUserAddAccount");
        task.setChannel(SchemaConstants.CHANNEL_GUI_USER_URI);
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);

        XMLGregorianCalendar startTime = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        when();
        ObjectDelta<UserType> userDelta = createAddAccountDelta(USER_JACK_OID, ACCOUNT_JACK_DUMMY_FILE);
        // This is to test for MID-5849. The applicability checking was not correct, so it passed even if there we no items
        // to show in the notification.
        userDelta.addModification(
                deltaFor(UserType.class)
                        .item(UserType.F_CREDENTIALS, CredentialsType.F_PASSWORD, PasswordType.F_METADATA, MetadataType.F_CREATE_CHANNEL)
                        .replace("dummy")
                        .asItemDelta());
        executeChanges(userDelta, null, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);
        XMLGregorianCalendar endTime = clock.currentTimeXMLGregorianCalendar();
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);

        // Check accountRef
        PrismObject<UserType> userJack = modelService.getObject(UserType.class, USER_JACK_OID, null, task, result);
        assertUserJack(userJack);
        UserType userJackType = userJack.asObjectable();
        assertEquals("Unexpected number of linkRefs", 1, userJackType.getLinkRef().size());
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
        displayDumpable("Dummy transport messages", dummyTransport);

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

        // GIVEN
        Task task = createPlainTask();
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);

        PrismObject<ShadowType> account = PrismTestUtil.parseObject(ACCOUNT_JACK_DUMMY_FILE);
        account.setOid(accountJackOid);

        ObjectDelta<UserType> userDelta = prismContext.deltaFactory().object()
                .createEmptyModifyDelta(UserType.class, USER_JACK_OID);
        ReferenceDelta accountDelta = prismContext.deltaFactory().reference()
                .createModificationDelete(UserType.F_LINK_REF, getUserDefinition(), account);
        userDelta.addModification(accountDelta);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);

        // WHEN
        when();
        modelService.executeChanges(deltas, null, task, result);

        // THEN
        then();
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
        displayDumpable("Notifications", dummyTransport);

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
        // GIVEN
        Task task = createPlainTask();
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.FULL);

        XMLGregorianCalendar startTime = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        when();
        assignAccountToUser(USER_JACK_OID, RESOURCE_DUMMY_OID, null, task, result);

        // THEN
        then();
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
        displayDumpable("Notifications", dummyTransport);

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
    public void test140ModifyUserJackAssignRole() throws Exception {
        // GIVEN
        Task task = createPlainTask();
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.FULL);

        // WHEN
        when();
        assignRole(USER_JACK_OID, ROLE_SUPERUSER_OID, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess("executeChanges result", result);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after change execution", userJack);
        assertUserJack(userJack);
        assertAssignedRole(userJack, ROLE_SUPERUSER_OID);
        assertAssignments(userJack, 2);

        // Check notifications
        displayDumpable("Notifications", dummyTransport);

        notificationManager.setDisabled(true);
        checkDummyTransportMessages("accountPasswordNotifier", 0);
        checkDummyTransportMessages("userPasswordNotifier", 0);
        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 0);
        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 0);
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
                + "      - Target: Superuser (role)\n"
                + "\n"
                + "Channel: ";
        assertEquals("Wrong message body", expected, dummyTransport.getMessages("dummy:simpleUserNotifier").get(0).getBody());
    }

    @Test
    public void test150ModifyUserJackModifyAssignment() throws Exception {
        // GIVEN
        Task task = createPlainTask();
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.FULL);

        // WHEN
        when();
        PrismObject<UserType> jack = getUser(USER_JACK_OID);
        AssignmentType assignment = findAssignmentByTargetRequired(jack, ROLE_SUPERUSER_OID);
        Long id = assignment.getId();
        executeChanges(
                prismContext.deltaFor(UserType.class)
                        .item(UserType.F_ASSIGNMENT, id, AssignmentType.F_DESCRIPTION)
                        .replace("hi")
                        .asObjectDeltaCast(jack.getOid()), null, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess("executeChanges result", result);

        PrismObject<UserType> jackAfter = getUser(USER_JACK_OID);
        display("User after change execution", jackAfter);
        assertUserJack(jackAfter);

        // Check notifications
        displayDumpable("Notifications", dummyTransport);

        notificationManager.setDisabled(true);
        checkDummyTransportMessages("accountPasswordNotifier", 0);
        checkDummyTransportMessages("userPasswordNotifier", 0);
        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 0);
        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 0);
        checkDummyTransportMessages("simpleAccountNotifier-DELETE-SUCCESS", 0);
        checkDummyTransportMessages("simpleUserNotifier", 1);
        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);

        assertSteadyResources();

        String expected = "Notification about user-related operation (status: SUCCESS)\n"
                + "\n"
                + "User: Jack Sparrow (jack, oid c0c010c0-d34d-b33f-f00d-111111111111)\n"
                + "\n"
                + "The user record was modified. Modified attributes are:\n"
                + " - Assignment["+id+"]/Description:\n"
                + "   - REPLACE: hi\n"
                + "\n"
                + "Notes:\n"
                + " - Assignment["+id+"]:\n"
                + "    - Description: hi\n"
                + "    - Target: Superuser (role) [default]\n"
                + "\n"
                + "Channel: ";
        assertEquals("Wrong message body", expected, dummyTransport.getMessages("dummy:simpleUserNotifier").get(0).getBody());
    }

    @Test
    public void test160ModifyUserJackDeleteAssignment() throws Exception {
        // GIVEN
        Task task = createPlainTask();
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.FULL);

        // WHEN
        when();
        PrismObject<UserType> jack = getUser(USER_JACK_OID);
        AssignmentType assignment = findAssignmentByTargetRequired(jack, ROLE_SUPERUSER_OID);
        Long id = assignment.getId();
        executeChanges(
                prismContext.deltaFor(UserType.class)
                        .item(UserType.F_ASSIGNMENT)
                        .delete(new AssignmentType(prismContext).id(id))
                        .asObjectDeltaCast(jack.getOid()), null, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess("executeChanges result", result);

        PrismObject<UserType> jackAfter = getUser(USER_JACK_OID);
        display("User after change execution", jackAfter);
        assertUserJack(jackAfter);

        // Check notifications
        displayDumpable("Notifications", dummyTransport);

        notificationManager.setDisabled(true);
        checkDummyTransportMessages("accountPasswordNotifier", 0);
        checkDummyTransportMessages("userPasswordNotifier", 0);
        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 0);
        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 0);
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
                + "   - DELETE: \n"
                + "      - Description: hi\n"
                + "      - Target: Superuser (role) [default]\n"
                + "\n"
                + "Channel: ";
        assertEquals("Wrong message body", expected, dummyTransport.getMessages("dummy:simpleUserNotifier").get(0).getBody());
    }

    @Test
    public void test200SendSmsUsingGet() {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        Event event = new CustomEventImpl(lightweightIdentifierGenerator, "get", null,
                "hello world", EventOperationType.ADD, EventStatusType.SUCCESS, null);
        notificationManager.processEvent(event, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess("processEvent result", result);

        assertNotNull("No http request found", httpHandler.lastRequest);
        assertEquals("Wrong HTTP method", "GET", httpHandler.lastRequest.method);
        assertEquals("Wrong URI", "/send?number=%2B421905123456&text=hello+world", httpHandler.lastRequest.uri.toString());
    }

    @Test
    public void test210SendSmsUsingPost() {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        Event event = new CustomEventImpl(lightweightIdentifierGenerator, "post", null,
                "hello world", EventOperationType.ADD, EventStatusType.SUCCESS, null);
        notificationManager.processEvent(event, task, result);

        // THEN
        then();
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
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        Event event = new CustomEventImpl(lightweightIdentifierGenerator, "general-post", null,
                "hello world", EventOperationType.ADD, EventStatusType.SUCCESS, null);
        notificationManager.processEvent(event, task, result);

        // THEN
        then();
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

    @Test
    public void test220SendSmsViaProxy() {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        Event event = new CustomEventImpl(lightweightIdentifierGenerator, "get-via-proxy", null,
                "hello world via proxy", EventOperationType.ADD, EventStatusType.SUCCESS, null);
        notificationManager.processEvent(event, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess("processEvent result", result);

        assertNotNull("No http request found", httpHandler.lastRequest);
        assertEquals("Wrong HTTP method", "GET", httpHandler.lastRequest.method);
        assertTrue("Header proxy-connection not found in request headers", httpHandler.lastRequest.headers.containsKey("proxy-connection"));
        assertEquals("Wrong proxy-connection header", "Keep-Alive", httpHandler.lastRequest.headers.get("proxy-connection").get(0));
    }

    @Test
    public void test300CheckVariables() {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        prepareNotifications();

        // WHEN
        when();
        Event event = new CustomEventImpl(lightweightIdentifierGenerator, "check-variables", null,
                "hello world", EventOperationType.ADD, EventStatusType.SUCCESS, null);
        notificationManager.processEvent(event, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess("processEvent result", result);

        displayAllNotifications();
        assertSingleDummyTransportMessage("check-variables", "variables ok");
    }

    @Test
    public void test400StringAttachment() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.FULL);

        // WHEN
        when();
        PrismObject<UserType> user = new UserType(prismContext)
                .name("testStringAttachmentUser")
                .asPrismObject();
        addObject(user);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess("addObject result", result);

        // Check notifications
        displayDumpable("Notifications", dummyTransport);

        notificationManager.setDisabled(true);
        checkDummyTransportMessages("string-attachment", 1);

        Message message = dummyTransport.getMessages("dummy:string-attachment").get(0);
        assertEquals("Wrong # of attachments", 1, message.getAttachments().size());
        assertEquals("Wrong contentType of attachment", "text/plain", message.getAttachments().get(0).getContentType());
        Object content = RawType.getValue(message.getAttachments().get(0).getContent());
        assertEquals("Wrong content of attachments", "Hello world", content);
        assertEquals("Wrong fileName of attachments", "plain.txt", message.getAttachments().get(0).getFileName());
        assertNull("Wrong fileName of attachments", message.getAttachments().get(0).getContentFromFile());
    }

    @Test
    public void test410ByteAttachment() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.FULL);

        // WHEN
        when();
        PrismObject<UserType> user = new UserType(prismContext)
                .name("testByteAttachmentUser")
                .asPrismObject();
        addObject(user);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess("addObject result", result);

        // Check notifications
        displayDumpable("Notifications", dummyTransport);

        notificationManager.setDisabled(true);
        checkDummyTransportMessages("byte-attachment", 1);

        Message message = dummyTransport.getMessages("dummy:byte-attachment").get(0);
        assertEquals("Wrong # of attachments", 1, message.getAttachments().size());
        assertEquals("Wrong contentType of attachment", "image/jpeg", message.getAttachments().get(0).getContentType());
        String origJPEGString = "/9j/4AAQSkZJRgABAQEAYABgAAD/4QAiRXhpZgAATU0AKgAAAAgAAQESAAMAAAABAAEAAAAAAAD/2wBDAAIBAQ"
                + "IBAQICAgICAgICAwUDAwMDAwYEBAMFBwYHBwcGBwcICQsJCAgKCAcHCg0KCgsMDAwMBwkODw0MDgsMDAz/2wBDAQICAgMDAwYDAw"
                + "YMCAcIDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAz/wAARCAAFAAUDASIAAhEBAxEB/8"
                + "QAHwAAAQUBAQEBAQEAAAAAAAAAAAECAwQFBgcICQoL/8QAtRAAAgEDAwIEAwUFBAQAAAF9AQIDAAQRBRIhMUEGE1FhByJxFDKBka"
                + "EII0KxwRVS0fAkM2JyggkKFhcYGRolJicoKSo0NTY3ODk6Q0RFRkdISUpTVFVWV1hZWmNkZWZnaGlqc3R1dnd4eXqDhIWGh4iJip"
                + "KTlJWWl5iZmqKjpKWmp6ipqrKztLW2t7i5usLDxMXGx8jJytLT1NXW19jZ2uHi4+Tl5ufo6erx8vP09fb3+Pn6/8QAHwEAAwEBAQ"
                + "EBAQEBAQAAAAAAAAECAwQFBgcICQoL/8QAtREAAgECBAQDBAcFBAQAAQJ3AAECAxEEBSExBhJBUQdhcRMiMoEIFEKRobHBCSMzUv"
                + "AVYnLRChYkNOEl8RcYGRomJygpKjU2Nzg5OkNERUZHSElKU1RVVldYWVpjZGVmZ2hpanN0dXZ3eHl6goOEhYaHiImKkpOUlZaXmJ"
                + "maoqOkpaanqKmqsrO0tba3uLm6wsPExcbHyMnK0tPU1dbX2Nna4uPk5ebn6Onq8vP09fb3+Pn6/9oADAMBAAIRAxEAPwDg/hn471"
                + "A6fqljY22i28kWq3V9NPNayTNcG6fzlU4lU/uwfLBJOVVeFxyUUV5OXcXZxSw8YU8RJLXRW6tt9O55nEnhZwnicxqVsRgKcpPlu2"
                + "m3pFJat9Ekl5H/2Q==";
        byte[] origJPEG = Base64.getDecoder().decode(origJPEGString);
        Object content = RawType.getValue(message.getAttachments().get(0).getContent());
        if(!(content instanceof byte[]) || !Arrays.equals(origJPEG, (byte[])content)) {
            throw new AssertionError("Wrong content of attachments expected:" + Arrays.toString(origJPEG) + " but was:" + content);
        }
        assertEquals("Wrong fileName of attachments", "alf.jpg", message.getAttachments().get(0).getFileName());
        assertNull("Wrong fileName of attachments", message.getAttachments().get(0).getContentFromFile());
    }

    @Test
    public void test420AttachmentFromFile() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.FULL);

        // WHEN
        when();
        PrismObject<UserType> user = new UserType(prismContext)
                .name("testAttachmentFromFileUser")
                .asPrismObject();
        addObject(user);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess("addObject result", result);

        // Check notifications
        displayDumpable("Notifications", dummyTransport);

        notificationManager.setDisabled(true);
        checkDummyTransportMessages("attachment-from-file", 1);

        Message message = dummyTransport.getMessages("dummy:attachment-from-file").get(0);
        assertEquals("Wrong # of attachments", 1, message.getAttachments().size());
        assertEquals("Wrong contentType of attachment", "image/png", message.getAttachments().get(0).getContentType());
        assertEquals("Wrong fileName of attachments", "alf.png", message.getAttachments().get(0).getFileName());
        assertEquals("Wrong fileName of attachments", "/home/user/example.png", message.getAttachments().get(0).getContentFromFile());
        assertNull("Wrong fileName of attachments", message.getAttachments().get(0).getContent());
    }

    @Test
    public void test430ExpressionAttachment() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.FULL);

        // WHEN
        when();
        PrismObject<UserType> user = new UserType(prismContext)
                .name("testExpressionAttachmentUser")
                .asPrismObject();
        addObject(user);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess("addObject result", result);

        // Check notifications
        displayDumpable("Notifications", dummyTransport);

        notificationManager.setDisabled(true);
        checkDummyTransportMessages("expression-attachment", 1);

        Message message = dummyTransport.getMessages("dummy:expression-attachment").get(0);
        assertEquals("Wrong # of attachments", 1, message.getAttachments().size());
        assertEquals("Wrong contentType of attachment", "text/html", message.getAttachments().get(0).getContentType());
        assertEquals("Wrong content of attachments", "<!DOCTYPE html><html><body>Hello World!</body></html>", message.getAttachments().get(0).getContent());
        assertEquals("Wrong fileName of attachments", "hello_world.html", message.getAttachments().get(0).getFileName());
        assertNull("Wrong fileName of attachments", message.getAttachments().get(0).getContentFromFile());
    }

    // TODO binary attachment, attachment from file, attachment from expression

    /**
     * MID-5350
     */
    @Test
    public void test500RecomputeRole() throws Exception {

        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.FULL);

        when();
        recomputeFocus(RoleType.class, ROLE_DUMMY.oid, task, result);

        then();
        result.computeStatus();
        TestUtil.assertSuccess("executeChanges result", result);

        // Check notifications
        displayDumpable("Notifications", dummyTransport);

        notificationManager.setDisabled(true);
        checkDummyTransportMessages("simpleRoleNotifier", 0); // MID-5350 (other asserts are just for sure)
        checkDummyTransportMessages("accountPasswordNotifier", 0);
        checkDummyTransportMessages("userPasswordNotifier", 0);
        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 0);
        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 0);
        checkDummyTransportMessages("simpleAccountNotifier-DELETE-SUCCESS", 0);
        checkDummyTransportMessages("simpleUserNotifier", 0);
        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);

        assertSteadyResources();
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

        private static class Request {
            private URI uri;
            private String method;
            private Map<String, List<String>> headers;
            private List<String> body;
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
