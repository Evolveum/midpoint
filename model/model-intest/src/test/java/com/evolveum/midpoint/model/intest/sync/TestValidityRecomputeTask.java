/*
 * Copyright (c) 2013 Evolveum
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
import static com.evolveum.midpoint.test.IntegrationTestTools.display;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.datatype.XMLGregorianCalendar;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.model.intest.AbstractInitializedModelIntegrationTest;
import com.evolveum.midpoint.model.intest.TestActivation;
import com.evolveum.midpoint.model.intest.TestMapping;
import com.evolveum.midpoint.model.intest.TestTriggerTask;
import com.evolveum.midpoint.model.trigger.RecomputeTriggerHandler;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.IdItemPathSegment;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AssignmentPolicyEnforcementType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.TimeIntervalStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;

/**
 * @author Radovan Semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestValidityRecomputeTask extends AbstractInitializedModelIntegrationTest {

	private static final XMLGregorianCalendar LONG_LONG_TIME_AGO = XmlTypeConverter.createXMLGregorianCalendar(1111, 1, 1, 12, 00, 00);

	private XMLGregorianCalendar drakeValidFrom;
	private XMLGregorianCalendar drakeValidTo;
	
	@Test
    public void test100ImportValidityScannerTask() throws Exception {
		final String TEST_NAME = "test100ImportValidityScannerTask";
        TestUtil.displayTestTile(this, TEST_NAME);

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
        importObjectFromFile(TASK_VALIDITY_SCANNER_FILENAME);
		
        waitForTaskStart(TASK_VALIDITY_SCANNER_OID, false);
        waitForTaskFinish(TASK_VALIDITY_SCANNER_OID, true);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        XMLGregorianCalendar endCal = clock.currentTimeXMLGregorianCalendar();
        assertLastRecomputeTimestamp(TASK_VALIDITY_SCANNER_OID, startCal, endCal);
        
        PrismObject<UserType> userHermanAfter = getUser(USER_HERMAN_OID);
        assertEffectiveActivation(userHermanAfter, ActivationStatusType.ENABLED);
        assertValidityStatus(userHermanAfter, TimeIntervalStatusType.IN);
	}
	
	@Test
    public void test110HermanAssignJudgeDisabled() throws Exception {
		final String TEST_NAME = "test110HermanAssignJudgeDisabled";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TestValidityRecomputeTask.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        ActivationType activationType = new ActivationType();
        activationType.setAdministrativeStatus(ActivationStatusType.DISABLED);
        
        testHermanAssignRoleJudgeInvalid(TEST_NAME, activationType, task, result);
	}
	
	@Test
    public void test111HermanAssignJudgeNotYetValid() throws Exception {
		final String TEST_NAME = "test111HermanAssignJudgeNotYetValid";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TestValidityRecomputeTask.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        ActivationType activationType = new ActivationType();
        XMLGregorianCalendar validFrom = clock.currentTimeXMLGregorianCalendar();
        validFrom.add(XmlTypeConverter.createDuration(60*60*1000)); // one hour ahead
        activationType.setValidFrom(validFrom);
        
        testHermanAssignRoleJudgeInvalid(TEST_NAME, activationType, task, result);
	}
	
	@Test
    public void test112HermanAssignJudgeAfterValidity() throws Exception {
		final String TEST_NAME = "test112HermanAssignJudgeAfterValidity";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TestValidityRecomputeTask.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        ActivationType activationType = new ActivationType();
        XMLGregorianCalendar validTo = clock.currentTimeXMLGregorianCalendar();
        validTo.add(XmlTypeConverter.createDuration(-60*60*1000)); // one hour ago
        activationType.setValidTo(validTo);
        
        testHermanAssignRoleJudgeInvalid(TEST_NAME, activationType, task, result);
	}
	
	private void testHermanAssignRoleJudgeInvalid(final String TEST_NAME, ActivationType activationType, Task task, OperationResult result) throws Exception {
	    
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        assignRole(USER_HERMAN_OID, ROLE_JUDGE_OID, activationType, task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        assertNoDummyAccount(null, USER_HERMAN_USERNAME);
        
		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        waitForTaskNextRun(TASK_VALIDITY_SCANNER_OID, true);
		
        // THEN
        TestUtil.displayThen(TEST_NAME);
        assertNoDummyAccount(null, USER_HERMAN_USERNAME);

        PrismObject<UserType> user = getUser(USER_HERMAN_OID);
        display("User after", user);
        assertNoLinkedAccount(user);
        
        // CLEANUP
        unassignAllRoles(USER_HERMAN_OID);
        assertNoDummyAccount(null, USER_HERMAN_USERNAME);
	}
	
	@Test
    public void test115HermanAssignJudgeEnabled() throws Exception {
		final String TEST_NAME = "test115HermanAssignJudgeEnabled";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TestValidityRecomputeTask.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        ActivationType activationType = new ActivationType();
        activationType.setAdministrativeStatus(ActivationStatusType.ENABLED);
        
        testHermanAssignRoleJudgeValid(TEST_NAME, activationType, task, result);
	}
	
	@Test
    public void test115HermanAssignJudgeValid() throws Exception {
		final String TEST_NAME = "test115HermanAssignJudgeValid";
        TestUtil.displayTestTile(this, TEST_NAME);

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
        
        testHermanAssignRoleJudgeValid(TEST_NAME, activationType, task, result);
	}
		
	private void testHermanAssignRoleJudgeValid(final String TEST_NAME, ActivationType activationType, Task task, OperationResult result) throws Exception {
	    
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        assignRole(USER_HERMAN_OID, ROLE_JUDGE_OID, activationType, task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        assertDummyAccount(null, USER_HERMAN_USERNAME);
        
		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        waitForTaskNextRun(TASK_VALIDITY_SCANNER_OID, true);
		
        // THEN
        TestUtil.displayThen(TEST_NAME);
        assertDummyAccount(null, USER_HERMAN_USERNAME);

        PrismObject<UserType> user = getUser(USER_HERMAN_OID);
        display("User after", user);
        assertLinks(user, 1);
        
        // CLEANUP
        unassignAllRoles(USER_HERMAN_OID);
        assertNoDummyAccount(null, USER_HERMAN_USERNAME);
	}
	
	@Test
    public void test120HermanDisableAssignmentJudge() throws Exception {
		final String TEST_NAME = "test120HermanDisableAssignmentJudge";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TestValidityRecomputeTask.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        ActivationType activationType = new ActivationType();
        activationType.setAdministrativeStatus(ActivationStatusType.ENABLED);
        assignRole(USER_HERMAN_OID, ROLE_JUDGE_OID, activationType, task, result);
        assertDummyAccount(null, USER_HERMAN_USERNAME);
        AssignmentType judgeAssignment = getJudgeAssignment(USER_HERMAN_OID);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        modifyObjectReplace(UserType.class, USER_HERMAN_OID, 
        		new ItemPath(
        				new NameItemPathSegment(UserType.F_ASSIGNMENT),
        				new IdItemPathSegment(judgeAssignment.getId()),
        				new NameItemPathSegment(AssignmentType.F_ACTIVATION),
        				new NameItemPathSegment(ActivationType.F_ADMINISTRATIVE_STATUS)), 
        		task, result, ActivationStatusType.DISABLED);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        PrismObject<UserType> user = getUser(USER_HERMAN_OID);
        display("User after", user);
        assertNoDummyAccount(null, USER_HERMAN_USERNAME);
	}
	
	private AssignmentType getJudgeAssignment(String userOid) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException {
		PrismObject<UserType> user = getUser(userOid);
		List<AssignmentType> assignments = user.asObjectable().getAssignment();
		assertEquals("Wrong num ass", 1, assignments.size());
		return assignments.iterator().next();
	}

	@Test
    public void test190HermanGoesInvalid() throws Exception {
		final String TEST_NAME = "test190HermanGoesInvalid";
        TestUtil.displayTestTile(this, TEST_NAME);

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
        waitForTaskNextRun(TASK_VALIDITY_SCANNER_OID, true);
		
        // THEN
        TestUtil.displayThen(TEST_NAME);
        
        // THEN
        XMLGregorianCalendar endCal = clock.currentTimeXMLGregorianCalendar();

        PrismObject<UserType> userHermanAfter = getUser(USER_HERMAN_OID);
        assertEffectiveActivation(userHermanAfter, ActivationStatusType.DISABLED);
        assertValidityStatus(userHermanAfter, TimeIntervalStatusType.AFTER);

        assertLastRecomputeTimestamp(TASK_VALIDITY_SCANNER_OID, startCal, endCal);
	}
	
	@Test
    public void test200ImportTriggerScannerTask() throws Exception {
		final String TEST_NAME = "test200ImportTriggerScannerTask";
        TestUtil.displayTestTile(this, TEST_NAME);

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
        assertLastRecomputeTimestamp(TASK_TRIGGER_SCANNER_OID, startCal, endCal);
        
	}
	
	/**
	 * Explicitly disable Elaine's red account. Do this at the beginning of the test. We will
	 * move time ahead in later tests. This account should remain here exactly like this
	 * at the end of all tests.
	 */
	@Test
    public void test205AccountRedElaineDisable() throws Exception {
		final String TEST_NAME = "test205AccountRedElaineDisable";
        TestUtil.displayTestTile(this, TEST_NAME);

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
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestMapping.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        // assign
        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<UserType> userDelta = createAccountAssignmentUserDelta(USER_JACK_OID, 
        		RESOURCE_DUMMY_RED_OID, null, true);
        deltas.add(userDelta);
                
		// WHEN
		modelService.executeChanges(deltas, null, task, result);
		
		// THEN
		assertDummyAccount(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME, "Jack Sparrow", true);
        
		// unassign
        deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        userDelta = createAccountAssignmentUserDelta(USER_JACK_OID, RESOURCE_DUMMY_RED_OID, null, false);
        deltas.add(userDelta);
                
		// WHEN
		modelService.executeChanges(deltas, null, task, result);
		
		// THEN
		result.computeStatus();
        TestUtil.assertSuccess(result);
        
        // Let's wait for the task to give it a change to screw up
        waitForTaskNextRun(TASK_TRIGGER_SCANNER_OID, true);
        
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
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestMapping.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        XMLGregorianCalendar time = clock.currentTimeXMLGregorianCalendar();
        // A month and a day, to make sure we move past the trigger
        time.add(XmlTypeConverter.createDuration(true, 0, 1, 1, 0, 0, 0));
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        clock.override(time);
        
        waitForTaskNextRun(TASK_TRIGGER_SCANNER_OID, true);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        
        assertNoDummyAccount(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
	}
	
	@Test
    public void test220AddDrake() throws Exception {
		final String TEST_NAME = "test220AddDrake";
        TestUtil.displayTestTile(this, TEST_NAME);

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
		
		display("Drake before", userDrake);
		
		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        
        addObject(userDrake);
        
        // THEN
        // Give the tasks a chance to screw up
        waitForTaskNextRun(TASK_VALIDITY_SCANNER_OID, true);
        waitForTaskNextRun(TASK_TRIGGER_SCANNER_OID, true);
        
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
        TestUtil.displayTestTile(this, TEST_NAME);

        XMLGregorianCalendar start = (XMLGregorianCalendar) drakeValidFrom.clone();
        start.add(XmlTypeConverter.createDuration(false, 0, 0, 4, 0, 0, 0));
        clock.override(start);
        display("Start", start);
        		
		// WHEN
        // just wait
        waitForTaskNextRun(TASK_VALIDITY_SCANNER_OID, true);
        waitForTaskNextRun(TASK_TRIGGER_SCANNER_OID, true);
        
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
        TestUtil.displayTestTile(this, TEST_NAME);

        XMLGregorianCalendar start = (XMLGregorianCalendar) drakeValidFrom.clone();
        start.add(XmlTypeConverter.createDuration(true, 0, 0, 1, 0, 0, 0));
        clock.override(start);
        display("Start", start);
        		
		// WHEN
        // just wait
        waitForTaskNextRun(TASK_VALIDITY_SCANNER_OID, true);
        waitForTaskNextRun(TASK_TRIGGER_SCANNER_OID, true);
        
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
        TestUtil.displayTestTile(this, TEST_NAME);

        XMLGregorianCalendar start = (XMLGregorianCalendar) drakeValidTo.clone();
        start.add(XmlTypeConverter.createDuration(false, 0, 0, 1, 0, 0, 0));
        clock.override(start);
        display("Start", start);
        		
		// WHEN
        // just wait
        waitForTaskNextRun(TASK_VALIDITY_SCANNER_OID, true);
        waitForTaskNextRun(TASK_TRIGGER_SCANNER_OID, true);
        
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
        TestUtil.displayTestTile(this, TEST_NAME);

        XMLGregorianCalendar start = (XMLGregorianCalendar) drakeValidTo.clone();
        start.add(XmlTypeConverter.createDuration(true, 0, 0, 1, 0, 0, 0));
        clock.override(start);
        display("Start", start);
        		
		// WHEN
        // just wait
        waitForTaskNextRun(TASK_VALIDITY_SCANNER_OID, true);
        waitForTaskNextRun(TASK_TRIGGER_SCANNER_OID, true);
        
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
        TestUtil.displayTestTile(this, TEST_NAME);

        XMLGregorianCalendar start = (XMLGregorianCalendar) drakeValidTo.clone();
        start.add(XmlTypeConverter.createDuration(true, 0, 0, 20, 0, 0, 0));
        clock.override(start);
        display("Start", start);
        		
		// WHEN
        // just wait
        waitForTaskNextRun(TASK_VALIDITY_SCANNER_OID, true);
        waitForTaskNextRun(TASK_TRIGGER_SCANNER_OID, true);
        
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
        TestUtil.displayTestTile(this, TEST_NAME);

        XMLGregorianCalendar start = (XMLGregorianCalendar) drakeValidTo.clone();
        start.add(XmlTypeConverter.createDuration(true, 0, 0, 40, 0, 0, 0));
        clock.override(start);
        display("Start", start);
        		
		// WHEN
        // just wait
        waitForTaskNextRun(TASK_VALIDITY_SCANNER_OID, true);
        waitForTaskNextRun(TASK_TRIGGER_SCANNER_OID, true);
        
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
        TestUtil.displayTestTile(this, TEST_NAME);

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
        TestUtil.displayTestTile(this, TEST_NAME);

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
        
        testHermanAssignRoleJudgeInvalid(TEST_NAME, activationType, task, result);
	}
	
	@Test(enabled=false) // MID-1830
    public void test310HermanAssignJudgeBecomesValid() throws Exception {
		final String TEST_NAME = "test310HermanAssignJudgeBecomesValid";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TestValidityRecomputeTask.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        XMLGregorianCalendar start = (XMLGregorianCalendar) judgeAssignmentValidFrom.clone();
        start.add(XmlTypeConverter.createDuration(1*60*1000));
        clock.override(start);
        display("Start", start);
        
        // WHEN
        // just wait
        waitForTaskNextRun(TASK_VALIDITY_SCANNER_OID, true);
        
        assertRoleJudgeValid(TEST_NAME, task, result);
	}
	
	@Test(enabled=false) // MID-1830
    public void test315HermanAssignJudgeBecomesInValid() throws Exception {
		final String TEST_NAME = "test315HermanAssignJudgeBecomesInValid";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TestValidityRecomputeTask.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        XMLGregorianCalendar start = (XMLGregorianCalendar) judgeAssignmentValidTo.clone();
        start.add(XmlTypeConverter.createDuration(1*60*1000));
        clock.override(start);
        display("Start", start);
        
        // WHEN
        // just wait
        waitForTaskNextRun(TASK_VALIDITY_SCANNER_OID, true);
        
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


}
