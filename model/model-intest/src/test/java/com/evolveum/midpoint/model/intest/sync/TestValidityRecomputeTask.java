/*
 * Copyright (c) 2013-2017 Evolveum
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
package com.evolveum.midpoint.model.intest.sync;

import static org.testng.AssertJUnit.assertEquals;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.impl.trigger.RecomputeTriggerHandler;
import com.evolveum.midpoint.model.intest.AbstractInitializedModelIntegrationTest;
import com.evolveum.midpoint.model.intest.TestActivation;
import com.evolveum.midpoint.model.intest.TestTriggerTask;
import com.evolveum.midpoint.model.intest.mapping.TestMapping;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.IdItemPathSegment;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.DebugUtil;

/**
 * @author Radovan Semancik
 *
 * @see TestActivation
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestValidityRecomputeTask extends AbstractInitializedModelIntegrationTest {

	private static final File TEST_DIR = new File("src/test/resources/sync");

	protected static final File ROLE_RED_JUDGE_FILE = new File(TEST_DIR, "role-red-judge.xml");
	protected static final String ROLE_RED_JUDGE_OID = "12345111-1111-2222-1111-121212111222";

	protected static final File ROLE_BIG_JUDGE_FILE = new File(TEST_DIR, "role-big-judge.xml");
	protected static final String ROLE_BIG_JUDGE_OID = "12345111-1111-2222-1111-121212111224";

	private static final XMLGregorianCalendar LONG_LONG_TIME_AGO = XmlTypeConverter.createXMLGregorianCalendar(1111, 1, 1, 12, 00, 00);

	private XMLGregorianCalendar drakeValidFrom;
	private XMLGregorianCalendar drakeValidTo;

	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);

		repoAddObjectFromFile(ROLE_RED_JUDGE_FILE, initResult);
		repoAddObjectFromFile(ROLE_BIG_JUDGE_FILE, initResult);

		DebugUtil.setDetailedDebugDump(true);
	}

	protected String getValidityScannerTaskFileName() {
		return TASK_VALIDITY_SCANNER_FILENAME;
	}

	@Test
    public void test100ImportValidityScannerTask() throws Exception {
		final String TEST_NAME = "test100ImportValidityScannerTask";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TestValidityRecomputeTask.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        // Pretend that the user was added a long time ago
        clock.override(LONG_LONG_TIME_AGO);
        addObject(USER_HERMAN_FILE);
        // Make sure that it is effectivelly disabled
        PrismObject<UserType> userHermanBefore = getUser(USER_HERMAN_OID);
        assertEffectiveActivation(userHermanBefore, ActivationStatusType.DISABLED);
        assertValidityStatus(userHermanBefore, TimeIntervalStatusType.BEFORE);
        clock.resetOverride();

        XMLGregorianCalendar startCal = clock.currentTimeXMLGregorianCalendar();

		/// WHEN
        TestUtil.displayWhen(TEST_NAME);
        importObjectFromFile(getValidityScannerTaskFileName());

        waitForValidityTaskStart();
        waitForValidityTaskFinish();

        // THEN
        TestUtil.displayThen(TEST_NAME);
		XMLGregorianCalendar endCal = clock.currentTimeXMLGregorianCalendar();
        assertLastScanTimestamp(TASK_VALIDITY_SCANNER_OID, startCal, endCal);

        PrismObject<UserType> userHermanAfter = getUser(USER_HERMAN_OID);
        assertEffectiveActivation(userHermanAfter, ActivationStatusType.ENABLED);
        assertValidityStatus(userHermanAfter, TimeIntervalStatusType.IN);
	}

	@Test
    public void test110JackAssignJudgeDisabled() throws Exception {
		final String TEST_NAME = "test110JackAssignJudgeDisabled";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TestValidityRecomputeTask.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        ActivationType activationType = new ActivationType();
        activationType.setAdministrativeStatus(ActivationStatusType.DISABLED);

        testJackAssignRoleJudgeInvalid(TEST_NAME, activationType, task, result);
	}

	@Test
    public void test111JackAssignJudgeNotYetValid() throws Exception {
		final String TEST_NAME = "test111JackAssignJudgeNotYetValid";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TestValidityRecomputeTask.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        ActivationType activationType = new ActivationType();
        XMLGregorianCalendar validFrom = clock.currentTimeXMLGregorianCalendar();
        validFrom.add(XmlTypeConverter.createDuration(60*60*1000)); // one hour ahead
        activationType.setValidFrom(validFrom);

        testJackAssignRoleJudgeInvalid(TEST_NAME, activationType, task, result);
	}

	@Test
    public void test112JackAssignJudgeAfterValidity() throws Exception {
		final String TEST_NAME = "test112JackAssignJudgeAfterValidity";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TestValidityRecomputeTask.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        ActivationType activationType = new ActivationType();
        XMLGregorianCalendar validTo = clock.currentTimeXMLGregorianCalendar();
        validTo.add(XmlTypeConverter.createDuration(-60*60*1000)); // one hour ago
        activationType.setValidTo(validTo);

        testJackAssignRoleJudgeInvalid(TEST_NAME, activationType, task, result);
	}

	@Test
    public void test115JackAssignJudgeEnabled() throws Exception {
		final String TEST_NAME = "test115JackAssignJudgeEnabled";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TestValidityRecomputeTask.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        ActivationType activationType = new ActivationType();
        activationType.setAdministrativeStatus(ActivationStatusType.ENABLED);

        testJackAssignRoleJudgeValid(TEST_NAME, activationType, task, result);
	}

	@Test
    public void test115JackAssignJudgeValid() throws Exception {
		final String TEST_NAME = "test115JackAssignJudgeValid";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TestValidityRecomputeTask.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        ActivationType activationType = new ActivationType();
        XMLGregorianCalendar validFrom = clock.currentTimeXMLGregorianCalendar();
        validFrom.add(XmlTypeConverter.createDuration(-60*60*1000)); // one hour ago
        activationType.setValidFrom(validFrom);
        XMLGregorianCalendar validTo = clock.currentTimeXMLGregorianCalendar();
        validTo.add(XmlTypeConverter.createDuration(60*60*1000)); // one hour ahead
        activationType.setValidTo(validTo);

        testJackAssignRoleJudgeValid(TEST_NAME, activationType, task, result);
	}

	private void testJackAssignRoleJudgeValid(final String TEST_NAME, ActivationType activationType, Task task, OperationResult result) throws Exception {

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        assignRole(USER_JACK_OID, ROLE_BIG_JUDGE_OID, activationType, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        assertDummyAccount(null, USER_JACK_USERNAME);
        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, USER_JACK_USERNAME);

		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        waitForValidityNextRunAssertSuccess();

        // THEN
        TestUtil.displayThen(TEST_NAME);
        assertDummyAccount(null, USER_JACK_USERNAME);

        PrismObject<UserType> user = getUser(USER_JACK_OID);
        display("User after", user);
        assertLinks(user, 2);
        assert11xUserOk(user);

        MidPointPrincipal principal = userProfileService.getPrincipal(user);
        assertAuthorized(principal, AUTZ_PUNISH_URL);

        // CLEANUP
        unassignAllRoles(USER_JACK_OID);
        assertNoDummyAccount(null, USER_JACK_USERNAME);
	}

	private void testJackAssignRoleJudgeInvalid(final String TEST_NAME, ActivationType activationType, Task task, OperationResult result) throws Exception {

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        assignRole(USER_JACK_OID, ROLE_BIG_JUDGE_OID, activationType, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        assertNoDummyAccount(null, USER_JACK_USERNAME);

		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        waitForValidityNextRunAssertSuccess();

        // THEN
        TestUtil.displayThen(TEST_NAME);
        assertNoDummyAccount(null, USER_JACK_USERNAME);
        assertNoDummyAccount(RESOURCE_DUMMY_RED_NAME, USER_JACK_USERNAME);

        PrismObject<UserType> user = getUser(USER_JACK_OID);
        display("User after", user);
        assertNoLinkedAccount(user);
        assert11xUserOk(user);

        MidPointPrincipal principal = userProfileService.getPrincipal(user);
        assertNotAuthorized(principal, AUTZ_PUNISH_URL);

        // CLEANUP
        unassignAllRoles(USER_JACK_OID);
        assertNoDummyAccount(null, USER_JACK_USERNAME);
	}

	private void assert11xUserOk(PrismObject<UserType> user) {
		assertAdministrativeStatusEnabled(user);
        assertEffectiveActivation(user, ActivationStatusType.ENABLED);
	}


	@Test
    public void test120JackDisableAssignmentJudge() throws Exception {
		final String TEST_NAME = "test120JackDisableAssignmentJudge";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TestValidityRecomputeTask.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        ActivationType activationType = new ActivationType();
        activationType.setAdministrativeStatus(ActivationStatusType.ENABLED);
        assignRole(USER_JACK_OID, ROLE_BIG_JUDGE_OID, activationType, task, result);
        assertDummyAccount(null, USER_JACK_USERNAME);
        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, USER_JACK_USERNAME);
        AssignmentType judgeAssignment = getJudgeAssignment(USER_JACK_OID);

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        modifyAssignmentAdministrativeStatus(USER_JACK_OID, judgeAssignment.getId(),
        		ActivationStatusType.DISABLED, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        PrismObject<UserType> user = getUser(USER_JACK_OID);
        display("User after", user);
        assertNoDummyAccount(null, USER_JACK_USERNAME);

        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, USER_JACK_USERNAME, USER_JACK_FULL_NAME, false);

        assert11xUserOk(user);
	}

	@Test
    public void test122JackReplaceNullAdministrativeStatusAssignmentJudge() throws Exception {
		final String TEST_NAME = "test122JackReplaceNullAdministrativeStatusAssignmentJudge";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TestValidityRecomputeTask.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        assertNoDummyAccount(null, USER_JACK_USERNAME);
        AssignmentType judgeAssignment = getJudgeAssignment(USER_JACK_OID);

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        modifyAssignmentAdministrativeStatus(USER_JACK_OID, judgeAssignment.getId(),
        		null, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        PrismObject<UserType> user = getUser(USER_JACK_OID);
        display("User after", user);
        assertDummyAccount(null, USER_JACK_USERNAME);
        assert11xUserOk(user);
	}

	@Test
    public void test123JackDisableAssignmentJudge() throws Exception {
		final String TEST_NAME = "test123JackDisableAssignmentJudge";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TestValidityRecomputeTask.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        assertDummyAccount(null, USER_JACK_USERNAME);
        AssignmentType judgeAssignment = getJudgeAssignment(USER_JACK_OID);

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        modifyAssignmentAdministrativeStatus(USER_JACK_OID, judgeAssignment.getId(),
        		ActivationStatusType.DISABLED, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        PrismObject<UserType> user = getUser(USER_JACK_OID);
        display("User after", user);
        assertNoDummyAccount(null, USER_JACK_USERNAME);
        assert11xUserOk(user);
	}

	@Test
    public void test124JackEnableAssignmentJudge() throws Exception {
		final String TEST_NAME = "test124JackEnableAssignmentJudge";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TestValidityRecomputeTask.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        assertNoDummyAccount(null, USER_JACK_USERNAME);
        AssignmentType judgeAssignment = getJudgeAssignment(USER_JACK_OID);

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        modifyAssignmentAdministrativeStatus(USER_JACK_OID, judgeAssignment.getId(),
        		ActivationStatusType.ENABLED, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        PrismObject<UserType> user = getUser(USER_JACK_OID);
        display("User after", user);
        assertDummyAccount(null, USER_JACK_USERNAME);
        assert11xUserOk(user);
	}

	@Test
    public void test125JackDeleteAdministrativeStatusAssignmentJudge() throws Exception {
		final String TEST_NAME = "test125JackDeleteAdministrativeStatusAssignmentJudge";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TestValidityRecomputeTask.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        assertDummyAccount(null, USER_JACK_USERNAME);
        AssignmentType judgeAssignment = getJudgeAssignment(USER_JACK_OID);

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        modifyObjectDeleteProperty(UserType.class, USER_JACK_OID,
        		new ItemPath(
        				new NameItemPathSegment(UserType.F_ASSIGNMENT),
        				new IdItemPathSegment(judgeAssignment.getId()),
        				new NameItemPathSegment(AssignmentType.F_ACTIVATION),
        				new NameItemPathSegment(ActivationType.F_ADMINISTRATIVE_STATUS)),
        		task, result, ActivationStatusType.ENABLED);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        PrismObject<UserType> user = getUser(USER_JACK_OID);
        display("User after", user);
        assertDummyAccount(null, USER_JACK_USERNAME);
        assert11xUserOk(user);
	}

	@Test
    public void test126JackAddAdministrativeStatusAssignmentJudge() throws Exception {
		final String TEST_NAME = "test126JackAddAdministrativeStatusAssignmentJudge";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TestValidityRecomputeTask.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        assertDummyAccount(null, USER_JACK_USERNAME);
        AssignmentType judgeAssignment = getJudgeAssignment(USER_JACK_OID);

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        modifyObjectAddProperty(UserType.class, USER_JACK_OID,
        		new ItemPath(
        				new NameItemPathSegment(UserType.F_ASSIGNMENT),
        				new IdItemPathSegment(judgeAssignment.getId()),
        				new NameItemPathSegment(AssignmentType.F_ACTIVATION),
        				new NameItemPathSegment(ActivationType.F_ADMINISTRATIVE_STATUS)),
        		task, result, ActivationStatusType.ENABLED);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        PrismObject<UserType> user = getUser(USER_JACK_OID);
        display("User after", user);
        assertDummyAccount(null, USER_JACK_USERNAME);
        assert11xUserOk(user);
	}

	@Test
    public void test127JackDeleteActivationAssignmentJudge() throws Exception {
		final String TEST_NAME = "test127JackDeleteActivationAssignmentJudge";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TestValidityRecomputeTask.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        assertDummyAccount(null, USER_JACK_USERNAME);
        AssignmentType judgeAssignment = getJudgeAssignment(USER_JACK_OID);
        ActivationType activationType = new ActivationType();
        activationType.setAdministrativeStatus(ActivationStatusType.ENABLED);

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        modifyObjectDeleteContainer(UserType.class, USER_JACK_OID,
        		new ItemPath(
        				new NameItemPathSegment(UserType.F_ASSIGNMENT),
        				new IdItemPathSegment(judgeAssignment.getId()),
        				new NameItemPathSegment(AssignmentType.F_ACTIVATION)),
        		task, result, activationType);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        PrismObject<UserType> user = getUser(USER_JACK_OID);
        display("User after", user);
        assertDummyAccount(null, USER_JACK_USERNAME);
        assert11xUserOk(user);
	}

	@Test
    public void test128JackAssignmentJudgeValidToSetInvalid() throws Exception {
		final String TEST_NAME = "test128JackAssignmentJudgeValidToSetInvalid";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TestValidityRecomputeTask.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        assertDummyAccount(null, USER_JACK_USERNAME);
        AssignmentType judgeAssignment = getJudgeAssignment(USER_JACK_OID);
        ActivationType activationType = new ActivationType();
        XMLGregorianCalendar validTo = clock.currentTimeXMLGregorianCalendar();
        validTo.add(XmlTypeConverter.createDuration(-60*60*1000)); // one hour ago
        activationType.setValidTo(validTo);

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        modifyObjectReplaceContainer(UserType.class, USER_JACK_OID,
        		new ItemPath(
        				new NameItemPathSegment(UserType.F_ASSIGNMENT),
        				new IdItemPathSegment(judgeAssignment.getId()),
        				new NameItemPathSegment(AssignmentType.F_ACTIVATION)),
        		task, result, activationType);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        PrismObject<UserType> user = getUser(USER_JACK_OID);
        display("User after", user);
        assertNoDummyAccount(null, USER_JACK_USERNAME);
        assert11xUserOk(user);
	}

	@Test
    public void test129JackAssignmentJudgeValidToSetValid() throws Exception {
		final String TEST_NAME = "test129JackAssignmentJudgeValidToSetValid";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TestValidityRecomputeTask.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        assertNoDummyAccount(null, USER_JACK_USERNAME);
        AssignmentType judgeAssignment = getJudgeAssignment(USER_JACK_OID);
        XMLGregorianCalendar validTo = clock.currentTimeXMLGregorianCalendar();
        validTo.add(XmlTypeConverter.createDuration(60*60*1000)); // one hour ahead

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        modifyObjectReplaceProperty(UserType.class, USER_JACK_OID,
        		new ItemPath(
        				new NameItemPathSegment(UserType.F_ASSIGNMENT),
        				new IdItemPathSegment(judgeAssignment.getId()),
        				new NameItemPathSegment(AssignmentType.F_ACTIVATION),
        				new NameItemPathSegment(ActivationType.F_VALID_TO)),
        		task, result, validTo);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        PrismObject<UserType> user = getUser(USER_JACK_OID);
        display("User after", user);
        assertDummyAccount(null, USER_JACK_USERNAME);

        assert11xUserOk(user);

        // CLEANUP
        unassignAllRoles(USER_JACK_OID);
        assertNoDummyAccount(null, USER_JACK_USERNAME);
	}

	private AssignmentType getJudgeAssignment(String userOid) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
		PrismObject<UserType> user = getUser(userOid);
		List<AssignmentType> assignments = user.asObjectable().getAssignment();
		assertEquals("Wrong num ass", 1, assignments.size());
		return assignments.iterator().next();
	}

	/**
	 * The test13x works with two roles for the same resource, enabling/disabling them.
	 */
	@Test
    public void test130BarbossaAssignJudgeEnabled() throws Exception {
		final String TEST_NAME = "test130BarbossaAssignJudgeEnabled";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TestValidityRecomputeTask.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        // preconditions
        assertNoAssignments(USER_BARBOSSA_OID);
        assertNoDummyAccount(null, USER_BARBOSSA_USERNAME);

        ActivationType activationType = new ActivationType();
        activationType.setAdministrativeStatus(ActivationStatusType.ENABLED);

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        assignRole(USER_BARBOSSA_OID, ROLE_JUDGE_OID, activationType, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        assertDummyAccount(null, USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME, true);

		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        waitForValidityNextRunAssertSuccess();

        // THEN
        TestUtil.displayThen(TEST_NAME);
        assertDummyAccount(null, USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME, true);
        assertDummyAccountAttribute(null, USER_BARBOSSA_USERNAME,
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME,
        		ROLE_JUDGE_TITLE);
        assertDummyAccountAttribute(null, USER_BARBOSSA_USERNAME,
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME,
        		RESOURCE_DUMMY_DRINK, ROLE_JUDGE_DRINK);

        PrismObject<UserType> user = getUser(USER_BARBOSSA_OID);
        display("User after", user);
        assertLinks(user, 1);

        MidPointPrincipal principal = userProfileService.getPrincipal(user);
        assertAuthorized(principal, AUTZ_PUNISH_URL);
	}

	@Test
    public void test131BarbossaAssignSailorEnabled() throws Exception {
		final String TEST_NAME = "test131BarbossaAssignSailorEnabled";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TestValidityRecomputeTask.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        ActivationType activationType = new ActivationType();
        activationType.setAdministrativeStatus(ActivationStatusType.ENABLED);

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        assignRole(USER_BARBOSSA_OID, ROLE_SAILOR_OID, activationType, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        assertDummyAccount(null, USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME, true);

		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        waitForValidityNextRunAssertSuccess();

        // THEN
        TestUtil.displayThen(TEST_NAME);
        assertDummyAccount(null, USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME, true);
        assertDummyAccountAttribute(null, USER_BARBOSSA_USERNAME,
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME,
        		ROLE_JUDGE_TITLE);
        assertDummyAccountAttribute(null, USER_BARBOSSA_USERNAME,
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME,
        		RESOURCE_DUMMY_DRINK, ROLE_JUDGE_DRINK, ROLE_SAILOR_DRINK);

        PrismObject<UserType> user = getUser(USER_BARBOSSA_OID);
        display("User after", user);
        assertLinks(user, 1);

        MidPointPrincipal principal = userProfileService.getPrincipal(user);
        assertAuthorized(principal, AUTZ_PUNISH_URL);
	}

	@Test
    public void test132BarbossaDisableAssignmentJudge() throws Exception {
		final String TEST_NAME = "test132BarbossaDisableAssignmentJudge";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TestValidityRecomputeTask.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        AssignmentType assignment = getUserAssignment(USER_BARBOSSA_OID, ROLE_JUDGE_OID);

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        modifyAssignmentAdministrativeStatus(USER_BARBOSSA_OID, assignment.getId(),
        		ActivationStatusType.DISABLED, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        assertDummyAccount(null, USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME, true);

		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        waitForValidityNextRunAssertSuccess();

        // THEN
        TestUtil.displayThen(TEST_NAME);
        assertDummyAccount(null, USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME, true);
        assertNoDummyAccountAttribute(null, USER_BARBOSSA_USERNAME,
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME);
        assertDummyAccountAttribute(null, USER_BARBOSSA_USERNAME,
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME,
        		RESOURCE_DUMMY_DRINK, ROLE_SAILOR_DRINK);

        PrismObject<UserType> user = getUser(USER_BARBOSSA_OID);
        display("User after", user);
        assertLinks(user, 1);

        MidPointPrincipal principal = userProfileService.getPrincipal(user);
        assertNotAuthorized(principal, AUTZ_PUNISH_URL);
	}

	@Test
    public void test133BarbossaDisableAssignmentSailor() throws Exception {
		final String TEST_NAME = "test133BarbossaDisableAssignmentSailor";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TestValidityRecomputeTask.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        AssignmentType assignment = getUserAssignment(USER_BARBOSSA_OID, ROLE_SAILOR_OID);

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        modifyAssignmentAdministrativeStatus(USER_BARBOSSA_OID, assignment.getId(),
        		ActivationStatusType.DISABLED, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        assertNoDummyAccount(null, USER_BARBOSSA_USERNAME);

		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        waitForValidityNextRunAssertSuccess();

        // THEN
        TestUtil.displayThen(TEST_NAME);
        assertNoDummyAccount(null, USER_BARBOSSA_USERNAME);

        PrismObject<UserType> user = getUser(USER_BARBOSSA_OID);
        display("User after", user);
        assertLinks(user, 0);

        MidPointPrincipal principal = userProfileService.getPrincipal(user);
        assertNotAuthorized(principal, AUTZ_PUNISH_URL);
	}

	@Test
    public void test134BarbossaEnableAssignmentJudge() throws Exception {
		final String TEST_NAME = "test134BarbossaEnableAssignmentJudge";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TestValidityRecomputeTask.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        AssignmentType assignment = getUserAssignment(USER_BARBOSSA_OID, ROLE_JUDGE_OID);

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        modifyAssignmentAdministrativeStatus(USER_BARBOSSA_OID, assignment.getId(),
        		ActivationStatusType.ENABLED, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        assertDummyAccount(null, USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME, true);

		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        assertDummyAccount(null, USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME, true);
        assertDummyAccountAttribute(null, USER_BARBOSSA_USERNAME,
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME,
        		ROLE_JUDGE_TITLE);
        assertDummyAccountAttribute(null, USER_BARBOSSA_USERNAME,
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME,
        		RESOURCE_DUMMY_DRINK, ROLE_JUDGE_DRINK);

        PrismObject<UserType> user = getUser(USER_BARBOSSA_OID);
        display("User after", user);
        assertLinks(user, 1);

        MidPointPrincipal principal = userProfileService.getPrincipal(user);
        assertAuthorized(principal, AUTZ_PUNISH_URL);
	}

	@Test
    public void test135BarbossaEnableAssignmentSailor() throws Exception {
		final String TEST_NAME = "test135BarbossaEnableAssignmentSailor";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TestValidityRecomputeTask.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        AssignmentType assignment = getUserAssignment(USER_BARBOSSA_OID, ROLE_SAILOR_OID);

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        modifyAssignmentAdministrativeStatus(USER_BARBOSSA_OID, assignment.getId(),
        		ActivationStatusType.ENABLED, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        assertDummyAccount(null, USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME, true);

		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        assertDummyAccount(null, USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME, true);
        assertDummyAccountAttribute(null, USER_BARBOSSA_USERNAME,
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME,
        		ROLE_JUDGE_TITLE);
        assertDummyAccountAttribute(null, USER_BARBOSSA_USERNAME,
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME,
        		RESOURCE_DUMMY_DRINK, ROLE_JUDGE_DRINK, ROLE_SAILOR_DRINK);

        PrismObject<UserType> user = getUser(USER_BARBOSSA_OID);
        display("User after", user);
        assertLinks(user, 1);

        MidPointPrincipal principal = userProfileService.getPrincipal(user);
        assertAuthorized(principal, AUTZ_PUNISH_URL);
	}

	@Test
    public void test136BarbossaDisableBothAssignments() throws Exception {
		final String TEST_NAME = "test136BarbossaDisableBothAssignments";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TestValidityRecomputeTask.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        AssignmentType judgeAssignment = getUserAssignment(USER_BARBOSSA_OID, ROLE_JUDGE_OID);
        AssignmentType sailorAssignment = getUserAssignment(USER_BARBOSSA_OID, ROLE_SAILOR_OID);

        ObjectDelta<UserType> objectDelta =
        		ObjectDelta.createModificationReplaceProperty(UserType.class,
        				USER_BARBOSSA_OID,
        				new ItemPath(
                				new NameItemPathSegment(UserType.F_ASSIGNMENT),
                				new IdItemPathSegment(judgeAssignment.getId()),
                				new NameItemPathSegment(AssignmentType.F_ACTIVATION),
                				new NameItemPathSegment(ActivationType.F_ADMINISTRATIVE_STATUS)),
                		prismContext, ActivationStatusType.DISABLED);
        objectDelta.addModificationReplaceProperty(new ItemPath(
                				new NameItemPathSegment(UserType.F_ASSIGNMENT),
                				new IdItemPathSegment(sailorAssignment.getId()),
                				new NameItemPathSegment(AssignmentType.F_ACTIVATION),
                				new NameItemPathSegment(ActivationType.F_ADMINISTRATIVE_STATUS)),
                				ActivationStatusType.DISABLED);

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        modelService.executeChanges(MiscSchemaUtil.createCollection(objectDelta), null, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        assertNoDummyAccount(null, USER_BARBOSSA_USERNAME);

		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        waitForValidityNextRunAssertSuccess();

        // THEN
        TestUtil.displayThen(TEST_NAME);
        assertNoDummyAccount(null, USER_BARBOSSA_USERNAME);

        PrismObject<UserType> user = getUser(USER_BARBOSSA_OID);
        display("User after", user);
        assertLinks(user, 0);

        MidPointPrincipal principal = userProfileService.getPrincipal(user);
        assertNotAuthorized(principal, AUTZ_PUNISH_URL);
	}

	@Test
    public void test137BarbossaEnableBothAssignments() throws Exception {
		final String TEST_NAME = "test137BarbossaEnableBothAssignments";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TestValidityRecomputeTask.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        AssignmentType judgeAssignment = getUserAssignment(USER_BARBOSSA_OID, ROLE_JUDGE_OID);
        AssignmentType sailorAssignment = getUserAssignment(USER_BARBOSSA_OID, ROLE_SAILOR_OID);

        ObjectDelta<UserType> objectDelta =
        		ObjectDelta.createModificationReplaceProperty(UserType.class,
        				USER_BARBOSSA_OID,
        				new ItemPath(
                				new NameItemPathSegment(UserType.F_ASSIGNMENT),
                				new IdItemPathSegment(judgeAssignment.getId()),
                				new NameItemPathSegment(AssignmentType.F_ACTIVATION),
                				new NameItemPathSegment(ActivationType.F_ADMINISTRATIVE_STATUS)),
                		prismContext, ActivationStatusType.ENABLED);
        objectDelta.addModificationReplaceProperty(new ItemPath(
                				new NameItemPathSegment(UserType.F_ASSIGNMENT),
                				new IdItemPathSegment(sailorAssignment.getId()),
                				new NameItemPathSegment(AssignmentType.F_ACTIVATION),
                				new NameItemPathSegment(ActivationType.F_ADMINISTRATIVE_STATUS)),
                				ActivationStatusType.ENABLED);

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        modelService.executeChanges(MiscSchemaUtil.createCollection(objectDelta), null, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        assertDummyAccount(null, USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME, true);

		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        waitForValidityNextRunAssertSuccess();

        // THEN
        TestUtil.displayThen(TEST_NAME);
        assertDummyAccount(null, USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME, true);
        assertDummyAccountAttribute(null, USER_BARBOSSA_USERNAME,
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME,
        		ROLE_JUDGE_TITLE);
        assertDummyAccountAttribute(null, USER_BARBOSSA_USERNAME,
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME,
        		RESOURCE_DUMMY_DRINK, ROLE_JUDGE_DRINK, ROLE_SAILOR_DRINK);

        PrismObject<UserType> user = getUser(USER_BARBOSSA_OID);
        display("User after", user);
        assertLinks(user, 1);

        MidPointPrincipal principal = userProfileService.getPrincipal(user);
        assertAuthorized(principal, AUTZ_PUNISH_URL);
	}

	/**
	 * Unassign disabled assignments.
	 */
	@Test
    public void test139BarbossaDisableBothAssignmentsUnassign() throws Exception {
		final String TEST_NAME = "test139BarbossaDisableBothAssignmentsUnassign";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TestValidityRecomputeTask.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        AssignmentType judgeAssignment = getUserAssignment(USER_BARBOSSA_OID, ROLE_JUDGE_OID);
        AssignmentType judgeAssignmentLight = new AssignmentType();
        judgeAssignmentLight.setId(judgeAssignment.getId());
        AssignmentType sailorAssignment = getUserAssignment(USER_BARBOSSA_OID, ROLE_SAILOR_OID);
        AssignmentType sailorAssignmentLight = new AssignmentType();
        sailorAssignmentLight.setId(sailorAssignment.getId());

        ObjectDelta<UserType> objectDelta =
        		ObjectDelta.createModificationReplaceProperty(UserType.class,
        				USER_BARBOSSA_OID,
        				new ItemPath(
                				new NameItemPathSegment(UserType.F_ASSIGNMENT),
                				new IdItemPathSegment(judgeAssignment.getId()),
                				new NameItemPathSegment(AssignmentType.F_ACTIVATION),
                				new NameItemPathSegment(ActivationType.F_ADMINISTRATIVE_STATUS)),
                		prismContext, ActivationStatusType.DISABLED);
        objectDelta.addModificationReplaceProperty(new ItemPath(
                				new NameItemPathSegment(UserType.F_ASSIGNMENT),
                				new IdItemPathSegment(sailorAssignment.getId()),
                				new NameItemPathSegment(AssignmentType.F_ACTIVATION),
                				new NameItemPathSegment(ActivationType.F_ADMINISTRATIVE_STATUS)),
                				ActivationStatusType.DISABLED);

        modelService.executeChanges(MiscSchemaUtil.createCollection(objectDelta), null, task, result);

        assertNoDummyAccount(null, USER_BARBOSSA_USERNAME);
        waitForValidityNextRunAssertSuccess();
        assertNoDummyAccount(null, USER_BARBOSSA_USERNAME);
        PrismObject<UserType> user = getUser(USER_BARBOSSA_OID);
        display("User after", user);
        assertLinks(user, 0);
        MidPointPrincipal principal = userProfileService.getPrincipal(user);
        assertNotAuthorized(principal, AUTZ_PUNISH_URL);

        objectDelta =
        		ObjectDelta.createModificationDeleteContainer(UserType.class,
        				USER_BARBOSSA_OID,
        				new ItemPath(new NameItemPathSegment(UserType.F_ASSIGNMENT)),
                		prismContext, judgeAssignmentLight);
        objectDelta.addModificationDeleteContainer(
        		new ItemPath(new NameItemPathSegment(UserType.F_ASSIGNMENT)),
        		sailorAssignmentLight);

        display("Unassign delta", objectDelta);

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        modelService.executeChanges(MiscSchemaUtil.createCollection(objectDelta), null, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        assertNoDummyAccount(null, USER_BARBOSSA_USERNAME);

		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        waitForValidityNextRunAssertSuccess();

        // THEN
        TestUtil.displayThen(TEST_NAME);
        assertNoDummyAccount(null, USER_BARBOSSA_USERNAME);

        user = getUser(USER_BARBOSSA_OID);
        display("User after", user);
        assertLinks(user, 0);

        assertNoAssignments(user);

        principal = userProfileService.getPrincipal(user);
        assertNotAuthorized(principal, AUTZ_PUNISH_URL);
	}

	/**
	 * The 14x tests are similar than test13x tests, they work with two roles for the same resource, enabling/disabling them.
	 * The 14x work with the red dummy resource that does disable instead of account delete.
	 */
	@Test
    public void test140BarbossaAssignRedJudgeEnabled() throws Exception {
		final String TEST_NAME = "test140BarbossaAssignRedJudgeEnabled";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TestValidityRecomputeTask.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        // preconditions
        PrismObject<UserType> user = getUser(USER_BARBOSSA_OID);
        display("User before", user);
        assertLinks(user, 0);
        assertNoAssignments(user);
        assertNoDummyAccount(null, USER_BARBOSSA_USERNAME); // just to be on the safe side
        assertNoDummyAccount(RESOURCE_DUMMY_RED_NAME, USER_BARBOSSA_USERNAME);

        ActivationType activationType = new ActivationType();
        activationType.setAdministrativeStatus(ActivationStatusType.ENABLED);

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        assignRole(USER_BARBOSSA_OID, ROLE_RED_JUDGE_OID, activationType, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME, true);

		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        waitForValidityNextRunAssertSuccess();

        // THEN
        TestUtil.displayThen(TEST_NAME);
        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME, true);
        assertDummyAccountAttribute(RESOURCE_DUMMY_RED_NAME, USER_BARBOSSA_USERNAME,
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME,
        		ROLE_JUDGE_TITLE);
        assertDummyAccountAttribute(RESOURCE_DUMMY_RED_NAME, USER_BARBOSSA_USERNAME,
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME,
        		ROLE_JUDGE_DRINK);

        user = getUser(USER_BARBOSSA_OID);
        display("User after", user);
        assertLinks(user, 1);

        MidPointPrincipal principal = userProfileService.getPrincipal(user);
        assertAuthorized(principal, AUTZ_PUNISH_URL);
	}

	@Test
    public void test141BarbossaAssignRedSailorEnabled() throws Exception {
		final String TEST_NAME = "test141BarbossaAssignRedSailorEnabled";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TestValidityRecomputeTask.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        ActivationType activationType = new ActivationType();
        activationType.setAdministrativeStatus(ActivationStatusType.ENABLED);

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        assignRole(USER_BARBOSSA_OID, ROLE_RED_SAILOR_OID, activationType, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME, true);

		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        waitForValidityNextRunAssertSuccess();

        // THEN
        TestUtil.displayThen(TEST_NAME);
        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME, true);
        assertDummyAccountAttribute(RESOURCE_DUMMY_RED_NAME, USER_BARBOSSA_USERNAME,
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME,
        		ROLE_JUDGE_TITLE);
        assertDummyAccountAttribute(RESOURCE_DUMMY_RED_NAME, USER_BARBOSSA_USERNAME,
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME,
        		ROLE_JUDGE_DRINK, ROLE_SAILOR_DRINK);

        PrismObject<UserType> user = getUser(USER_BARBOSSA_OID);
        display("User after", user);
        assertLinks(user, 1);

        MidPointPrincipal principal = userProfileService.getPrincipal(user);
        assertAuthorized(principal, AUTZ_PUNISH_URL);
	}

	@Test
    public void test142BarbossaDisableAssignmentRedJudge() throws Exception {
		final String TEST_NAME = "test142BarbossaDisableAssignmentRedJudge";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TestValidityRecomputeTask.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        AssignmentType assignment = getUserAssignment(USER_BARBOSSA_OID, ROLE_RED_JUDGE_OID);

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        modifyAssignmentAdministrativeStatus(USER_BARBOSSA_OID, assignment.getId(),
        		ActivationStatusType.DISABLED, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME, true);

		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        waitForValidityNextRunAssertSuccess();

        // THEN
        TestUtil.displayThen(TEST_NAME);
        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME, true);
        assertNoDummyAccountAttribute(RESOURCE_DUMMY_RED_NAME, USER_BARBOSSA_USERNAME,
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME);
        assertDummyAccountAttribute(RESOURCE_DUMMY_RED_NAME, USER_BARBOSSA_USERNAME,
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME,
        		ROLE_SAILOR_DRINK);

        PrismObject<UserType> user = getUser(USER_BARBOSSA_OID);
        display("User after", user);
        assertLinks(user, 1);

        MidPointPrincipal principal = userProfileService.getPrincipal(user);
        assertNotAuthorized(principal, AUTZ_PUNISH_URL);
	}

	@Test
    public void test143BarbossaDisableAssignmentRedSailor() throws Exception {
		final String TEST_NAME = "test143BarbossaDisableAssignmentRedSailor";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TestValidityRecomputeTask.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        AssignmentType assignment = getUserAssignment(USER_BARBOSSA_OID, ROLE_RED_SAILOR_OID);

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        modifyAssignmentAdministrativeStatus(USER_BARBOSSA_OID, assignment.getId(),
        		ActivationStatusType.DISABLED, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME, false);

		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        waitForValidityNextRunAssertSuccess();

        // THEN
        TestUtil.displayThen(TEST_NAME);
        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME, false);
        assertNoDummyAccountAttribute(RESOURCE_DUMMY_RED_NAME, USER_BARBOSSA_USERNAME,
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME);
        assertNoDummyAccountAttribute(RESOURCE_DUMMY_RED_NAME, USER_BARBOSSA_USERNAME,
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME);

        PrismObject<UserType> user = getUser(USER_BARBOSSA_OID);
        display("User after", user);
        assertLinks(user, 1);

        MidPointPrincipal principal = userProfileService.getPrincipal(user);
        assertNotAuthorized(principal, AUTZ_PUNISH_URL);
	}

	@Test
    public void test144BarbossaEnableAssignmentRedJudge() throws Exception {
		final String TEST_NAME = "test144BarbossaEnableAssignmentRedJudge";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TestValidityRecomputeTask.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        AssignmentType assignment = getUserAssignment(USER_BARBOSSA_OID, ROLE_RED_JUDGE_OID);

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        modifyAssignmentAdministrativeStatus(USER_BARBOSSA_OID, assignment.getId(),
        		ActivationStatusType.ENABLED, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME, true);

		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME, true);
        assertDummyAccountAttribute(RESOURCE_DUMMY_RED_NAME, USER_BARBOSSA_USERNAME,
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME,
        		ROLE_JUDGE_TITLE);
        assertDummyAccountAttribute(RESOURCE_DUMMY_RED_NAME, USER_BARBOSSA_USERNAME,
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME,
        		ROLE_JUDGE_DRINK);

        PrismObject<UserType> user = getUser(USER_BARBOSSA_OID);
        display("User after", user);
        assertLinks(user, 1);

        MidPointPrincipal principal = userProfileService.getPrincipal(user);
        assertAuthorized(principal, AUTZ_PUNISH_URL);
	}

	@Test
    public void test145BarbossaEnableAssignmentRedSailor() throws Exception {
		final String TEST_NAME = "test145BarbossaEnableAssignmentRedSailor";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TestValidityRecomputeTask.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        AssignmentType assignment = getUserAssignment(USER_BARBOSSA_OID, ROLE_RED_SAILOR_OID);

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        modifyAssignmentAdministrativeStatus(USER_BARBOSSA_OID, assignment.getId(),
        		ActivationStatusType.ENABLED, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME, true);

		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME, true);
        assertDummyAccountAttribute(RESOURCE_DUMMY_RED_NAME, USER_BARBOSSA_USERNAME,
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME,
        		ROLE_JUDGE_TITLE);
        assertDummyAccountAttribute(RESOURCE_DUMMY_RED_NAME, USER_BARBOSSA_USERNAME,
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME,
        		ROLE_JUDGE_DRINK, ROLE_SAILOR_DRINK);

        PrismObject<UserType> user = getUser(USER_BARBOSSA_OID);
        display("User after", user);
        assertLinks(user, 1);

        MidPointPrincipal principal = userProfileService.getPrincipal(user);
        assertAuthorized(principal, AUTZ_PUNISH_URL);
	}

	@Test
    public void test146BarbossaDisableBothRedAssignments() throws Exception {
		final String TEST_NAME = "test146BarbossaDisableBothRedAssignments";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TestValidityRecomputeTask.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        AssignmentType judgeAssignment = getUserAssignment(USER_BARBOSSA_OID, ROLE_RED_JUDGE_OID);
        AssignmentType sailorAssignment = getUserAssignment(USER_BARBOSSA_OID, ROLE_RED_SAILOR_OID);

        ObjectDelta<UserType> objectDelta =
        		ObjectDelta.createModificationReplaceProperty(UserType.class,
        				USER_BARBOSSA_OID,
        				new ItemPath(
                				new NameItemPathSegment(UserType.F_ASSIGNMENT),
                				new IdItemPathSegment(judgeAssignment.getId()),
                				new NameItemPathSegment(AssignmentType.F_ACTIVATION),
                				new NameItemPathSegment(ActivationType.F_ADMINISTRATIVE_STATUS)),
                		prismContext, ActivationStatusType.DISABLED);
        objectDelta.addModificationReplaceProperty(new ItemPath(
                				new NameItemPathSegment(UserType.F_ASSIGNMENT),
                				new IdItemPathSegment(sailorAssignment.getId()),
                				new NameItemPathSegment(AssignmentType.F_ACTIVATION),
                				new NameItemPathSegment(ActivationType.F_ADMINISTRATIVE_STATUS)),
                				ActivationStatusType.DISABLED);

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        modelService.executeChanges(MiscSchemaUtil.createCollection(objectDelta), null, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME, false);

		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        waitForValidityNextRunAssertSuccess();

        // THEN
        TestUtil.displayThen(TEST_NAME);
        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME, false);
        assertNoDummyAccountAttribute(RESOURCE_DUMMY_RED_NAME, USER_BARBOSSA_USERNAME,
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME);
        assertNoDummyAccountAttribute(RESOURCE_DUMMY_RED_NAME, USER_BARBOSSA_USERNAME,
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME);

        PrismObject<UserType> user = getUser(USER_BARBOSSA_OID);
        display("User after", user);
        assertLinks(user, 1);

        MidPointPrincipal principal = userProfileService.getPrincipal(user);
        assertNotAuthorized(principal, AUTZ_PUNISH_URL);
	}

	@Test
    public void test147BarbossaEnableBothRedAssignments() throws Exception {
		final String TEST_NAME = "test147BarbossaEnableBothRedAssignments";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TestValidityRecomputeTask.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        AssignmentType judgeAssignment = getUserAssignment(USER_BARBOSSA_OID, ROLE_RED_JUDGE_OID);
        AssignmentType sailorAssignment = getUserAssignment(USER_BARBOSSA_OID, ROLE_RED_SAILOR_OID);

        ObjectDelta<UserType> objectDelta =
        		ObjectDelta.createModificationReplaceProperty(UserType.class,
        				USER_BARBOSSA_OID,
        				new ItemPath(
                				new NameItemPathSegment(UserType.F_ASSIGNMENT),
                				new IdItemPathSegment(judgeAssignment.getId()),
                				new NameItemPathSegment(AssignmentType.F_ACTIVATION),
                				new NameItemPathSegment(ActivationType.F_ADMINISTRATIVE_STATUS)),
                		prismContext, ActivationStatusType.ENABLED);
        objectDelta.addModificationReplaceProperty(new ItemPath(
                				new NameItemPathSegment(UserType.F_ASSIGNMENT),
                				new IdItemPathSegment(sailorAssignment.getId()),
                				new NameItemPathSegment(AssignmentType.F_ACTIVATION),
                				new NameItemPathSegment(ActivationType.F_ADMINISTRATIVE_STATUS)),
                				ActivationStatusType.ENABLED);

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        modelService.executeChanges(MiscSchemaUtil.createCollection(objectDelta), null, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME, true);

		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        waitForValidityNextRunAssertSuccess();

        // THEN
        TestUtil.displayThen(TEST_NAME);
        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME, true);
        assertDummyAccountAttribute(RESOURCE_DUMMY_RED_NAME, USER_BARBOSSA_USERNAME,
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME,
        		ROLE_JUDGE_TITLE);
        assertDummyAccountAttribute(RESOURCE_DUMMY_RED_NAME, USER_BARBOSSA_USERNAME,
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME,
        		ROLE_JUDGE_DRINK, ROLE_SAILOR_DRINK);

        PrismObject<UserType> user = getUser(USER_BARBOSSA_OID);
        display("User after", user);
        assertLinks(user, 1);

        MidPointPrincipal principal = userProfileService.getPrincipal(user);
        assertAuthorized(principal, AUTZ_PUNISH_URL);
	}

	/**
	 * Unassign disabled assignments.
	 */
	@Test
    public void test149BarbossaDisableBothRedAssignmentsUnassign() throws Exception {
		final String TEST_NAME = "test149BarbossaDisableBothRedAssignmentsUnassign";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TestValidityRecomputeTask.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        AssignmentType judgeAssignment = getUserAssignment(USER_BARBOSSA_OID, ROLE_RED_JUDGE_OID);
        AssignmentType judgeAssignmentLight = new AssignmentType();
        judgeAssignmentLight.setId(judgeAssignment.getId());
        AssignmentType sailorAssignment = getUserAssignment(USER_BARBOSSA_OID, ROLE_RED_SAILOR_OID);
        AssignmentType sailorAssignmentLight = new AssignmentType();
        sailorAssignmentLight.setId(sailorAssignment.getId());

        ObjectDelta<UserType> objectDelta =
        		ObjectDelta.createModificationReplaceProperty(UserType.class,
        				USER_BARBOSSA_OID,
        				new ItemPath(
                				new NameItemPathSegment(UserType.F_ASSIGNMENT),
                				new IdItemPathSegment(judgeAssignment.getId()),
                				new NameItemPathSegment(AssignmentType.F_ACTIVATION),
                				new NameItemPathSegment(ActivationType.F_ADMINISTRATIVE_STATUS)),
                		prismContext, ActivationStatusType.DISABLED);
        objectDelta.addModificationReplaceProperty(new ItemPath(
                				new NameItemPathSegment(UserType.F_ASSIGNMENT),
                				new IdItemPathSegment(sailorAssignment.getId()),
                				new NameItemPathSegment(AssignmentType.F_ACTIVATION),
                				new NameItemPathSegment(ActivationType.F_ADMINISTRATIVE_STATUS)),
                				ActivationStatusType.DISABLED);

        modelService.executeChanges(MiscSchemaUtil.createCollection(objectDelta), null, task, result);

        assertNoDummyAccount(null, USER_BARBOSSA_USERNAME);
        waitForValidityNextRunAssertSuccess();
        assertNoDummyAccount(null, USER_BARBOSSA_USERNAME);
        PrismObject<UserType> user = getUser(USER_BARBOSSA_OID);
        display("User after", user);
        assertLinks(user, 1);
        MidPointPrincipal principal = userProfileService.getPrincipal(user);
        assertNotAuthorized(principal, AUTZ_PUNISH_URL);

        objectDelta =
        		ObjectDelta.createModificationDeleteContainer(UserType.class,
        				USER_BARBOSSA_OID,
        				new ItemPath(new NameItemPathSegment(UserType.F_ASSIGNMENT)),
                		prismContext, judgeAssignmentLight);
        objectDelta.addModificationDeleteContainer(
        		new ItemPath(new NameItemPathSegment(UserType.F_ASSIGNMENT)),
        		sailorAssignmentLight);

        display("Unassign delta", objectDelta);

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        modelService.executeChanges(MiscSchemaUtil.createCollection(objectDelta), null, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME, false);
        assertNoDummyAccount(null, USER_BARBOSSA_USERNAME); // to be on the safe side

		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        waitForValidityNextRunAssertSuccess();

        // THEN
        TestUtil.displayThen(TEST_NAME);
        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME, false);
        assertNoDummyAccountAttribute(RESOURCE_DUMMY_RED_NAME, USER_BARBOSSA_USERNAME,
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME);
        assertNoDummyAccountAttribute(RESOURCE_DUMMY_RED_NAME, USER_BARBOSSA_USERNAME,
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME);
        assertNoDummyAccount(null, USER_BARBOSSA_USERNAME); // to be on the safe side

        user = getUser(USER_BARBOSSA_OID);
        display("User after", user);
        assertLinks(user, 1);

        assertNoAssignments(user);

        principal = userProfileService.getPrincipal(user);
        assertNotAuthorized(principal, AUTZ_PUNISH_URL);
	}


	@Test
    public void test190HermanGoesInvalid() throws Exception {
		final String TEST_NAME = "test190HermanGoesInvalid";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TestValidityRecomputeTask.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        XMLGregorianCalendar startCal = clock.currentTimeXMLGregorianCalendar();

        PrismObject<UserType> userHermanBefore = getUser(USER_HERMAN_OID);
        XMLGregorianCalendar validTo = userHermanBefore.asObjectable().getActivation().getValidTo();
        assertEffectiveActivation(userHermanBefore, ActivationStatusType.ENABLED);
        assertValidityStatus(userHermanBefore, TimeIntervalStatusType.IN);

        // Let's move the clock tiny bit after herman's validTo
        validTo.add(XmlTypeConverter.createDuration(100));
        clock.override(validTo);

		/// WHEN
        TestUtil.displayWhen(TEST_NAME);
        waitForValidityNextRunAssertSuccess();

        // THEN
        TestUtil.displayThen(TEST_NAME);

        // THEN
        XMLGregorianCalendar endCal = clock.currentTimeXMLGregorianCalendar();

        PrismObject<UserType> userHermanAfter = getUser(USER_HERMAN_OID);
        assertEffectiveActivation(userHermanAfter, ActivationStatusType.DISABLED);
        assertValidityStatus(userHermanAfter, TimeIntervalStatusType.AFTER);

        assertLastScanTimestamp(TASK_VALIDITY_SCANNER_OID, startCal, endCal);
	}

	@Test
    public void test200ImportTriggerScannerTask() throws Exception {
		final String TEST_NAME = "test200ImportTriggerScannerTask";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TestTriggerTask.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        XMLGregorianCalendar startCal = clock.currentTimeXMLGregorianCalendar();

		/// WHEN
        TestUtil.displayWhen(TEST_NAME);
        importObjectFromFile(TASK_TRIGGER_SCANNER_FILE);

        waitForTaskStart(TASK_TRIGGER_SCANNER_OID, false);
        waitForTaskFinish(TASK_TRIGGER_SCANNER_OID, true);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        XMLGregorianCalendar endCal = clock.currentTimeXMLGregorianCalendar();
        assertLastScanTimestamp(TASK_TRIGGER_SCANNER_OID, startCal, endCal);

	}

	/**
	 * Explicitly disable Elaine's red account. Do this at the beginning of the test. We will
	 * move time ahead in later tests. This account should remain here exactly like this
	 * at the end of all tests.
	 */
	@Test
    public void test205AccountRedElaineDisable() throws Exception {
		final String TEST_NAME = "test205AccountRedElaineDisable";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestActivation.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

		// WHEN
        modifyAccountShadowReplace(ACCOUNT_SHADOW_ELAINE_DUMMY_RED_OID, ACTIVATION_ADMINISTRATIVE_STATUS_PATH,
        		task, result, ActivationStatusType.DISABLED);

		// THEN
		result.computeStatus();
        TestUtil.assertSuccess("executeChanges result", result);

        PrismObject<ShadowType> accountShadow = getShadowModel(ACCOUNT_SHADOW_ELAINE_DUMMY_RED_OID);
		assertDisableReasonShadow(accountShadow, SchemaConstants.MODEL_DISABLE_REASON_EXPLICIT);
	}

	/**
	 * Note: red resource disables account on unsassign, does NOT delete it.
	 * Just the recompute trigger is set
	 */
	@Test
    public void test210JackAssignAndUnassignAccountRed() throws Exception {
		final String TEST_NAME = "test210JackAssignAndUnassignAccountRed";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestMapping.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        // assign
        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<>();
        ObjectDelta<UserType> userDelta = createAccountAssignmentUserDelta(USER_JACK_OID,
        		RESOURCE_DUMMY_RED_OID, null, true);
        deltas.add(userDelta);

		// WHEN
		modelService.executeChanges(deltas, null, task, result);

		// THEN
		assertDummyAccount(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME, "Jack Sparrow", true);

		// unassign
        deltas = new ArrayList<>();
        userDelta = createAccountAssignmentUserDelta(USER_JACK_OID, RESOURCE_DUMMY_RED_OID, null, false);
        deltas.add(userDelta);

		// WHEN
		modelService.executeChanges(deltas, null, task, result);

		// THEN
		result.computeStatus();
        TestUtil.assertSuccess(result);

        // Let's wait for the task to give it a change to screw up
        waitForTaskNextRunAssertSuccess(TASK_TRIGGER_SCANNER_OID, true);

		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("Jack", userJack);
		assertUserJack(userJack, "Jack Sparrow", "Jack", "Sparrow");

		String accountRedOid = getLinkRefOid(userJack, RESOURCE_DUMMY_RED_OID);
		PrismObject<ShadowType> accountRed = getShadowModel(accountRedOid);

		XMLGregorianCalendar start = clock.currentTimeXMLGregorianCalendar();
        start.add(XmlTypeConverter.createDuration(true, 0, 0, 25, 0, 0, 0));
        XMLGregorianCalendar end = clock.currentTimeXMLGregorianCalendar();
        end.add(XmlTypeConverter.createDuration(true, 0, 0, 35, 0, 0, 0));
		assertTrigger(accountRed, RecomputeTriggerHandler.HANDLER_URI, start, end);
		assertAdministrativeStatusDisabled(accountRed);
		assertDisableReasonShadow(accountRed, SchemaConstants.MODEL_DISABLE_REASON_DEPROVISION);

		assertDummyAccount(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME, "Jack Sparrow", false);
	}

	/**
	 * Move time a month ahead. The account that was disabled in a previous test should be
	 * deleted now.
	 */
	@Test
    public void test215JackDummyAccountDeleteAfterMonth() throws Exception {
		final String TEST_NAME = "test215JackDummyAccountDeleteAfterMonth";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestMapping.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        XMLGregorianCalendar time = clock.currentTimeXMLGregorianCalendar();
        // A month and a day, to make sure we move past the trigger
        time.add(XmlTypeConverter.createDuration(true, 0, 1, 1, 0, 0, 0));

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        clock.override(time);

        waitForTaskNextRunAssertSuccess(TASK_TRIGGER_SCANNER_OID, true);

        // THEN
        TestUtil.displayThen(TEST_NAME);

        assertNoDummyAccount(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
	}

	@Test
    public void test220AddDrake() throws Exception {
		final String TEST_NAME = "test220AddDrake";
        TestUtil.displayTestTitle(this, TEST_NAME);

        XMLGregorianCalendar start = clock.currentTimeXMLGregorianCalendar();
        display("Start", start);

        PrismObject<UserType> userDrake = PrismTestUtil.parseObject(USER_DRAKE_FILE);
        UserType userDrakeType = userDrake.asObjectable();

        // Activation
        ActivationType activationType = new ActivationType();
		userDrakeType.setActivation(activationType);
		drakeValidFrom = clock.currentTimeXMLGregorianCalendar();
		drakeValidFrom.add(XmlTypeConverter.createDuration(true, 0, 0, 10, 0, 0, 0));
		activationType.setValidFrom(drakeValidFrom);
		drakeValidTo = clock.currentTimeXMLGregorianCalendar();
		drakeValidTo.add(XmlTypeConverter.createDuration(true, 0, 0, 80, 0, 0, 0));
		activationType.setValidTo(drakeValidTo);

		// Assignment: dummy red
		AssignmentType assignmentType = new AssignmentType();
		userDrakeType.getAssignment().add(assignmentType);
		ConstructionType constructionType = new ConstructionType();
		assignmentType.setConstruction(constructionType);
		constructionType.setKind(ShadowKindType.ACCOUNT);
		ObjectReferenceType resourceRedRef = new ObjectReferenceType();
		resourceRedRef.setOid(RESOURCE_DUMMY_RED_OID);
		constructionType.setResourceRef(resourceRedRef);

		// the following assignments are used only to generate superfluous searches
		// in validity scanner task
		AssignmentType dummyAssignmentType1 = new AssignmentType();
		userDrakeType.getAssignment().add(dummyAssignmentType1);
		dummyAssignmentType1.setTargetRef(ObjectTypeUtil.createObjectRef(ROLE_SUPERUSER_OID, ObjectTypes.ROLE));
		dummyAssignmentType1.setActivation(activationType.clone());

		AssignmentType dummyAssignmentType2 = new AssignmentType();
		userDrakeType.getAssignment().add(dummyAssignmentType2);
		dummyAssignmentType2.setTargetRef(ObjectTypeUtil.createObjectRef(ROLE_SUPERUSER_OID, ObjectTypes.ROLE));
		dummyAssignmentType2.setActivation(activationType.clone());
		dummyAssignmentType2.setDescription("just to differentiate");

		display("Drake before", userDrake);

		// WHEN
        TestUtil.displayWhen(TEST_NAME);

        addObject(userDrake);

        // THEN
        // Give the tasks a chance to screw up
        waitForValidityNextRunAssertSuccess();
        waitForTaskNextRunAssertSuccess(TASK_TRIGGER_SCANNER_OID, true);

        // Make sure that it is effectivelly disabled
        PrismObject<UserType> userDrakeAfter = getUser(USER_DRAKE_OID);
        display("Drake after", userDrakeAfter);
        assertEffectiveActivation(userDrakeAfter, ActivationStatusType.DISABLED);

        assertLinks(userDrakeAfter, 0);

        assertNoDummyAccount(RESOURCE_DUMMY_RED_NAME, "drake");
	}

	@Test
    public void test222Drake4DaysBeforeValidFrom() throws Exception {
		final String TEST_NAME = "test222Drake4DaysBeforeValidFrom";
        TestUtil.displayTestTitle(this, TEST_NAME);

        XMLGregorianCalendar start = (XMLGregorianCalendar) drakeValidFrom.clone();
        start.add(XmlTypeConverter.createDuration(false, 0, 0, 4, 0, 0, 0));
        clock.override(start);
        display("Start", start);

		// WHEN
        // just wait
        waitForValidityNextRunAssertSuccess();
        waitForTaskNextRunAssertSuccess(TASK_TRIGGER_SCANNER_OID, true);

        // THEN
        // Make sure that it is effectivelly disabled
        PrismObject<UserType> userDrakeAfter = getUser(USER_DRAKE_OID);
        display("Drake after", userDrakeAfter);
        assertEffectiveActivation(userDrakeAfter, ActivationStatusType.DISABLED);

        String accountRedOid = getLinkRefOid(userDrakeAfter, RESOURCE_DUMMY_RED_OID);
        PrismObject<ShadowType> accountRed = getShadowModel(accountRedOid);
        display("Drake account RED after", accountRed);

        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, "drake", "Francis Drake", false);
	}

	@Test
    public void test224Drake1DaysAfterValidFrom() throws Exception {
		final String TEST_NAME = "test224Drake1DaysAfterValidFrom";
        TestUtil.displayTestTitle(this, TEST_NAME);

        XMLGregorianCalendar start = (XMLGregorianCalendar) drakeValidFrom.clone();
        start.add(XmlTypeConverter.createDuration(true, 0, 0, 1, 0, 0, 0));
        clock.override(start);
        display("Start", start);

		// WHEN
        // just wait
        waitForValidityNextRunAssertSuccess();
        waitForTaskNextRunAssertSuccess(TASK_TRIGGER_SCANNER_OID, true);

        // THEN
        PrismObject<UserType> userDrakeAfter = getUser(USER_DRAKE_OID);
        display("Drake after", userDrakeAfter);
        assertEffectiveActivation(userDrakeAfter, ActivationStatusType.ENABLED);

        String accountRedOid = getLinkRefOid(userDrakeAfter, RESOURCE_DUMMY_RED_OID);
        PrismObject<ShadowType> accountRed = getShadowModel(accountRedOid);
        display("Drake account RED after", accountRed);

        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, "drake", "Francis Drake", true);
	}

	@Test
    public void test226Drake1DayBeforeValidTo() throws Exception {
		final String TEST_NAME = "test226Drake1DayBeforeValidTo";
        TestUtil.displayTestTitle(this, TEST_NAME);

		XMLGregorianCalendar start = (XMLGregorianCalendar) drakeValidTo.clone();
        start.add(XmlTypeConverter.createDuration(false, 0, 0, 1, 0, 0, 0));
        clock.override(start);
        display("Start", start);

		// WHEN
        // just wait
        waitForValidityNextRunAssertSuccess();
        waitForTaskNextRunAssertSuccess(TASK_TRIGGER_SCANNER_OID, true);

        // THEN
        PrismObject<UserType> userDrakeAfter = getUser(USER_DRAKE_OID);
        display("Drake after", userDrakeAfter);
        assertEffectiveActivation(userDrakeAfter, ActivationStatusType.ENABLED);

        String accountRedOid = getLinkRefOid(userDrakeAfter, RESOURCE_DUMMY_RED_OID);
        PrismObject<ShadowType> accountRed = getShadowModel(accountRedOid);
        display("Drake account RED after", accountRed);

        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, "drake", "Francis Drake", true);
	}

	@Test
    public void test228Drake1DayAfterValidTo() throws Exception {
		final String TEST_NAME = "test228Drake1DayAfterValidTo";
        TestUtil.displayTestTitle(this, TEST_NAME);

        XMLGregorianCalendar start = (XMLGregorianCalendar) drakeValidTo.clone();
        start.add(XmlTypeConverter.createDuration(true, 0, 0, 1, 0, 0, 0));
        clock.override(start);
        display("Start", start);

		// WHEN
        // just wait
        waitForValidityNextRunAssertSuccess();
        waitForTaskNextRunAssertSuccess(TASK_TRIGGER_SCANNER_OID, true);

        // THEN
        PrismObject<UserType> userDrakeAfter = getUser(USER_DRAKE_OID);
        display("Drake after", userDrakeAfter);
        assertEffectiveActivation(userDrakeAfter, ActivationStatusType.DISABLED);

        String accountRedOid = getLinkRefOid(userDrakeAfter, RESOURCE_DUMMY_RED_OID);
        PrismObject<ShadowType> accountRed = getShadowModel(accountRedOid);
        display("Drake account RED after", accountRed);
        assertDisableReasonShadow(accountRed, SchemaConstants.MODEL_DISABLE_REASON_MAPPED);

        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, "drake", "Francis Drake", false);
	}

	@Test
    public void test230Drake20DaysAfterValidTo() throws Exception {
		final String TEST_NAME = "test230Drake20DaysAfterValidTo";
        TestUtil.displayTestTitle(this, TEST_NAME);

        XMLGregorianCalendar start = (XMLGregorianCalendar) drakeValidTo.clone();
        start.add(XmlTypeConverter.createDuration(true, 0, 0, 20, 0, 0, 0));
        clock.override(start);
        display("Start", start);

		// WHEN
        // just wait
        waitForValidityNextRunAssertSuccess();
        waitForTaskNextRunAssertSuccess(TASK_TRIGGER_SCANNER_OID, true);

        // THEN
        PrismObject<UserType> userDrakeAfter = getUser(USER_DRAKE_OID);
        display("Drake after", userDrakeAfter);
        assertEffectiveActivation(userDrakeAfter, ActivationStatusType.DISABLED);

        String accountRedOid = getLinkRefOid(userDrakeAfter, RESOURCE_DUMMY_RED_OID);
        PrismObject<ShadowType> accountRed = getShadowModel(accountRedOid);
        display("Drake account RED after", accountRed);
        assertDisableReasonShadow(accountRed, SchemaConstants.MODEL_DISABLE_REASON_MAPPED);

        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, "drake", "Francis Drake", false);
	}

	@Test
    public void test232Drake40DaysAfterValidTo() throws Exception {
		final String TEST_NAME = "test232Drake40DaysAfterValidTo";
        TestUtil.displayTestTitle(this, TEST_NAME);

        XMLGregorianCalendar start = (XMLGregorianCalendar) drakeValidTo.clone();
        start.add(XmlTypeConverter.createDuration(true, 0, 0, 40, 0, 0, 0));
        clock.override(start);
        display("Start", start);

		// WHEN
        // just wait
        waitForValidityNextRunAssertSuccess();
        waitForTaskNextRunAssertSuccess(TASK_TRIGGER_SCANNER_OID, true);

        // THEN
        PrismObject<UserType> userDrakeAfter = getUser(USER_DRAKE_OID);
        display("Drake after", userDrakeAfter);
        assertEffectiveActivation(userDrakeAfter, ActivationStatusType.DISABLED);

        assertLinks(userDrakeAfter, 0);

        assertNoDummyAccount(RESOURCE_DUMMY_RED_NAME, "drake");
	}

	/**
	 * Elaine's red account was explicitly disabled. We have moved the time ahead in previous tests.
	 * But this account should remain as it is.
	 */
	@Test
    public void test250CheckAccountRedElaine() throws Exception {
		final String TEST_NAME = "test250CheckAccountRedElaine";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN

		// WHEN
        // nothing to do

		// THEN

        PrismObject<ShadowType> accountShadow = getShadowModel(ACCOUNT_SHADOW_ELAINE_DUMMY_RED_OID);
		assertDisableReasonShadow(accountShadow, SchemaConstants.MODEL_DISABLE_REASON_EXPLICIT);
	}

	private XMLGregorianCalendar judgeAssignmentValidFrom;
	private XMLGregorianCalendar judgeAssignmentValidTo;

	@Test
    public void test300HermanAssignJudgeNotYetValid() throws Exception {
		final String TEST_NAME = "test300HermanAssignJudgeNotYetValid";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TestValidityRecomputeTask.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        ActivationType activationType = new ActivationType();
        judgeAssignmentValidFrom = clock.currentTimeXMLGregorianCalendar();
        judgeAssignmentValidFrom.add(XmlTypeConverter.createDuration(10*60*1000)); // 10 minutes ahead
        activationType.setValidFrom(judgeAssignmentValidFrom);
        judgeAssignmentValidTo = clock.currentTimeXMLGregorianCalendar();
        judgeAssignmentValidTo.add(XmlTypeConverter.createDuration(30*60*1000)); // 30 minutes ahead
        activationType.setValidTo(judgeAssignmentValidTo);
        display("Assignment validFrom", judgeAssignmentValidFrom);
        display("Assignment validTo", judgeAssignmentValidTo);

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        assignRole(USER_HERMAN_OID, ROLE_JUDGE_OID, activationType, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        assertNoDummyAccount(null, USER_HERMAN_USERNAME);
	}

	@Test
    public void test310HermanAssignJudgeBecomesValid() throws Exception {
		final String TEST_NAME = "test310HermanAssignJudgeBecomesValid";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TestValidityRecomputeTask.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        PrismObject<UserType> user = getUser(USER_HERMAN_OID);
        display("User before", user);
        XMLGregorianCalendar start = (XMLGregorianCalendar) judgeAssignmentValidFrom.clone();
        start.add(XmlTypeConverter.createDuration(1*60*1000));
        clock.override(start);
        display("Start", start);

        // WHEN
        // just wait
        waitForValidityNextRunAssertSuccess();

        assertRoleJudgeValid(TEST_NAME, task, result);
	}

	@Test
    public void test315HermanAssignJudgeBecomesInValid() throws Exception {
		final String TEST_NAME = "test315HermanAssignJudgeBecomesInValid";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TestValidityRecomputeTask.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        XMLGregorianCalendar start = (XMLGregorianCalendar) judgeAssignmentValidTo.clone();
        start.add(XmlTypeConverter.createDuration(1*60*1000));
        clock.override(start);
        display("Start", start);

        // WHEN
        // just wait
        waitForValidityNextRunAssertSuccess();

        assertRoleJudgeInValid(TEST_NAME, task, result);
	}

	private void assertRoleJudgeValid(final String TEST_NAME, Task task, OperationResult result) throws Exception {
        assertDummyAccount(null, USER_HERMAN_USERNAME);
        PrismObject<UserType> user = getUser(USER_HERMAN_OID);
        display("User after", user);
        assertLinks(user, 1);
	}

	private void assertRoleJudgeInValid(final String TEST_NAME, Task task, OperationResult result) throws Exception {
        assertNoDummyAccount(null, USER_HERMAN_USERNAME);
        PrismObject<UserType> user = getUser(USER_HERMAN_OID);
        display("User after", user);
        assertLinks(user, 0);
	}

	private void modifyAssignmentAdministrativeStatus(String userOid, long assignmentId, ActivationStatusType status, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException, PolicyViolationException, SecurityViolationException {
		if (status == null) {
			modifyObjectReplaceProperty(UserType.class, userOid,
	        		new ItemPath(
	        				new NameItemPathSegment(UserType.F_ASSIGNMENT),
	        				new IdItemPathSegment(assignmentId),
	        				new NameItemPathSegment(AssignmentType.F_ACTIVATION),
	        				new NameItemPathSegment(ActivationType.F_ADMINISTRATIVE_STATUS)),
	        		task, result);
		} else {
			modifyObjectReplaceProperty(UserType.class, userOid,
        		new ItemPath(
        				new NameItemPathSegment(UserType.F_ASSIGNMENT),
        				new IdItemPathSegment(assignmentId),
        				new NameItemPathSegment(AssignmentType.F_ACTIVATION),
        				new NameItemPathSegment(ActivationType.F_ADMINISTRATIVE_STATUS)),
        		task, result, status);
		}
	}

	protected void waitForValidityTaskFinish() throws Exception {
		waitForTaskFinish(TASK_VALIDITY_SCANNER_OID, true);
	}

	protected void waitForValidityTaskStart() throws Exception {
		waitForTaskStart(TASK_VALIDITY_SCANNER_OID, false);
	}

	protected void waitForValidityNextRunAssertSuccess() throws Exception {
		waitForTaskNextRunAssertSuccess(TASK_VALIDITY_SCANNER_OID, true);
	}
}
