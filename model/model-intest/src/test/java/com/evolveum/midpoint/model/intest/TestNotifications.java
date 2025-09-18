/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.AssertJUnit.*;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.prism.path.ItemPath;

import com.evolveum.midpoint.repo.api.RepoAddOptions;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import com.evolveum.midpoint.notifications.api.events.Event;
import com.evolveum.midpoint.notifications.api.transports.Message;
import com.evolveum.midpoint.notifications.impl.events.CustomEventImpl;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.LightweightIdentifierGenerator;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyTestResource;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.RawType;

@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestNotifications extends AbstractInitializedModelIntegrationTest {

    public static final File TEST_DIR = new File("src/test/resources/notifications");
    private static final File SYSTEM_CONFIGURATION_FILE = new File(TEST_DIR, "system-configuration.xml");

    private static final TestObject<ArchetypeType> ARCHETYPE_DUMMY = TestObject.file(TEST_DIR, "archetype-dummy.xml", "c97780b7-6b07-4a25-be95-60125af6f650");
    private static final TestObject<RoleType> ROLE_DUMMY = TestObject.file(TEST_DIR, "role-dummy.xml", "8bc6d827-6ea6-4671-a506-a8388f117880");
    private static final TestObject<RoleType> ROLE_WEBMAIL = TestObject.file(TEST_DIR, "role-webmail.xml", "ba0d281a-b0e0-4d3a-ade0-513f53454c27");

    private static final TestObject<TaskType> TASK_HR_IMPORT = TestObject.file(TEST_DIR, "task-hr-import.xml", "b5ee6532-b779-4bee-b713-d394346170f7");
    private static final TestObject<TaskType> SECURITY_POLICY_HASH_PASSWORD = TestObject.file(TEST_DIR, "security-policy-hash-password.xml", "00000000-0000-0000-0000-000000000120");

    private static final DummyTestResource RESOURCE_HR = new DummyTestResource(TEST_DIR, "resource-hr.xml", "bb9b9bca-5d47-446a-83ed-6c5411ac219f", "hr");
    private static final DummyTestResource RESOURCE_WEBMAIL = new DummyTestResource(TEST_DIR, "resource-webmail.xml", "657fce5e-9d7a-4bab-b475-157ca586f73a", "webmail");
    private static final String USERNAME_GREGOR = "gregor";

    @Autowired private LightweightIdentifierGenerator lightweightIdentifierGenerator;

    private String accountJackOid;
    private HttpServer httpServer;
    private MyHttpHandler httpHandler;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        repoAdd(ARCHETYPE_DUMMY, initResult);
        repoAdd(ROLE_DUMMY, initResult);
        repoAdd(ROLE_WEBMAIL, initResult);
        repoAdd(TASK_HR_IMPORT, initResult);
        repoAdd(SECURITY_POLICY_HASH_PASSWORD, RepoAddOptions.createOverwrite(), initResult);

        initDummyResource(RESOURCE_HR, initTask, initResult);
        initDummyResource(RESOURCE_WEBMAIL, initTask, initResult);

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
        given();
        Task task = taskManager.createTaskInstance(TestNotifications.class.getName() + ".test100ModifyUserAddAccount");
        task.setChannel(SchemaConstants.CHANNEL_USER_URI);
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);

        XMLGregorianCalendar startTime = clock.currentTimeXMLGregorianCalendar();

        when();
        ObjectDelta<UserType> userDelta = createAddAccountDelta(USER_JACK_OID, ACCOUNT_JACK_DUMMY_FILE);
        // This is to test for MID-5849. The applicability checking was not correct, so it passed
        // even if there are no items to show in the notification.
        userDelta.addModification(
                deltaFor(UserType.class)
                        .item(UserType.F_CREDENTIALS, CredentialsType.F_PASSWORD, PasswordType.F_METADATA, MetadataType.F_CREATE_CHANNEL)
                        .replace("dummy")
                        .asItemDelta());
        executeChanges(userDelta, null, task, result);

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
        var accountShadow = getShadowRepo(accountJackOid);
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
        checkDummyTransportMessages("simpleAccountNotifier-IN-PROGRESS", 0);
        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 1);
        checkDummyTransportMessages("simpleUserNotifier", 0);
        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);

        List<Message> pwdMessages = dummyTransport.getMessages("dummy:accountPasswordNotifier");
        Message pwdMessage = pwdMessages.get(0); // number of messages was already checked
        assertEquals("Invalid list of recipients", singletonList("recipient@evolveum.com"), pwdMessage.getTo());
        assertThat(pwdMessage.getBody()) // there can be subscription footer
                .startsWith("Password for account jack on Dummy Resource is: deadmentellnotales");

        List<Message> addMessages = dummyTransport.getMessages("dummy:simpleAccountNotifier-ADD-SUCCESS");
        Message addMessage = addMessages.get(0); // number of messages was already checked
        assertEquals("Invalid list of recipients", singletonList("recipient@evolveum.com"), addMessage.getTo());
        assertThat(addMessage.getBody()) // there can be subscription footer
                .startsWith("Notification about account-related operation\n"
                        + "\n"
                        + "Owner: Jack Sparrow (jack, oid c0c010c0-d34d-b33f-f00d-111111111111)\n"
                        + "Resource: Dummy Resource (oid 10000000-0000-0000-0000-000000000004)\n"
                        + "\n"
                        + "An account has been successfully created on the resource with attributes:\n"
                        + "Account \"jack\" (default) created on \"Dummy Resource\":\n"
                        + "|\tResource: Dummy Resource\n"
                        + "|\tKind: Account\n"
                        + "|\tIntent: default\n"
                        + "|\tAdd \"Activation\":\n"
                        + "|\t|\tAdministrative status: Enabled\n"
                        + "|\tAdd \"Default Account\":\n"
                        + "|\t|\tUID: jack\n"
                        + "|\t|\tUsername: jack\n"
                        + "|\t|\tLocation: Caribbean\n"
                        + "|\t|\tQuote: Arr!\n"
                        + "|\t|\tDrink: rum\n"
                        + "|\t|\tWeapon: rum\n"
                        + "|\t|\tFull Name: Jack Sparrow\n"
                        + "|\tPassword created:\n"
                        + "|\t|\tValue: (protected string)\n"
                        + "Channel: http://midpoint.evolveum.com/xml/ns/public/common/channels-3#user\n"
                        + "No active subscription. Please support midPoint by purchasing a subscription.");

        assertSteadyResources();
    }

    @Test
    public void test119ModifyUserDeleteAccount() throws Exception {

        given();
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

        when();
        modelService.executeChanges(deltas, null, task, result);

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
            AssertJUnit.fail("Shadow " + accountJackOid + " still exists");
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

        // there can be subscription footer
        assertThat(dummyTransport.getMessages("dummy:simpleAccountNotifier-DELETE-SUCCESS").get(0).getBody())
                .startsWith("Notification about account-related operation\n"
                        + "\n"
                        + "Owner: Jack Sparrow (jack, oid c0c010c0-d34d-b33f-f00d-111111111111)\n"
                        + "Resource: Dummy Resource (oid 10000000-0000-0000-0000-000000000004)\n"
                        + "Account: jack\n"
                        + "\n"
                        + "The account has been successfully removed from the resource.\n"
                        + "\n"
                        + "Channel: ");

        assertSteadyResources();
    }

    @Test
    public void test131ModifyUserJackAssignAccount() throws Exception {
        given();
        Task task = createPlainTask();
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.FULL);

        XMLGregorianCalendar startTime = clock.currentTimeXMLGregorianCalendar();

        when();
        assignAccountToUser(USER_JACK_OID, RESOURCE_DUMMY_OID, null, task, result);

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
        var accountShadow = getShadowRepo(accountJackOid);
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

        // there can be subscription footer
        assertThat(dummyTransport.getMessages("dummy:simpleUserNotifier").get(0).getBody())
                .startsWith("Notification about user-related operation (status: SUCCESS)\n"
                        + "\n"
                        + "User: Jack Sparrow (jack, oid c0c010c0-d34d-b33f-f00d-111111111111)\n"
                        + "\n"
                        + "The user record was modified. Modified attributes are:\n"
                        + "Modify User \"Jack Sparrow (jack)\":\n"
                        + "|	Construction of Account on \"Dummy Resource\" assigned:\n"
                        + "|	|	Add \"Construction\":\n"
                        + "|	|	|	Kind: Account\n"
                        + "|	|	|	resourceRef: Dummy Resource\n"
                        + "|	|	Assignment was enabled\n"
                        + "\n"
                        + "Channel: ");
    }

    @Test
    public void test140ModifyUserJackAssignRole() throws Exception {
        given();
        Task task = createPlainTask();
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.FULL);

        when();
        assignRole(USER_JACK_OID, ROLE_SUPERUSER.oid, task, result);

        then();
        result.computeStatus();
        TestUtil.assertSuccess("executeChanges result", result);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after change execution", userJack);
        assertUserJack(userJack);
        assertAssignedRole(userJack, ROLE_SUPERUSER.oid);
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

        // there can be subscription footer
        assertThat(dummyTransport.getMessages("dummy:simpleUserNotifier").get(0).getBody())
                .startsWith("Notification about user-related operation (status: SUCCESS)\n"
                        + "\n"
                        + "User: Jack Sparrow (jack, oid c0c010c0-d34d-b33f-f00d-111111111111)\n"
                        + "\n"
                        + "The user record was modified. Modified attributes are:\n"
                        + "Modify User \"Jack Sparrow (jack)\":\n"
                        + "|	Role \"Superuser\" assigned:\n"
                        + "|	|	Target: Superuser\n"
                        + "|	|	Assignment was enabled\n"
                        + "\n"
                        + "Channel: ");
    }

    @Test
    public void test150ModifyUserJackModifyAssignment() throws Exception {
        given();
        Task task = createPlainTask();
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.FULL);

        when();
        PrismObject<UserType> jack = getUser(USER_JACK_OID);
        AssignmentType assignment = findAssignmentByTargetRequired(jack, ROLE_SUPERUSER.oid);
        Long id = assignment.getId();
        executeChanges(
                prismContext.deltaFor(UserType.class)
                        .item(UserType.F_ASSIGNMENT, id, AssignmentType.F_DESCRIPTION)
                        .replace("hi")
                        .asObjectDelta(jack.getOid()), null, task, result);

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

        // there can be subscription footer
        assertThat(dummyTransport.getMessages("dummy:simpleUserNotifier").get(0).getBody())
                .startsWith("Notification about user-related operation (status: SUCCESS)\n"
                        + "\n"
                        + "User: Jack Sparrow (jack, oid c0c010c0-d34d-b33f-f00d-111111111111)\n"
                        + "\n"
                        + "The user record was modified. Modified attributes are:\n"
                        + "Modify User \"Jack Sparrow (jack)\":\n"
                        + "|	Role \"Superuser\" modified:\n"
                        + "|	|	Additional identification (not modified data):\n"
                        + "|	|	|	Target: Superuser\n"
                        + "|	|	Added properties:\n"
                        + "|	|	|	Description: hi\n"
                        + "\n"
                        + "Channel: ");
    }

    @Test
    public void test160ModifyUserJackDeleteAssignment() throws Exception {
        given();
        Task task = createPlainTask();
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.FULL);

        when();
        PrismObject<UserType> jack = getUser(USER_JACK_OID);
        AssignmentType assignment = findAssignmentByTargetRequired(jack, ROLE_SUPERUSER.oid);
        Long id = assignment.getId();
        executeChanges(
                prismContext.deltaFor(UserType.class)
                        .item(UserType.F_ASSIGNMENT)
                        .delete(new AssignmentType().id(id))
                        .asObjectDelta(jack.getOid()), null, task, result);

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

        // there can be subscription footer
        assertThat(dummyTransport.getMessages("dummy:simpleUserNotifier").get(0).getBody())
                .startsWith("Notification about user-related operation (status: SUCCESS)\n"
                        + "\n"
                        + "User: Jack Sparrow (jack, oid c0c010c0-d34d-b33f-f00d-111111111111)\n"
                        + "\n"
                        + "The user record was modified. Modified attributes are:\n"
                        + "Modify User \"Jack Sparrow (jack)\":\n"
                        + "|	Role \"Superuser\" unassigned:\n"
                        + "|	|	Description: hi\n"
                        + "|	|	Target: Superuser\n"
                        + "|	|	Delete \"Activation\"\n"
                        + "\n"
                        + "Channel: ");
    }

    /**
     * Tests MID-6289 - correct resource object notification during import process
     */
    @Test
    public void test170ImportUserGregor() throws Exception {
        given();
        Task task = createPlainTask();
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.RELATIVE);

        RESOURCE_HR.controller.addAccount(USERNAME_GREGOR);

        when();
        rerunTask(TASK_HR_IMPORT.oid, result);

        then();
        assertUserAfterByUsername(USERNAME_GREGOR)
                .assertAssignments(1)
                .assertLiveLinks(2);

        displayDumpable("Notifications", dummyTransport);

        notificationManager.setDisabled(true);
        checkDummyTransportMessages("accountPasswordNotifier", 0);
        checkDummyTransportMessages("userPasswordNotifier", 0);
        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 1);
        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 1);
        checkDummyTransportMessages("simpleAccountNotifier-DELETE-SUCCESS", 0);
        checkDummyTransportMessages("simpleUserNotifier", 1);
        checkDummyTransportMessages("simpleUserNotifier-ADD", 1);

        assertChannel(dummyTransport.getMessages("dummy:simpleUserNotifier").get(0), SchemaConstants.CHANNEL_IMPORT_URI);
        assertChannel(dummyTransport.getMessages("dummy:simpleUserNotifier-ADD").get(0), SchemaConstants.CHANNEL_IMPORT_URI);
        assertChannel(dummyTransport.getMessages("dummy:simpleAccountNotifier-SUCCESS").get(0), SchemaConstants.CHANNEL_IMPORT_URI);
        assertChannel(dummyTransport.getMessages("dummy:simpleAccountNotifier-ADD-SUCCESS").get(0), SchemaConstants.CHANNEL_IMPORT_URI);
    }

    @Test
    public void test200SendSmsUsingGet() {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        Event event = new CustomEventImpl(
                lightweightIdentifierGenerator, "get",
                "hello world", EventOperationType.ADD, EventStatusType.SUCCESS, null);
        notificationManager.processEvent(event, task, result);

        then();
        result.computeStatus();
        TestUtil.assertSuccess("processEvent result", result);

        assertNotNull("No http request found", httpHandler.lastRequest);
        assertEquals("Wrong HTTP method", "GET", httpHandler.lastRequest.method);
        assertEquals("Wrong URI", "/send?number=%2B421905123456&text=hello+world", httpHandler.lastRequest.uri.toString());
    }

    @Test
    public void test210SendSmsUsingPost() {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        Event event = new CustomEventImpl(
                lightweightIdentifierGenerator, "post",
                "hello world", EventOperationType.ADD, EventStatusType.SUCCESS, null);
        notificationManager.processEvent(event, task, result);

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
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        int messagesHandledOld = httpHandler.messagesHandled;

        when();
        Event event = new CustomEventImpl(
                lightweightIdentifierGenerator, "general-post",
                "hello world", EventOperationType.ADD, EventStatusType.SUCCESS, null);
        notificationManager.processEvent(event, task, result);

        then();
        result.computeStatus();
        TestUtil.assertSuccess("processEvent result", result);

        assertThat(httpHandler.messagesHandled).isEqualTo(messagesHandledOld + 3); // 3 messages for 3 recipients
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
        // only the last recipient here + there can be subscription footer in the body
        assertThat(httpHandler.lastRequest.body.get(0))
                .matches("Body=\"body[^\"]*\"&To=\\[%2B789]&From=from");
    }

    // FIXME: Disabled, HttpClient after update does not generate proxy-connection headers.
    @Test(enabled = false)
    public void test220SendSmsViaProxy() {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        Event event = new CustomEventImpl(
                lightweightIdentifierGenerator, "get-via-proxy",
                "hello world via proxy", EventOperationType.ADD, EventStatusType.SUCCESS, null);
        notificationManager.processEvent(event, task, result);

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
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        prepareNotifications();

        when();
        Event event = new CustomEventImpl(
                lightweightIdentifierGenerator, "check-variables",
                "hello world", EventOperationType.ADD, EventStatusType.SUCCESS, null);
        notificationManager.processEvent(event, task, result);

        then();
        result.computeStatus();
        TestUtil.assertSuccess("processEvent result", result);

        displayAllNotifications();
        assertSingleDummyTransportMessage("check-variables", "variables ok");
    }

    @Test
    public void test400StringAttachment() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.FULL);

        when();
        PrismObject<UserType> user = new UserType()
                .name("testStringAttachmentUser")
                .asPrismObject();
        addObject(user);

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
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.FULL);

        when();
        PrismObject<UserType> user = new UserType()
                .name("testByteAttachmentUser")
                .asPrismObject();
        addObject(user);

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
        if (!(content instanceof byte[]) || !Arrays.equals(origJPEG, (byte[]) content)) {
            throw new AssertionError("Wrong content of attachments expected:" + Arrays.toString(origJPEG) + " but was:" + content);
        }
        assertEquals("Wrong fileName of attachments", "alf.jpg", message.getAttachments().get(0).getFileName());
        assertNull("Wrong fileName of attachments", message.getAttachments().get(0).getContentFromFile());
    }

    @Test
    public void test420AttachmentFromFile() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.FULL);

        when();
        PrismObject<UserType> user = new UserType()
                .name("testAttachmentFromFileUser")
                .asPrismObject();
        addObject(user);

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
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.FULL);

        when();
        PrismObject<UserType> user = new UserType()
                .name("testExpressionAttachmentUser")
                .asPrismObject();
        addObject(user);

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

    /**
     * Modifying an account while resource is in maintenance mode.
     *
     * It should not display operational items: MID-6859
     */
    @Test
    public void test510ModifyUserAssignAccountInMaintenance() throws Exception {
        given();

        Task task = createPlainTask();
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.FULL);

        UserType user = new UserType()
                .name("test510")
                .beginAssignment()
                .beginConstruction()
                .resourceRef(RESOURCE_DUMMY_OID, ResourceType.COMPLEX_TYPE)
                .<AssignmentType>end()
                .end();

        addObject(user, task, result);

        turnMaintenanceModeOn(RESOURCE_DUMMY_OID, result);

        dummyTransport.clearMessages();

        when();

        ObjectDelta<UserType> delta = deltaFor(UserType.class)
                .item(UserType.F_FULL_NAME).replace(PolyString.fromOrig("TEST510"))
                .asObjectDelta(user.getOid());

        executeChanges(delta, null, task, result);

        then();

        assertInProgress(result);

        assertUserAfter(user.getOid())
                .assertLinks(1, 0);

        notificationManager.setDisabled(true);
        displayDumpable("dummy", dummyTransport);
        checkDummyTransportMessages("simpleAccountNotifier-IN-PROGRESS", 1);

        String expected = "The account has been ATTEMPTED to be modified on the resource. Modified attributes are:\n"
                + "Account \"test510\" (default) modified on \"Dummy Resource\":\n"
                + "|	Additional identification (not modified data):\n"
                + "|	|	Resource: Dummy Resource\n"
                + "|	|	Kind: Account\n"
                + "|	|	Intent: default\n"
                + "|	Modify \"Default Account\":\n"
                + "|	|	Added properties:\n"
                + "|	|	|	Full Name: TEST510\n"
                + "\n"
                + "The operation will be retried.\n";
        String actual = dummyTransport.getMessages("dummy:simpleAccountNotifier-IN-PROGRESS").get(0).getBody();
        assertTrue("Wrong message body:\n" + actual, actual.contains(expected));
    }

    /**
     * Change user password, when store method is hash (MID-9504)
     */
    @Test
    public void test600ModifyHashStoredPassword() throws Exception {
        given();
        Task task = taskManager.createTaskInstance(TestNotifications.class.getName() + ".test600ModifyHashStoredPassword");
        task.setChannel(SchemaConstants.CHANNEL_USER_URI);
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);

        when();
        ProtectedStringType passEncrypt = protector.encryptString("dummyPassword");
        passEncrypt.setClearValue(null);
        ObjectDelta<UserType> userDelta = createModifyUserAddDelta(
                USER_BARBOSSA_OID,
                ItemPath.create(UserType.F_CREDENTIALS, CredentialsType.F_PASSWORD, PasswordType.F_VALUE),
                passEncrypt);
        executeChanges(userDelta, new ModelExecuteOptions().reconcile(false), task, result);

        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userBarbarosa = modelService.getObject(UserType.class, USER_BARBOSSA_OID, null, task, result);
        UserType userBarbarosaType = userBarbarosa.asObjectable();
        assertTrue("Password isn't save as hash", userBarbarosaType.getCredentials().getPassword().getValue().isHashed());

        notificationManager.setDisabled(true);

        // Check notifications
        displayDumpable("Dummy transport messages", dummyTransport);

        checkDummyTransportMessages("accountPasswordNotifier", 0);
        checkDummyTransportMessages("userPasswordNotifier", 1);
        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 0);
        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
        checkDummyTransportMessages("simpleAccountNotifier-IN-PROGRESS", 0);
        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 0);
        checkDummyTransportMessages("simpleUserNotifier", 1);
        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);

        List<Message> pwdMessages = dummyTransport.getMessages("dummy:userPasswordNotifier");
        Message pwdMessage = pwdMessages.get(0); // number of messages was already checked
        assertEquals("Invalid list of recipients", singletonList("recipient@evolveum.com"), pwdMessage.getTo());
        assertThat(pwdMessage.getBody()) // there can be subscription footer
                .startsWith("Password: dummyPassword");
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
        private int messagesHandled;

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
                messagesHandled++; // we want to count only OK messages, not authorization requests
            }
            httpExchange.sendResponseHeaders(responseCode, response.length());
            OutputStream os = httpExchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

    private void assertChannel(Message message, String expectedChannelUri) {
        if (!message.getBody().contains("Channel: " + expectedChannelUri)) {
            fail("Channel " + expectedChannelUri + " not present in the message body:\n" + message.getBody());
        }
    }
}
