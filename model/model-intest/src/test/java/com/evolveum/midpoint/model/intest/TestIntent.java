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

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;

/**
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestIntent extends AbstractInitializedModelIntegrationTest {

	public static final File TEST_DIR = new File("src/test/resources/xxxxxxxxxx");
	private static final String ACCOUNT_INTENT_TEST = "test";
	private String accountOid;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        addObject(SHADOW_GROUP_DUMMY_TESTERS_FILE, initTask, initResult);

        rememberSteadyResources();
    }

    @Test
    public void test131ModifyUserJackAssignAccountDefault() throws Exception {
		final String TEST_NAME="test131ModifyUserJackAssignAccountDefault";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.RELATIVE);

        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<>();
        ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(USER_JACK_OID,
        		RESOURCE_DUMMY_OID, null, true);
        deltas.add(accountAssignmentUserDelta);

        XMLGregorianCalendar startTime = clock.currentTimeXMLGregorianCalendar();

		// WHEN
        displayWhen(TEST_NAME);
		modelService.executeChanges(deltas, null, task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);
        XMLGregorianCalendar endTime = clock.currentTimeXMLGregorianCalendar();
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);

		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack);
		assertLinks(userJack, 1);
        accountOid = getSingleLinkOid(userJack);

		// Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        assertDummyAccountShadowRepo(accountShadow, accountOid, ACCOUNT_JACK_DUMMY_USERNAME);
        assertEnableTimestampShadow(accountShadow, startTime, endTime);

        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        assertDummyAccountShadowModel(accountModel, accountOid, ACCOUNT_JACK_DUMMY_USERNAME, "Jack Sparrow");
        assertEnableTimestampShadow(accountModel, startTime, endTime);

        // Check account in dummy resource
        assertDefaultDummyAccount("jack", "Jack Sparrow", true);

        // Check audit
        display("Audit", dummyAuditService);
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
		final String TEST_NAME="test132ModifyUserJackAssignAccountTest";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.RELATIVE);

        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<>();
        ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(USER_JACK_OID,
        		RESOURCE_DUMMY_OID, ACCOUNT_INTENT_TEST, true);
        deltas.add(accountAssignmentUserDelta);

        XMLGregorianCalendar startTime = clock.currentTimeXMLGregorianCalendar();

		// WHEN
        displayWhen(TEST_NAME);
		modelService.executeChanges(deltas, null, task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);
        XMLGregorianCalendar endTime = clock.currentTimeXMLGregorianCalendar();
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 1);

		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack);
		assertLinks(userJack, 2);
		String accountOidDefault = getLinkRefOid(userJack, RESOURCE_DUMMY_OID, ShadowKindType.ACCOUNT, SchemaConstants.INTENT_DEFAULT);
		String accountOidTest = getLinkRefOid(userJack, RESOURCE_DUMMY_OID, ShadowKindType.ACCOUNT, ACCOUNT_INTENT_TEST);

		// Check shadow: intent=default
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOidDefault, null, result);
        assertDummyAccountShadowRepo(accountShadow, accountOidDefault, ACCOUNT_JACK_DUMMY_USERNAME);

        // Check account: intent=default
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOidDefault, null, task, result);
        assertDummyAccountShadowModel(accountModel, accountOidDefault, ACCOUNT_JACK_DUMMY_USERNAME, "Jack Sparrow");

        // Check account in dummy resource: intent=default
        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, "Jack Sparrow", true);

        // Check shadow: intent=test
        PrismObject<ShadowType> accountShadowTest = repositoryService.getObject(ShadowType.class, accountOidTest, null, result);
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
        display("Audit", dummyAuditService);
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
		final String TEST_NAME="test135ModifyUserJackFullName";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.RELATIVE);

		// WHEN
        displayWhen(TEST_NAME);
        modifyUserReplace(USER_JACK_OID, UserType.F_FULL_NAME, task, result, PrismTestUtil.createPolyString("cpt. Jack Sparrow"));

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 2);

		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack, "cpt. Jack Sparrow", "Jack", "Sparrow");
		assertLinks(userJack, 2);
		String accountOidDefault = getLinkRefOid(userJack, RESOURCE_DUMMY_OID, ShadowKindType.ACCOUNT, SchemaConstants.INTENT_DEFAULT);
		String accountOidTest = getLinkRefOid(userJack, RESOURCE_DUMMY_OID, ShadowKindType.ACCOUNT, ACCOUNT_INTENT_TEST);

		// Check shadow: intent=default
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOidDefault, null, result);
        assertDummyAccountShadowRepo(accountShadow, accountOidDefault, ACCOUNT_JACK_DUMMY_USERNAME);

        // Check account: intent=default
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOidDefault, null, task, result);
        assertDummyAccountShadowModel(accountModel, accountOidDefault, ACCOUNT_JACK_DUMMY_USERNAME, "cpt. Jack Sparrow");

        // Check account in dummy resource: intent=default
        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, "cpt. Jack Sparrow", true);

        // Check shadow: intent=test
        PrismObject<ShadowType> accountShadowTest = repositoryService.getObject(ShadowType.class, accountOidTest, null, result);
        assertDummyAccountShadowRepo(accountShadowTest, accountOidTest, "T"+ACCOUNT_JACK_DUMMY_USERNAME);

        // Check account: intent=test
        PrismObject<ShadowType> accountModelTest = modelService.getObject(ShadowType.class, accountOidTest, null, task, result);
        assertDummyAccountShadowModel(accountModelTest, accountOidTest, "T"+ACCOUNT_JACK_DUMMY_USERNAME, "cpt. Jack Sparrow (test)");

        // Check account in dummy resource: intent=test
        assertDefaultDummyAccount("T"+ACCOUNT_JACK_DUMMY_USERNAME, "cpt. Jack Sparrow (test)", true);

        // Check audit
        display("Audit", dummyAuditService);
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
		final String TEST_NAME="test147ModifyUserJackUnAssignAccountDefault";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.RELATIVE);

        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<>();
        ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(USER_JACK_OID,
        		RESOURCE_DUMMY_OID, null, false);
        deltas.add(accountAssignmentUserDelta);

        XMLGregorianCalendar startTime = clock.currentTimeXMLGregorianCalendar();

		// WHEN
        displayWhen(TEST_NAME);
		modelService.executeChanges(deltas, null, task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);
        XMLGregorianCalendar endTime = clock.currentTimeXMLGregorianCalendar();
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 1);

		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack, "cpt. Jack Sparrow", "Jack", "Sparrow");
		assertLinks(userJack, 1);
		String accountOidTest = getLinkRefOid(userJack, RESOURCE_DUMMY_OID, ShadowKindType.ACCOUNT, ACCOUNT_INTENT_TEST);

        // Check shadow: intent=test
        PrismObject<ShadowType> accountShadowTest = repositoryService.getObject(ShadowType.class, accountOidTest, null, result);
        assertDummyAccountShadowRepo(accountShadowTest, accountOidTest, "T"+ACCOUNT_JACK_DUMMY_USERNAME);

        // Check account: intent=test
        PrismObject<ShadowType> accountModelTest = modelService.getObject(ShadowType.class, accountOidTest, null, task, result);
        assertDummyAccountShadowModel(accountModelTest, accountOidTest, "T"+ACCOUNT_JACK_DUMMY_USERNAME, "cpt. Jack Sparrow (test)");

        // Check account in dummy resource: intent=test
        assertDefaultDummyAccount("T"+ACCOUNT_JACK_DUMMY_USERNAME, "cpt. Jack Sparrow (test)", true);

        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);

        // Check audit
        display("Audit", dummyAuditService);
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
		final String TEST_NAME = "test149ModifyUserJackUnassignAccountTest";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.RELATIVE);

        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<>();
        ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(USER_JACK_OID,
        		RESOURCE_DUMMY_OID, ACCOUNT_INTENT_TEST, false);
        deltas.add(accountAssignmentUserDelta);

		// WHEN
		modelService.executeChanges(deltas, null, task, result);

		// THEN
		assertSuccess(result);
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);

		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		assertUserJack(userJack, "cpt. Jack Sparrow", "Jack", "Sparrow");
		assertLinks(userJack, 0);

        // Check is shadow is gone
        assertNoShadow(accountOid);

        // Check if dummy resource account is gone
        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
        assertNoDummyAccount("T"+ACCOUNT_JACK_DUMMY_USERNAME);

        // Check audit
        display("Audit", dummyAuditService);
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

    private void preTestCleanup(AssignmentPolicyEnforcementType enforcementPolicy) throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
		assumeAssignmentPolicy(enforcementPolicy);
        dummyAuditService.clear();
        prepareNotifications();
        rememberCounter(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);
	}
}
