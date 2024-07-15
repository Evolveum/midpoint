/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.RI_ACCOUNT_OBJECT_CLASS;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType.ACCOUNT;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.icf.dummy.resource.DummyPrivilege;

import com.evolveum.midpoint.schema.processor.ResourceSchemaFactory;
import com.evolveum.midpoint.schema.util.Resource;
import com.evolveum.midpoint.test.DummyResourceContoller;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyTestResource;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentPolicyEnforcementType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Various intent-related tests:
 *
 * . `test1xx`: basic functioning of multiple intents (assigning/unassigning accounts with different intents)
 * . `test2xx`: synchronization tasks in environments with multiple intents
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestIntent extends AbstractInitializedModelIntegrationTest {

    private static final String ACCOUNT_INTENT_TEST = "test";

    private String accountOid;

    private static final File TEST_DIR = new File("src/test/resources/intents");

    private static final DummyTestResource RESOURCE_DUMMY_INTENTS = new DummyTestResource(
            TEST_DIR, "resource-dummy-intents.xml", "82fcb11c-136d-4d9e-a2bb-6e6c79c547e4", "intents");

    private DummyPrivilege guestPrivilege;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        var testResult = testResource(RESOURCE_DUMMY_OID, initTask, initResult);
        assertSuccess(testResult);

        addObject(SHADOW_GROUP_DUMMY_TESTERS_FILE, initTask, initResult);

        initTestObjects(initTask, initResult,
                RESOURCE_DUMMY_INTENTS);

        guestPrivilege = new DummyPrivilege("guest");
        RESOURCE_DUMMY_INTENTS.controller.getDummyResource().addPrivilege(guestPrivilege);

        var schema = ResourceSchemaFactory.getCompleteSchema(getDummyResourceType());
        displayDumpable("dummy schema", schema);

        rememberSteadyResources();
    }

    @Test
    public void test131ModifyUserJackAssignAccountDefault() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.RELATIVE);

        XMLGregorianCalendar startTime = clock.currentTimeXMLGregorianCalendar();

        when("user is assigned a default account");
        executeChanges(
                createAccountAssignmentUserDelta(USER_JACK_OID, RESOURCE_DUMMY_OID, null, true),
                null, task, result);

        // THEN
        then();
        assertSuccess(result);
        XMLGregorianCalendar endTime = clock.currentTimeXMLGregorianCalendar();
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after change execution", userJack);
        assertUserJack(userJack);
        assertLiveLinks(userJack, 1);
        accountOid = getSingleLinkOid(userJack);

        // Check shadow
        var accountShadow = getShadowRepo(accountOid);
        assertDummyAccountShadowRepo(accountShadow, accountOid, ACCOUNT_JACK_DUMMY_USERNAME);
        assertEnableTimestampShadow(accountShadow, startTime, endTime);

        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        assertDummyAccountShadowModel(accountModel, accountOid, ACCOUNT_JACK_DUMMY_USERNAME, "Jack Sparrow");
        assertEnableTimestampShadow(accountModel, startTime, endTime);

        // Check account in dummy resource
        assertDefaultDummyAccount("jack", "Jack Sparrow", true);

        // Check audit
        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(3);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(ChangeType.ADD, ShadowType.class);
        dummyAuditService.assertTarget(USER_JACK_OID);
        dummyAuditService.assertExecutionSuccess();

        assertSteadyResources();
    }

    @Test
    public void test132ModifyUserJackAssignAccountTest() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.RELATIVE);

        XMLGregorianCalendar startTime = clock.currentTimeXMLGregorianCalendar();

        when("user is assigned 'test' account");
        executeChanges(
                createAccountAssignmentUserDelta(USER_JACK_OID, RESOURCE_DUMMY_OID, ACCOUNT_INTENT_TEST, true),
                null, task, result);

        // THEN
        then();
        assertSuccess(result);
        XMLGregorianCalendar endTime = clock.currentTimeXMLGregorianCalendar();
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 1);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after change execution", userJack);
        assertUserJack(userJack);
        assertLiveLinks(userJack, 2);
        String accountOidDefault = getLinkRefOid(userJack, RESOURCE_DUMMY_OID, ACCOUNT, SchemaConstants.INTENT_DEFAULT);
        String accountOidTest = getLinkRefOid(userJack, RESOURCE_DUMMY_OID, ACCOUNT, ACCOUNT_INTENT_TEST);

        // Check shadow: intent=default
        var accountShadow = getShadowRepo(accountOidDefault);
        assertDummyAccountShadowRepo(accountShadow, accountOidDefault, ACCOUNT_JACK_DUMMY_USERNAME);

        // Check account: intent=default
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOidDefault, null, task, result);
        assertDummyAccountShadowModel(accountModel, accountOidDefault, ACCOUNT_JACK_DUMMY_USERNAME, "Jack Sparrow");

        // Check account in dummy resource: intent=default
        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, "Jack Sparrow", true);

        // Check shadow: intent=test
        var accountShadowTest = getShadowRepo(accountOidTest);
        assertDummyAccountShadowRepo(accountShadowTest, accountOidTest, "T"+ACCOUNT_JACK_DUMMY_USERNAME);
        assertEnableTimestampShadow(accountShadowTest, startTime, endTime);

        // Check account: intent=test
        PrismObject<ShadowType> accountModelTest = modelService.getObject(ShadowType.class, accountOidTest, null, task, result);
        assertDummyAccountShadowModel(accountModelTest, accountOidTest, "T"+ACCOUNT_JACK_DUMMY_USERNAME, "Jack Sparrow (test)");
        assertEnableTimestampShadow(accountModelTest, startTime, endTime);

        // Check account in dummy resource: intent=test
        assertDefaultDummyAccount("T"+ACCOUNT_JACK_DUMMY_USERNAME, "Jack Sparrow (test)", true);
        assertDefaultDummyGroupMember(GROUP_DUMMY_TESTERS_NAME, "T"+ACCOUNT_JACK_DUMMY_USERNAME);

        // Check audit
        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(3);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(ChangeType.ADD, ShadowType.class);
        dummyAuditService.assertTarget(USER_JACK_OID);
        dummyAuditService.assertExecutionSuccess();

        assertSteadyResources();
    }

    @Test
    public void test135ModifyUserJackFullName() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.RELATIVE);

        // WHEN
        when();
        modifyUserReplace(USER_JACK_OID, UserType.F_FULL_NAME, task, result, PolyString.fromOrig("cpt. Jack Sparrow"));

        // THEN
        then();
        assertSuccess(result);
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 2);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after change execution", userJack);
        assertUserJack(userJack, "cpt. Jack Sparrow", "Jack", "Sparrow");
        assertLiveLinks(userJack, 2);
        String accountOidDefault = getLinkRefOid(userJack, RESOURCE_DUMMY_OID, ACCOUNT, SchemaConstants.INTENT_DEFAULT);
        String accountOidTest = getLinkRefOid(userJack, RESOURCE_DUMMY_OID, ACCOUNT, ACCOUNT_INTENT_TEST);

        // Check shadow: intent=default
        var accountShadow = getShadowRepo(accountOidDefault);
        assertDummyAccountShadowRepo(accountShadow, accountOidDefault, ACCOUNT_JACK_DUMMY_USERNAME);

        // Check account: intent=default
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOidDefault, null, task, result);
        assertDummyAccountShadowModel(accountModel, accountOidDefault, ACCOUNT_JACK_DUMMY_USERNAME, "cpt. Jack Sparrow");

        // Check account in dummy resource: intent=default
        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, "cpt. Jack Sparrow", true);

        // Check shadow: intent=test
        var accountShadowTest = getShadowRepo(accountOidTest);
        assertDummyAccountShadowRepo(accountShadowTest, accountOidTest, "T"+ACCOUNT_JACK_DUMMY_USERNAME);

        // Check account: intent=test
        PrismObject<ShadowType> accountModelTest = modelService.getObject(ShadowType.class, accountOidTest, null, task, result);
        assertDummyAccountShadowModel(accountModelTest, accountOidTest, "T"+ACCOUNT_JACK_DUMMY_USERNAME, "cpt. Jack Sparrow (test)");

        // Check account in dummy resource: intent=test
        assertDefaultDummyAccount("T"+ACCOUNT_JACK_DUMMY_USERNAME, "cpt. Jack Sparrow (test)", true);

        // Check audit
        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(3);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, ShadowType.class);
        dummyAuditService.assertTarget(USER_JACK_OID);
        dummyAuditService.assertExecutionSuccess();

        assertSteadyResources();
    }

    @Test
    public void test147ModifyUserJackUnAssignAccountDefault() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.RELATIVE);

        when("account/default is unassigned");
        executeChanges(
                createAccountAssignmentUserDelta(USER_JACK_OID, RESOURCE_DUMMY_OID, null, false),
                null, task, result);

        // THEN
        then();
        assertSuccess(result);
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 1);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after change execution", userJack);
        assertUserJack(userJack, "cpt. Jack Sparrow", "Jack", "Sparrow");
        assertLiveLinks(userJack, 1);
        String accountOidTest = getLinkRefOid(userJack, RESOURCE_DUMMY_OID, ACCOUNT, ACCOUNT_INTENT_TEST);

        // Check shadow: intent=test
        var accountShadowTest = getShadowRepo(accountOidTest);
        assertDummyAccountShadowRepo(accountShadowTest, accountOidTest, "T"+ACCOUNT_JACK_DUMMY_USERNAME);

        // Check account: intent=test
        PrismObject<ShadowType> accountModelTest = modelService.getObject(ShadowType.class, accountOidTest, null, task, result);
        assertDummyAccountShadowModel(accountModelTest, accountOidTest, "T"+ACCOUNT_JACK_DUMMY_USERNAME, "cpt. Jack Sparrow (test)");

        // Check account in dummy resource: intent=test
        assertDefaultDummyAccount("T"+ACCOUNT_JACK_DUMMY_USERNAME, "cpt. Jack Sparrow (test)", true);

        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);

        // Check audit
        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(3);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(ChangeType.DELETE, ShadowType.class);
        dummyAuditService.assertTarget(USER_JACK_OID);
        dummyAuditService.assertExecutionSuccess();

        assertSteadyResources();
    }

    @Test
    public void test149ModifyUserJackUnassignAccountTest() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.RELATIVE);

        when("unassigning account/test");
        executeChanges(
                createAccountAssignmentUserDelta(USER_JACK_OID, RESOURCE_DUMMY_OID, ACCOUNT_INTENT_TEST, false),
                null, task, result);

        // THEN
        assertSuccess(result);
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        assertUserJack(userJack, "cpt. Jack Sparrow", "Jack", "Sparrow");
        assertLiveLinks(userJack, 0);

        // Check is shadow is gone
        assertNoShadow(accountOid);

        // Check if dummy resource account is gone
        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
        assertNoDummyAccount("T"+ACCOUNT_JACK_DUMMY_USERNAME);

        // Check audit
        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(3);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(ChangeType.DELETE, ShadowType.class);
        dummyAuditService.assertTarget(USER_JACK_OID);
        dummyAuditService.assertExecutionSuccess();

        assertSteadyResources();
    }

    /**
     * Searching for accounts that are defined by classification conditions.
     * (It would be more appropriate to have this test in the provisioning module,
     * but the required script expression evaluator is not available there.)
     *
     * MID-9591
     */
    @Test
    public void test200SearchForAccounts() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        given("accounts are created on the resource");
        createDummyAccountWithPrivilege("100200");
        createDummyAccountWithPrivilege("T100200");
        createDummyAccountWithPrivilege("O100200");

        when("searching for all accounts");
        var accounts = provisioningService.searchObjects(
                ShadowType.class,
                Resource.of(RESOURCE_DUMMY_INTENTS.get())
                        .queryFor(RI_ACCOUNT_OBJECT_CLASS)
                        .build(),
                null, task, result);

        then("three accounts are found");
        assertThat(accounts).hasSize(3);

        when("searching for account/main");
        var main = provisioningService.searchObjects(
                ShadowType.class,
                Resource.of(RESOURCE_DUMMY_INTENTS.get())
                        .queryFor(ShadowKindType.ACCOUNT, "main")
                        .build(),
                null, task, result);

        then("three accounts are found (provisioning does not do filtering here)");
        display("account/main", main);
        assertThat(main).hasSize(3);

        when("searching for account/test");
        var test = provisioningService.searchObjects(
                ShadowType.class,
                Resource.of(RESOURCE_DUMMY_INTENTS.get())
                        .queryFor(ShadowKindType.ACCOUNT, "test")
                        .build(),
                null, task, result);

        then("three accounts are found (provisioning does not do filtering here)");
        display("account/test", test);
        assertThat(test).hasSize(3);
    }

    /** MID-9591 */
    @Test
    public void test210ReconcileAllAccounts() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        String main100210 = "100210";
        String test100210 = "T100210";
        String other100210 = "O100210";

        given("accounts are created on the resource");
        createDummyAccountWithPrivilege(main100210);
        createDummyAccountWithPrivilege(test100210);
        createDummyAccountWithPrivilege(other100210);

        when("all accounts are reconciled");
        reconcileAccountsRequest()
                .withResourceOid(RESOURCE_DUMMY_INTENTS.oid)
                .withWholeObjectClass(RI_ACCOUNT_OBJECT_CLASS)
                .withProcessingAllAccounts()
                .execute(result);

        then("main + test users are there");
        assertUserAfterByUsername(main100210);
        assertUserAfterByUsername(test100210);
        assertNoObjectByName(UserType.class, other100210, task, result);
    }

    private void createDummyAccountWithPrivilege(String name) throws Exception {
        var account = RESOURCE_DUMMY_INTENTS.controller.addAccount(name);
        account.addAttributeValues(DummyResourceContoller.DUMMY_ENTITLEMENT_PRIVILEGE_NAME, guestPrivilege.getName());
    }

    /** MID-9591 */
    @Test
    public void test220ReconcileIntentMain() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        String main100220 = "100220";
        String test100220 = "T100220";
        String other100220 = "O100220";

        given("accounts are created on the resource");
        createDummyAccountWithPrivilege(main100220);
        createDummyAccountWithPrivilege(test100220);
        createDummyAccountWithPrivilege(other100220);

        when("account/main is reconciled");
        reconcileAccountsRequest()
                .withResourceOid(RESOURCE_DUMMY_INTENTS.oid)
                .withTypeIdentification(ResourceObjectTypeIdentification.of(ACCOUNT, "main"))
                .withProcessingAllAccounts()
                .execute(result);

        then("only the main user is there");
        assertUserAfterByUsername(main100220);
        assertNoObjectByName(UserType.class, test100220, task, result);
        assertNoObjectByName(UserType.class, other100220, task, result);
    }

    /** MID-9591 */
    @Test
    public void test230ReconcileIntentTest() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        String main100230 = "100230";
        String test100230 = "T100230";
        String other100230 = "O100230";

        given("accounts are created on the resource");
        createDummyAccountWithPrivilege(main100230);
        createDummyAccountWithPrivilege(test100230);
        createDummyAccountWithPrivilege(other100230);

        when("account/test is reconciled");
        reconcileAccountsRequest()
                .withResourceOid(RESOURCE_DUMMY_INTENTS.oid)
                .withTypeIdentification(ResourceObjectTypeIdentification.of(ACCOUNT, "test"))
                .withProcessingAllAccounts()
                .execute(result);

        then("only the test user is there");
        assertNoObjectByName(UserType.class, main100230, task, result);
        assertUserAfterByUsername(test100230);
        assertNoObjectByName(UserType.class, other100230, task, result);
    }

    @SuppressWarnings("SameParameterValue")
    private void preTestCleanup(AssignmentPolicyEnforcementType enforcementPolicy) throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
        assumeAssignmentPolicy(enforcementPolicy);
        dummyAuditService.clear();
        prepareNotifications();
        rememberCounter(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);
    }
}
