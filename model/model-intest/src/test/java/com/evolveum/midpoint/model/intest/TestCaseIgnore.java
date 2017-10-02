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

import com.evolveum.icf.dummy.resource.DummyGroup;
import com.evolveum.midpoint.model.intest.rbac.TestRbac;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.match.MatchingRule;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.match.StringIgnoreCaseMatchingRule;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.internals.InternalOperationClasses;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.assertEquals;

/**
 * Test various case ignore and case transformation scenarios.
 *
 * @author semancik
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestCaseIgnore extends AbstractInitializedModelIntegrationTest {

	public static final File TEST_DIR = new File("src/test/resources/caseignore");

	protected static final String ACCOUNT_JACK_DUMMY_UPCASE_NAME = "JACK";
	protected static final String ACCOUNT_GUYBRUSH_DUMMY_UPCASE_NAME = "GUYBRUSH";

	protected static final File ROLE_X_FILE = new File(TEST_DIR, "role-x.xml");
	protected static final String ROLE_X_OID = "ef7edff4-813c-11e4-b893-3c970e467874";

	protected static final File ROLE_JOKER_FILE = new File(TEST_DIR, "role-joker.xml");
	protected static final String ROLE_JOKER_OID = "0a736ff6-9ca8-11e4-b820-001e8c717e5b";

	protected static final File ROLE_UPCASE_BASIC_FILE = new File(TEST_DIR, "role-upcase-basic.xml");
	protected static final String ROLE_UPCASE_BASIC_OID = "008a071a-9cc2-11e4-913d-001e8c717e5b";

	protected static final File ROLE_FOOL_FILE = new File(TEST_DIR, "role-fool.xml");
	protected static final String ROLE_FOOL_OID = "97c24f16-e082-11e5-be34-13ace5aaad31";

	private static final String GROUP_DUMMY_FOOLS_NAME = "FoOlS";

	@Autowired(required = true)
	protected MatchingRuleRegistry matchingRuleRegistry;

	private MatchingRule<String> caseIgnoreMatchingRule;

	private static String accountOid;

	@Override
	public void initSystem(Task initTask, OperationResult initResult)
			throws Exception {
		super.initSystem(initTask, initResult);
		caseIgnoreMatchingRule = matchingRuleRegistry.getMatchingRule(StringIgnoreCaseMatchingRule.NAME, DOMUtil.XSD_STRING);
        preTestCleanup(AssignmentPolicyEnforcementType.FULL);

        repoAddObjectFromFile(ROLE_X_FILE, initResult);
        repoAddObjectFromFile(ROLE_JOKER_FILE, initResult);
        repoAddObjectFromFile(ROLE_UPCASE_BASIC_FILE, initResult);
        repoAddObjectFromFile(ROLE_FOOL_FILE, initResult);

		InternalMonitor.reset();
		InternalMonitor.setTrace(InternalOperationClasses.SHADOW_FETCH_OPERATIONS, false);
		InternalMonitor.setTrace(InternalOperationClasses.RESOURCE_SCHEMA_OPERATIONS, false);
	}

	@Test
    public void test000Sanity() throws Exception {
		final String TEST_NAME="test000Sanity";
        displayTestTitle(TEST_NAME);

        assertShadows(5);
	}

	@Test
    public void test131ModifyUserJackAssignAccount() throws Exception {
		final String TEST_NAME="test131ModifyUserJackAssignAccount";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestCaseIgnore.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(USER_JACK_OID, RESOURCE_DUMMY_UPCASE_OID, null, true);
        deltas.add(accountAssignmentUserDelta);

        XMLGregorianCalendar startTime = clock.currentTimeXMLGregorianCalendar();

		// WHEN
       displayWhen(TEST_NAME);
		modelService.executeChanges(deltas, null, task, result);

		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
        TestUtil.assertSuccess("executeChanges result", result);
        XMLGregorianCalendar endTime = clock.currentTimeXMLGregorianCalendar();
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);

		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack);
        accountOid = getSingleLinkOid(userJack);

		// Check shadow
        rememberCounter(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        display("Repo shadow", accountShadow);
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);
        assertAccountShadowRepo(accountShadow, accountOid, "jack", resourceDummyUpcaseType, caseIgnoreMatchingRule);
        assertEnableTimestampShadow(accountShadow, startTime, endTime);

        // Check account
        rememberCounter(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        display("Model shadow", accountModel);
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 1);
        assertAccountShadowModel(accountModel, accountOid, ACCOUNT_JACK_DUMMY_UPCASE_NAME, resourceDummyUpcaseType, caseIgnoreMatchingRule);
        assertEnableTimestampShadow(accountModel, startTime, endTime);

        // Check account in dummy resource
        assertDummyAccount(RESOURCE_DUMMY_UPCASE_NAME, "jack", "Jack Sparrow", true);

        assertShadows(6);

        assertSteadyResources();
    }

	@Test
    public void test133SeachAccountShadows() throws Exception {
		final String TEST_NAME="test133SeachAccountShadows";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestCaseIgnore.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        ObjectQuery query = ObjectQueryUtil.createResourceAndObjectClassQuery(RESOURCE_DUMMY_UPCASE_OID,
        		new QName(ResourceTypeUtil.getResourceNamespace(resourceDummyUpcaseType),
                SchemaConstants.ACCOUNT_OBJECT_CLASS_LOCAL_NAME), prismContext);
        rememberCounter(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);

		// WHEN
       displayWhen(TEST_NAME);
        SearchResultList<PrismObject<ShadowType>> foundShadows = modelService.searchObjects(ShadowType.class, query, null, task, result);

		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
        TestUtil.assertSuccess(result);

        display("Shadows", foundShadows);
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 1);
        assertEquals("Wrong number of shadows found", 1, foundShadows.size());
        PrismObject<ShadowType> foundShadow = foundShadows.get(0);
        assertAccountShadowModel(foundShadow, accountOid, ACCOUNT_JACK_DUMMY_UPCASE_NAME, resourceDummyUpcaseType, caseIgnoreMatchingRule);

		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack);
        accountOid = getSingleLinkOid(userJack);

		// Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        display("Repo shadow", accountShadow);
        assertAccountShadowRepo(accountShadow, accountOid, ACCOUNT_JACK_DUMMY_UPCASE_NAME, resourceDummyUpcaseType, caseIgnoreMatchingRule);

        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        display("Model shadow", accountModel);
        assertAccountShadowModel(accountModel, accountOid, ACCOUNT_JACK_DUMMY_UPCASE_NAME, resourceDummyUpcaseType, caseIgnoreMatchingRule);

        // Check account in dummy resource
        assertDummyAccount(RESOURCE_DUMMY_UPCASE_NAME, ACCOUNT_JACK_DUMMY_UPCASE_NAME, "Jack Sparrow", true);

        assertShadows(6);

        assertSteadyResources();
    }

	// TODO: searchGroupShadows

	@Test
    public void test139ModifyUserJackUnassignAccount() throws Exception {
		final String TEST_NAME = "test139ModifyUserJackUnassignAccount";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestCaseIgnore.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        rememberCounter(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);

        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(USER_JACK_OID, RESOURCE_DUMMY_UPCASE_OID, null, false);
        deltas.add(accountAssignmentUserDelta);

		// WHEN
		modelService.executeChanges(deltas, null, task, result);

		// THEN
		result.computeStatus();
        TestUtil.assertSuccess("executeChanges result", result);
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);

		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		assertUserJack(userJack, "Jack Sparrow", "Jack", "Sparrow");
		// Check accountRef
        assertUserNoAccountRefs(userJack);

        // Check is shadow is gone
        assertNoShadow(accountOid);

        // Check if dummy resource account is gone
        assertNoDummyAccount(RESOURCE_DUMMY_UPCASE_NAME, "jack");
        assertNoDummyAccount(RESOURCE_DUMMY_UPCASE_NAME, ACCOUNT_JACK_DUMMY_UPCASE_NAME);

        assertShadows(5);

        assertSteadyResources();
    }


	@Test
    public void test150JackAssignRoleX() throws Exception {
		final String TEST_NAME = "test150JackAssignRoleX";
        displayTestTitle(TEST_NAME);

        Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);

        // WHEN
        assignRole(USER_JACK_OID, ROLE_X_OID, task, result);

        // THEN
       displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        // Make sure this is repository so we do not destroy the "evidence" yet.
        PrismObject<UserType> userJack = repositoryService.getObject(UserType.class, USER_JACK_OID, null, result);

		display("User after change execution", userJack);
		assertUserJack(userJack);
        assertAssignedRole(userJack, ROLE_X_OID);
        accountOid = getSingleLinkOid(userJack);

		// Check shadow
        rememberCounter(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        display("Repo shadow", accountShadow);
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);
        assertAccountShadowRepo(accountShadow, accountOid, "X-jack", resourceDummyUpcaseType, caseIgnoreMatchingRule);

        // Check account
        rememberCounter(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        display("Model shadow", accountModel);
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 1);
        assertAccountShadowModel(accountModel, accountOid, "X-"+ACCOUNT_JACK_DUMMY_UPCASE_NAME, resourceDummyUpcaseType, caseIgnoreMatchingRule);

        assertDummyAccount(RESOURCE_DUMMY_UPCASE_NAME, "X-"+ACCOUNT_JACK_DUMMY_UPCASE_NAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDummyAccountAttribute(RESOURCE_DUMMY_UPCASE_NAME, "X-"+ACCOUNT_JACK_DUMMY_UPCASE_NAME, "title", "XXX");

        assertShadows(6);
	}

	@Test
    public void test152GetJack() throws Exception {
		final String TEST_NAME = "test152GetJack";
        displayTestTitle(TEST_NAME);

        Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        PrismObject<UserType> userJack = modelService.getObject(UserType.class, USER_JACK_OID, null, task, result);

        // THEN
       displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

		display("User", userJack);
		assertUserJack(userJack);
        assertAssignedRole(userJack, ROLE_X_OID);
        String accountOidAfter = getSingleLinkOid(userJack);
        assertEquals("Account OID has changed", accountOid, accountOidAfter);

		// Check shadow
        rememberCounter(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        display("Repo shadow", accountShadow);
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);
        assertAccountShadowRepo(accountShadow, accountOid, "X-JACK", resourceDummyUpcaseType, caseIgnoreMatchingRule);

        // Check account
        rememberCounter(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        display("Model shadow", accountModel);
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 1);
        assertAccountShadowModel(accountModel, accountOid, "X-JACK", resourceDummyUpcaseType, caseIgnoreMatchingRule);

        assertDummyAccount(RESOURCE_DUMMY_UPCASE_NAME, "X-"+ACCOUNT_JACK_DUMMY_UPCASE_NAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDummyAccountAttribute(RESOURCE_DUMMY_UPCASE_NAME, "X-"+ACCOUNT_JACK_DUMMY_UPCASE_NAME, "title", "XXX");

        assertShadows(6);
	}

	@Test
    public void test159JackUnAssignRoleX() throws Exception {
		final String TEST_NAME = "test159JackUnAssignRoleX";
        displayTestTitle(TEST_NAME);

        Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);

        // WHEN
        unassignRole(USER_JACK_OID, ROLE_X_OID, task, result);

        // THEN
       displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        // Make sure this is repository so we do not destroy the "evidence" yet.
        PrismObject<UserType> userJack = repositoryService.getObject(UserType.class, USER_JACK_OID, null, result);

		display("User after change execution", userJack);
		assertUserJack(userJack);
		assertNoAssignments(userJack);
        assertLinks(userJack, 0);

        assertNoDummyAccount(RESOURCE_DUMMY_UPCASE_NAME, "X-"+ACCOUNT_JACK_DUMMY_UPCASE_NAME);

        assertShadows(5);
	}

	@Test
    public void test160JackAssignRoleBasic() throws Exception {
		final String TEST_NAME = "test160JackAssignRoleBasic";
        displayTestTitle(TEST_NAME);

        Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);

        // WHEN
       displayWhen(TEST_NAME);
        assignRole(USER_JACK_OID, ROLE_UPCASE_BASIC_OID, task, result);

        // THEN
       displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        // Make sure this is repository so we do not destroy the "evidence" yet.
        PrismObject<UserType> userJack = repositoryService.getObject(UserType.class, USER_JACK_OID, null, result);

		display("User after change execution", userJack);
		assertUserJack(userJack);
        assertAssignedRole(userJack, ROLE_UPCASE_BASIC_OID);
        accountOid = getSingleLinkOid(userJack);

		// Check shadow
        rememberCounter(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        display("Repo shadow", accountShadow);
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);
        assertAccountShadowRepo(accountShadow, accountOid, ACCOUNT_JACK_DUMMY_USERNAME, resourceDummyUpcaseType, caseIgnoreMatchingRule);

        // Check account
        rememberCounter(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        display("Model shadow", accountModel);
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 1);
        assertAccountShadowModel(accountModel, accountOid, ACCOUNT_JACK_DUMMY_UPCASE_NAME, resourceDummyUpcaseType, caseIgnoreMatchingRule);

        assertDummyAccount(RESOURCE_DUMMY_UPCASE_NAME, ACCOUNT_JACK_DUMMY_UPCASE_NAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertNoDummyAccountAttribute(RESOURCE_DUMMY_UPCASE_NAME, ACCOUNT_JACK_DUMMY_UPCASE_NAME, "title");
        assertNoDummyGroupMember(RESOURCE_DUMMY_UPCASE_NAME, GROUP_JOKER_DUMMY_UPCASE_NAME, ACCOUNT_JACK_DUMMY_UPCASE_NAME);

        assertShadows(6);
	}

	@Test
    public void test161JackAssignRoleJoker() throws Exception {
		final String TEST_NAME = "test161JackAssignRoleJoker";
        displayTestTitle(TEST_NAME);

        Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);

        // WHEN
       displayWhen(TEST_NAME);
        assignRole(USER_JACK_OID, ROLE_JOKER_OID, task, result);

        // THEN
       displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        // Make sure this is repository so we do not destroy the "evidence" yet.
        PrismObject<UserType> userJack = repositoryService.getObject(UserType.class, USER_JACK_OID, null, result);

		display("User after change execution", userJack);
		assertUserJack(userJack);
        assertAssignedRole(userJack, ROLE_JOKER_OID);
        assertAssignedRole(userJack, ROLE_UPCASE_BASIC_OID);
        accountOid = getSingleLinkOid(userJack);

		// Check shadow
        rememberCounter(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);
        PrismObject<ShadowType> accountRepoShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        display("Repo shadow", accountRepoShadow);
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);
        assertAccountShadowRepo(accountRepoShadow, accountOid, ACCOUNT_JACK_DUMMY_UPCASE_NAME, resourceDummyUpcaseType, caseIgnoreMatchingRule);

        // Check account
        rememberCounter(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);
        PrismObject<ShadowType> accountModelShadow = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        display("Model shadow", accountModelShadow);
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 1);
        assertAccountShadowModel(accountModelShadow, accountOid, ACCOUNT_JACK_DUMMY_UPCASE_NAME, resourceDummyUpcaseType, caseIgnoreMatchingRule);

        assertDummyAccount(RESOURCE_DUMMY_UPCASE_NAME, ACCOUNT_JACK_DUMMY_UPCASE_NAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDummyAccountAttribute(RESOURCE_DUMMY_UPCASE_NAME, ACCOUNT_JACK_DUMMY_UPCASE_NAME, "title", "JoKeR");
        assertDummyGroupMember(RESOURCE_DUMMY_UPCASE_NAME, GROUP_JOKER_DUMMY_UPCASE_NAME, ACCOUNT_JACK_DUMMY_UPCASE_NAME);

        IntegrationTestTools.assertAssociation(accountModelShadow, RESOURCE_DUMMY_UPCASE_ASSOCIATION_GROUP_QNAME,
        		GROUP_SHADOW_JOKER_DUMMY_UPCASE_OID);

        assertShadows(6);
	}

	@Test
    public void test165JackUnAssignRoleJoker() throws Exception {
		final String TEST_NAME = "test165JackUnAssignRoleJoker";
        displayTestTitle(TEST_NAME);

        Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);

        // WHEN
       displayWhen(TEST_NAME);
        unassignRole(USER_JACK_OID, ROLE_JOKER_OID, task, result);

        // THEN
       displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        // Make sure this is repository so we do not destroy the "evidence" yet.
        PrismObject<UserType> userJack = repositoryService.getObject(UserType.class, USER_JACK_OID, null, result);

		display("User after change execution", userJack);
		assertUserJack(userJack);
        assertAssignedRole(userJack, ROLE_UPCASE_BASIC_OID);
        accountOid = getSingleLinkOid(userJack);

		// Check shadow
        rememberCounter(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        display("Repo shadow", accountShadow);
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);
        assertAccountShadowRepo(accountShadow, accountOid, ACCOUNT_JACK_DUMMY_UPCASE_NAME, resourceDummyUpcaseType, caseIgnoreMatchingRule);

        // Check account
        rememberCounter(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        display("Model shadow", accountModel);
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 1);
        assertAccountShadowModel(accountModel, accountOid, ACCOUNT_JACK_DUMMY_UPCASE_NAME, resourceDummyUpcaseType, caseIgnoreMatchingRule);

        assertDummyAccount(RESOURCE_DUMMY_UPCASE_NAME, ACCOUNT_JACK_DUMMY_UPCASE_NAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertNoDummyAccountAttribute(RESOURCE_DUMMY_UPCASE_NAME, ACCOUNT_JACK_DUMMY_UPCASE_NAME, "title");
        assertNoDummyGroupMember(RESOURCE_DUMMY_UPCASE_NAME, GROUP_JOKER_DUMMY_UPCASE_NAME, ACCOUNT_JACK_DUMMY_UPCASE_NAME);

        assertShadows(6);
	}

	@Test
    public void test169JackUnAssignRoleBasic() throws Exception {
		final String TEST_NAME = "test169JackUnAssignRoleBasic";
        displayTestTitle(TEST_NAME);

        Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);

        // WHEN
       displayWhen(TEST_NAME);
        unassignRole(USER_JACK_OID, ROLE_UPCASE_BASIC_OID, task, result);

        // THEN
       displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        // Make sure this is repository so we do not destroy the "evidence" yet.
        PrismObject<UserType> userJack = repositoryService.getObject(UserType.class, USER_JACK_OID, null, result);

		display("User after change execution", userJack);
		assertUserJack(userJack);
		assertUserJack(userJack);
		assertNoAssignments(userJack);
        assertLinks(userJack, 0);

        assertNoDummyAccount(RESOURCE_DUMMY_UPCASE_NAME, ACCOUNT_JACK_DUMMY_UPCASE_NAME);
        assertNoDummyGroupMember(RESOURCE_DUMMY_UPCASE_NAME, GROUP_JOKER_DUMMY_UPCASE_NAME, ACCOUNT_JACK_DUMMY_UPCASE_NAME);

        assertShadows(5);
	}

	@Test
    public void test170JackAssignRoleJoker() throws Exception {
		final String TEST_NAME = "test170JackAssignRoleJoker";
        displayTestTitle(TEST_NAME);

        Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);

        // WHEN
        displayWhen(TEST_NAME);
        assignRole(USER_JACK_OID, ROLE_JOKER_OID, task, result);

        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

        // Make sure this is repository so we do not destroy the "evidence" yet.
        PrismObject<UserType> userJack = repositoryService.getObject(UserType.class, USER_JACK_OID, null, result);

		display("User after change execution", userJack);
		assertUserJack(userJack);
        assertAssignedRole(userJack, ROLE_JOKER_OID);
        accountOid = getSingleLinkOid(userJack);

		// Check shadow
        rememberCounter(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        display("Repo shadow", accountShadow);
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);
        assertAccountShadowRepo(accountShadow, accountOid, ACCOUNT_JACK_DUMMY_USERNAME, resourceDummyUpcaseType, caseIgnoreMatchingRule);

        // Check account
        rememberCounter(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        display("Model shadow", accountModel);
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 1);
        assertAccountShadowModel(accountModel, accountOid, ACCOUNT_JACK_DUMMY_UPCASE_NAME, resourceDummyUpcaseType, caseIgnoreMatchingRule);

        assertDummyAccount(RESOURCE_DUMMY_UPCASE_NAME, ACCOUNT_JACK_DUMMY_UPCASE_NAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDummyAccountAttribute(RESOURCE_DUMMY_UPCASE_NAME, ACCOUNT_JACK_DUMMY_UPCASE_NAME, "title", "JoKeR");
        assertDummyGroupMember(RESOURCE_DUMMY_UPCASE_NAME, GROUP_JOKER_DUMMY_UPCASE_NAME, ACCOUNT_JACK_DUMMY_UPCASE_NAME);

        assertShadows(6);
	}

	@Test
    public void test179JackUnAssignRoleJoker() throws Exception {
		final String TEST_NAME = "test179JackUnAssignRoleJoker";
        displayTestTitle(TEST_NAME);

        Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);

        // WHEN
        displayWhen(TEST_NAME);
        unassignRole(USER_JACK_OID, ROLE_JOKER_OID, task, result);

        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

        // Make sure this is repository so we do not destroy the "evidence" yet.
        PrismObject<UserType> userJack = repositoryService.getObject(UserType.class, USER_JACK_OID, null, result);

		display("User after change execution", userJack);
		assertUserJack(userJack);
		assertUserJack(userJack);
		assertNoAssignments(userJack);
        assertLinks(userJack, 0);

        assertNoDummyAccount(RESOURCE_DUMMY_UPCASE_NAME, ACCOUNT_JACK_DUMMY_UPCASE_NAME);
        // MID-2147
        assertNoDummyGroupMember(RESOURCE_DUMMY_UPCASE_NAME, GROUP_JOKER_DUMMY_UPCASE_NAME, ACCOUNT_JACK_DUMMY_UPCASE_NAME);

        assertShadows(5);
	}


	/**
	 * Create group no resource in such a way that midpoint does not know about it.
	 * The assign a role that refers to this group by using associationTargetSearch.
	 * The group shadow has to be created in midPoint and it should have the correct
	 * kind/intent. Otherwise the shadow will not have proper matching rules and the
	 * identifiers in the shadow will not be normalized. This may lead to shadow duplication.
	 */
	@Test
    public void test200GuybrushAssignRoleFools() throws Exception {
		final String TEST_NAME = "test200GuybrushAssignRoleFools";
        displayTestTitle(TEST_NAME);

        Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        assertShadows(5);
        assertNoDummyAccount(RESOURCE_DUMMY_UPCASE_NAME, ACCOUNT_GUYBRUSH_DUMMY_UPCASE_NAME);

        DummyGroup dummyGroupFools = new DummyGroup(GROUP_DUMMY_FOOLS_NAME);
		dummyResourceUpcase.addGroup(dummyGroupFools);

		recomputeUser(USER_GUYBRUSH_OID, task, result);

		assertShadows(4);

        PrismObject<UserType> userBefore = getUser(USER_GUYBRUSH_OID);
        display("User before", userBefore);

		assertShadows(4);

        // WHEN
		displayWhen(TEST_NAME);
        assignRole(USER_GUYBRUSH_OID, ROLE_FOOL_OID, task, result);

        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

        assertShadows(6);

        // Make sure this is repository so we do not destroy the "evidence" yet.
        PrismObject<UserType> userAfter = repositoryService.getObject(UserType.class, USER_GUYBRUSH_OID, null, result);

		display("User after change execution", userAfter);
        assertAssignedRole(userAfter, ROLE_FOOL_OID);
        accountOid = getSingleLinkOid(userAfter);

        assertShadows(6);

		// Check shadow
        rememberCounter(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        display("Repo shadow", accountShadow);
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);
        assertAccountShadowRepo(accountShadow, accountOid, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, resourceDummyUpcaseType, caseIgnoreMatchingRule);

        assertShadows(6);

        // Check account
        rememberCounter(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        display("Model shadow", accountModel);
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 1);
        assertAccountShadowModel(accountModel, accountOid, ACCOUNT_GUYBRUSH_DUMMY_UPCASE_NAME, resourceDummyUpcaseType, caseIgnoreMatchingRule);

//        assertShadows(6);

        assertDummyAccount(RESOURCE_DUMMY_UPCASE_NAME, ACCOUNT_GUYBRUSH_DUMMY_UPCASE_NAME, ACCOUNT_GUYBRUSH_DUMMY_FULLNAME, true);
        assertDummyAccountAttribute(RESOURCE_DUMMY_UPCASE_NAME, ACCOUNT_GUYBRUSH_DUMMY_UPCASE_NAME, "title", "FOOL!");
        assertDummyGroupMember(RESOURCE_DUMMY_UPCASE_NAME, GROUP_DUMMY_FOOLS_NAME, ACCOUNT_GUYBRUSH_DUMMY_UPCASE_NAME);

        assertEquals(1, accountModel.asObjectable().getAssociation().size());
        ObjectReferenceType shadowRef = accountModel.asObjectable().getAssociation().get(0).getShadowRef();
        PrismObject<ShadowType> groupFoolsRepoShadow = repositoryService.getObject(ShadowType.class, shadowRef.getOid(), null, result);
        display("group fools repo shadow", groupFoolsRepoShadow);

        PrismAsserts.assertPropertyValue(groupFoolsRepoShadow, new ItemPath(ShadowType.F_ATTRIBUTES, SchemaConstants.ICFS_NAME),
        		GROUP_DUMMY_FOOLS_NAME.toLowerCase());
        PrismAsserts.assertPropertyValue(groupFoolsRepoShadow, new ItemPath(ShadowType.F_ATTRIBUTES, SchemaConstants.ICFS_UID),
        		GROUP_DUMMY_FOOLS_NAME.toLowerCase());
        assertShadowKindIntent(groupFoolsRepoShadow, ShadowKindType.ENTITLEMENT, INTENT_DUMMY_GROUP);

        assertShadows(6);

	}

	private void preTestCleanup(AssignmentPolicyEnforcementType enforcementPolicy) throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
		assumeAssignmentPolicy(enforcementPolicy);
        dummyAuditService.clear();
        prepareNotifications();
        purgeProvisioningScriptHistory();
        rememberCounter(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);
	}

}
