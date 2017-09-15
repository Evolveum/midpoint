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
package com.evolveum.midpoint.model.impl.lens;

import com.evolveum.midpoint.model.api.context.EvaluatedExclusionTrigger;
import com.evolveum.midpoint.model.api.context.EvaluatedPolicyRule;
import com.evolveum.midpoint.model.api.context.EvaluatedSituationTrigger;
import com.evolveum.midpoint.model.api.context.SynchronizationPolicyDecision;
import com.evolveum.midpoint.model.impl.util.RecordingProgressListener;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.builder.DeltaBuilder;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.testng.AssertJUnit.*;

/**
 * Tests triggering of exclusion, assignment and situation rules.
 * Also rule exceptions.
 *
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestPolicyRules extends AbstractLensTest {

	private static final String ROLE_JUDGE_POLICY_RULE_EXCLUSION_NAME = "criminal exclusion";

	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);
		setDefaultUserTemplate(USER_TEMPLATE_OID);

		repoAddObjectFromFile(USER_DRAKE_FILE, initResult);

		addObject(ROLE_PIRATE_FILE);
		addObject(ROLE_MUTINIER_FILE);
		addObject(ROLE_JUDGE_FILE);
		addObject(ROLE_CONSTABLE_FILE);
		addObject(ROLE_THIEF_FILE);

		addObjects(ROLE_CORP_FILES);

		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

		InternalMonitor.reset();
//		InternalMonitor.setTraceShadowFetchOperation(true);

//		DebugUtil.setPrettyPrintBeansAs(PrismContext.LANG_YAML);
	}

	@Test
	public void test005JackAttemptAssignRoleJudge() throws Exception {
		final String TEST_NAME = "test005JackAttemptAssignRoleJudge";
		TestUtil.displayTestTitle(this, TEST_NAME);

		// GIVEN
		Task task = taskManager.createTaskInstance(TestPolicyRules.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();

		LensContext<UserType> context = createUserLensContext();
		fillContextWithUser(context, USER_JACK_OID, result);
		addModificationToContextAssignRole(context, USER_JACK_OID, ROLE_JUDGE_OID);

		display("Input context", context);

		assertFocusModificationSanity(context);
		rememberCounter(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);

		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		projector.project(context, "test", task, result);

		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
		TestUtil.assertSuccess(result);

		dumpPolicyRules(context);
		//dumpPolicySituations(context);

		assertEvaluatedTargetPolicyRules(context, 7);
		assertTargetTriggers(context, PolicyConstraintKindType.OBJECT_STATE, 2);
		assertTargetTriggers(context, PolicyConstraintKindType.ASSIGNMENT_MODIFICATION, 4);
		assertTargetTriggers(context, null, 6);
	}

	@Test(enabled = false)
	public void test007JackAttemptAssignRoleJudgeAsOwner() throws Exception {
		final String TEST_NAME = "test007JackAttemptAssignRoleJudgeAsOwner";
		TestUtil.displayTestTitle(this, TEST_NAME);

		// GIVEN
		Task task = taskManager.createTaskInstance(TestPolicyRules.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();

		LensContext<UserType> context = createUserLensContext();
		fillContextWithUser(context, USER_JACK_OID, result);
		addModificationToContextAssignRole(context, USER_JACK_OID, ROLE_PIRATE_OID,
				assignment -> assignment.getTargetRef().setRelation(SchemaConstants.ORG_OWNER)
		);

		display("Input context", context);

		assertFocusModificationSanity(context);
		rememberCounter(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);

		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		projector.project(context, "test", task, result);

		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
		TestUtil.assertSuccess(result);

		dumpPolicyRules(context);
		//dumpPolicySituations(context);

		assertEvaluatedTargetPolicyRules(context, 4);
		assertTargetTriggers(context, PolicyConstraintKindType.ASSIGNMENT_MODIFICATION, 0);
	}


	/**
	 * Mostly preparation for other tests. But good check for no exclusion conflict.
	 */
	@Test
    public void test010JackAssignRoleJudge() throws Exception {
		final String TEST_NAME = "test010JackAssignRoleJudge";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestPolicyRules.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        assignRole(USER_JACK_OID, ROLE_JUDGE_OID, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertAssignedRole(userAfter, ROLE_JUDGE_OID);
	}

	/**
	 * We expect 2 assignments rules to be triggered: unassign + all operations.
	 */
	@Test
	public void test020JackUnassignRoleJudge() throws Exception {
		final String TEST_NAME = "test020JackUnassignRoleJudge";
		TestUtil.displayTestTitle(this, TEST_NAME);

		// GIVEN
		Task task = taskManager.createTaskInstance(TestPolicyRules.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();

		LensContext<UserType> context = createUserLensContext();
		fillContextWithUser(context, USER_JACK_OID, result);
		addModificationToContextUnassignRole(context, USER_JACK_OID, ROLE_JUDGE_OID);

		display("Input context", context);

		assertFocusModificationSanity(context);
		rememberCounter(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);

		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		projector.project(context, "test", task, result);

		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
		TestUtil.assertSuccess(result);

		dumpPolicyRules(context);
		dumpPolicySituations(context);

		assertEvaluatedTargetPolicyRules(context, 7);
		assertTargetTriggers(context, PolicyConstraintKindType.ASSIGNMENT_MODIFICATION, 2);
		assertTargetTriggers(context, null, 2);
	}


	/**
	 * No exclusion here. The assignment should go smoothly.
	 */
	@Test
    public void test100AssignRoleMutinierToJack() throws Exception {
		final String TEST_NAME = "test100AssignRoleMutinierToJack";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestPolicyRules.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        LensContext<UserType> context = createUserLensContext();
        fillContextWithUser(context, USER_JACK_OID, result);
        addModificationToContextAssignRole(context, USER_JACK_OID, ROLE_MUTINIER_OID);

        display("Input context", context);

        assertFocusModificationSanity(context);
        rememberCounter(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        projector.project(context, "test", task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertAssignAccountToJack(context);

        DeltaSetTriple<EvaluatedAssignmentImpl<UserType>> evaluatedAssignmentTriple =
        		(DeltaSetTriple)context.getEvaluatedAssignmentTriple();
//        display("Output evaluatedAssignmentTriple", evaluatedAssignmentTriple);

        dumpPolicyRules(context);
        dumpPolicySituations(context);
        assertEvaluatedTargetPolicyRules(context, 7);
        assertTargetTriggers(context,  null, 0);
	}

	@Test
    public void test110AssignRolePirateToJack() throws Exception {
		final String TEST_NAME = "test110AssignRolePirateToJack";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestPolicyRules.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        LensContext<UserType> context = createUserLensContext();
        fillContextWithUser(context, USER_JACK_OID, result);
        addModificationToContextAssignRole(context, USER_JACK_OID, ROLE_PIRATE_OID);

        display("Input context", context);

        assertFocusModificationSanity(context);
        rememberCounter(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        projector.project(context, "test", task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertAssignAccountToJack(context);

        DeltaSetTriple<EvaluatedAssignmentImpl<UserType>> evaluatedAssignmentTriple =
        		(DeltaSetTriple)context.getEvaluatedAssignmentTriple();
//        display("Output evaluatedAssignmentTriple", evaluatedAssignmentTriple);

        dumpPolicyRules(context);
        dumpPolicySituations(context);
        assertEvaluatedTargetPolicyRules(context, 7);
        EvaluatedExclusionTrigger trigger = (EvaluatedExclusionTrigger) assertTriggeredTargetPolicyRule(context, null, PolicyConstraintKindType.EXCLUSION, 1, true);
        assertNotNull("No conflicting assignment in trigger", trigger.getConflictingAssignment());
        assertEquals("Wrong conflicting assignment in trigger", ROLE_PIRATE_OID, trigger.getConflictingAssignment().getTarget().getOid());
	}

	/**
	 * Assignment with an exception from the exclusion rule.
	 */
	@Test
    public void test112AssignRolePirateWithExceptionToJack() throws Exception {
		final String TEST_NAME = "test112AssignRolePirateWithExceptionToJack";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestPolicyRules.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        LensContext<UserType> context = createUserLensContext();
        fillContextWithUser(context, USER_JACK_OID, result);
        addModificationToContextAssignRole(context, USER_JACK_OID, ROLE_PIRATE_OID,
        		assignment -> {
        			PolicyExceptionType policyException = new PolicyExceptionType();
        			policyException.setRuleName(ROLE_JUDGE_POLICY_RULE_EXCLUSION_NAME);
					assignment.getPolicyException().add(policyException);
        		});

        display("Input context", context);

        assertFocusModificationSanity(context);
        rememberCounter(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        projector.project(context, "test", task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertAssignAccountToJack(context);

        DeltaSetTriple<EvaluatedAssignmentImpl<UserType>> evaluatedAssignmentTriple =
        		(DeltaSetTriple)context.getEvaluatedAssignmentTriple();
//        display("Output evaluatedAssignmentTriple", evaluatedAssignmentTriple);

        dumpPolicyRules(context);
        dumpPolicySituations(context);
        List<EvaluatedPolicyRule> evaluatedRules = assertEvaluatedTargetPolicyRules(context, 7);
        assertTargetTriggers(context,  null, 0);

        EvaluatedPolicyRule evaluatedPolicyRule = evaluatedRules.get(0);
        Collection<PolicyExceptionType> exceptions = evaluatedPolicyRule.getPolicyExceptions();
        assertEquals("Wrong number of exceptions", 1, exceptions.size());
        PolicyExceptionType policyException = exceptions.iterator().next();
        assertEquals("Wrong rule name in poliy excpetion", ROLE_JUDGE_POLICY_RULE_EXCLUSION_NAME, policyException.getRuleName());
	}

	@Test
    public void test120AssignRoleConstableToJack() throws Exception {
		final String TEST_NAME = "test120AssignRoleConstableToJack";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestPolicyRules.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        LensContext<UserType> context = createUserLensContext();
        fillContextWithUser(context, USER_JACK_OID, result);
        addModificationToContextAssignRole(context, USER_JACK_OID, ROLE_CONSTABLE_OID);

        display("Input context", context);

        assertFocusModificationSanity(context);
        rememberCounter(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        projector.project(context, "test", task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        display("Output context", context);

        DeltaSetTriple<EvaluatedAssignmentImpl<UserType>> evaluatedAssignmentTriple =
        		(DeltaSetTriple)context.getEvaluatedAssignmentTriple();
//        display("Output evaluatedAssignmentTriple", evaluatedAssignmentTriple);

        dumpPolicyRules(context);
        dumpPolicySituations(context);
        assertEvaluatedTargetPolicyRules(context, 8);
        // conflicting assignment was pruned, so the exclusion is no longer present here
//		EvaluatedExclusionTrigger trigger = (EvaluatedExclusionTrigger) assertTriggeredTargetPolicyRule(context, null, PolicyConstraintKindType.EXCLUSION, 1, true);
//        assertNotNull("No conflicting assignment in trigger", trigger.getConflictingAssignment());
//        assertEquals("Wrong conflicting assignment in trigger", ROLE_JUDGE_OID, trigger.getConflictingAssignment().getTarget().getOid());

        ObjectDelta<UserType> focusSecondaryDelta = context.getFocusContext().getSecondaryDelta();
        PrismAsserts.assertIsModify(focusSecondaryDelta);
        PrismAsserts.assertModifications(focusSecondaryDelta, 2);
        ContainerDelta<AssignmentType> assignmentDelta = focusSecondaryDelta.findContainerDelta(FocusType.F_ASSIGNMENT);
        assertEquals("Unexpected assignment secondary delta", 1, assignmentDelta.getValuesToDelete().size());
        PrismContainerValue<AssignmentType> deletedAssignment = assignmentDelta.getValuesToDelete().iterator().next();
        assertEquals("Wrong OID in deleted assignment", ROLE_JUDGE_OID, deletedAssignment.asContainerable().getTargetRef().getOid());

        ObjectDelta<ShadowType> accountSecondaryDelta = assertAssignAccountToJack(context);
        PrismAsserts.assertPropertyAdd(accountSecondaryDelta,
    		  getDummyResourceController().getAttributePath(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME),
    		  "Constable");
        PrismAsserts.assertPropertyDelete(accountSecondaryDelta,
      		  getDummyResourceController().getAttributePath(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME),
      		  "Honorable Justice");
	}

	/**
	 * Thief has got separated constraints and actions.
	 */
	@Test
	public void test150AssignRoleThiefToJack() throws Exception {
		final String TEST_NAME = "test150AssignRoleThiefToJack";
		TestUtil.displayTestTitle(this, TEST_NAME);

		// GIVEN
		Task task = taskManager.createTaskInstance(TestPolicyRules.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();

		LensContext<UserType> context = createUserLensContext();
		fillContextWithUser(context, USER_JACK_OID, result);
		addModificationToContextAssignRole(context, USER_JACK_OID, ROLE_THIEF_OID);

		display("Input context", context);

		assertFocusModificationSanity(context);
		rememberCounter(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);

		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		projector.project(context, "test", task, result);

		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
		TestUtil.assertSuccess(result);

		DeltaSetTriple<EvaluatedAssignmentImpl<UserType>> evaluatedAssignmentTriple =
				(DeltaSetTriple)context.getEvaluatedAssignmentTriple();
		display("Output evaluatedAssignmentTriple", evaluatedAssignmentTriple);

		dumpPolicyRules(context);
		dumpPolicySituations(context);

		assertEvaluatedTargetPolicyRules(context, 9);
		assertTargetTriggers(context, PolicyConstraintKindType.EXCLUSION, 1);
		assertTargetTriggers(context, PolicyConstraintKindType.SITUATION, 1);

		EvaluatedExclusionTrigger triggerExclusion = (EvaluatedExclusionTrigger) assertTriggeredTargetPolicyRule(context, null, PolicyConstraintKindType.EXCLUSION, 1, false);
		assertNotNull("No conflicting assignment in trigger", triggerExclusion.getConflictingAssignment());
		assertEquals("Wrong conflicting assignment in trigger", ROLE_JUDGE_OID, triggerExclusion.getConflictingAssignment().getTarget().getOid());

		EvaluatedSituationTrigger triggerSituation = (EvaluatedSituationTrigger) assertTriggeredTargetPolicyRule(context, null, PolicyConstraintKindType.SITUATION, 1, false);
		assertEquals("Wrong # of source rules", 1, triggerSituation.getSourceRules().size());
		EvaluatedPolicyRule sourceRule = triggerSituation.getSourceRules().iterator().next();
		EvaluatedExclusionTrigger sourceTrigger = (EvaluatedExclusionTrigger) sourceRule.getTriggers().iterator().next();
		assertNotNull("No conflicting assignment in source trigger", sourceTrigger.getConflictingAssignment());
		assertEquals("Wrong conflicting assignment in source trigger", ROLE_JUDGE_OID, sourceTrigger.getConflictingAssignment().getTarget().getOid());
	}

	/**
	 * Mostly preparation for other tests. But good check for no exclusion conflict.
	 */
	@Test
	public void test200JackAssignRoleContractor() throws Exception {
		final String TEST_NAME = "test200JackAssignRoleContractor";
		TestUtil.displayTestTitle(this, TEST_NAME);

		// GIVEN
		Task task = taskManager.createTaskInstance(TestPolicyRules.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();

		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		assignRole(USER_JACK_OID, ROLE_CORP_CONTRACTOR_OID, task, result);

		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
		TestUtil.assertSuccess(result);

		PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
		display("User after", userAfter);
		assertAssignedRole(userAfter, ROLE_CORP_CONTRACTOR_OID);
	}

	/**
	 *  Employee conflicts with Contractor.
	 */
	@Test
	public void test210AssignRoleEmployeeToJack() throws Exception {
		final String TEST_NAME = "test210AssignRoleEmployeeToJack";
		TestUtil.displayTestTitle(this, TEST_NAME);

		// GIVEN
		Task task = taskManager.createTaskInstance(TestPolicyRules.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();

		LensContext<UserType> context = createUserLensContext();
		fillContextWithUser(context, USER_JACK_OID, result);
		addModificationToContextAssignRole(context, USER_JACK_OID, ROLE_CORP_EMPLOYEE_OID);

		display("Input context", context);

		assertFocusModificationSanity(context);
		rememberCounter(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);

		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		projector.project(context, "test", task, result);

		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
		TestUtil.assertSuccess(result);

		DeltaSetTriple<EvaluatedAssignmentImpl<UserType>> evaluatedAssignmentTriple =
				(DeltaSetTriple)context.getEvaluatedAssignmentTriple();
		//display("Output evaluatedAssignmentTriple", evaluatedAssignmentTriple);

		dumpPolicyRules(context);
		dumpPolicySituations(context);

		// Judge: criminal-exclusion, unassignment, all-assignment-operations, all-assignment-operations-on-jack, all-assignment-operations-on-elaine, all-assignment-operations-on-jack-via-script, global-assignment-notification-for-judge
		// Employee: approve-any-corp-role, notify-exclusion-violations, employee-excludes-contractor
		// Contractor: approve-any-corp-role, notify-exclusion-violations, contractor-excludes-employee
		assertEvaluatedTargetPolicyRules(context, 13);

		EvaluatedExclusionTrigger trigger = (EvaluatedExclusionTrigger) assertTriggeredTargetPolicyRule(context, ROLE_CORP_EMPLOYEE_OID, PolicyConstraintKindType.EXCLUSION, 1, false);
		assertNotNull("No conflicting assignment in trigger", trigger.getConflictingAssignment());
		assertEquals("Wrong conflicting assignment in trigger", ROLE_CORP_CONTRACTOR_OID, trigger.getConflictingAssignment().getTarget().getOid());
	}

	/**
	 *  Engineer->Employee conflicts with Contractor.
	 */
	@Test
	public void test220AssignRoleEngineerToJack() throws Exception {
		final String TEST_NAME = "test220AssignRoleEngineerToJack";
		TestUtil.displayTestTitle(this, TEST_NAME);

		// GIVEN
		Task task = taskManager.createTaskInstance(TestPolicyRules.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();

		LensContext<UserType> context = createUserLensContext();
		fillContextWithUser(context, USER_JACK_OID, result);
		addModificationToContextAssignRole(context, USER_JACK_OID, ROLE_CORP_ENGINEER_OID);

		display("Input context", context);

		assertFocusModificationSanity(context);
		rememberCounter(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);

		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		projector.project(context, "test", task, result);

		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
		TestUtil.assertSuccess(result);

		DeltaSetTriple<EvaluatedAssignmentImpl<UserType>> evaluatedAssignmentTriple =
				(DeltaSetTriple)context.getEvaluatedAssignmentTriple();
		//display("Output evaluatedAssignmentTriple", evaluatedAssignmentTriple);

		dumpPolicyRules(context);
		dumpPolicySituations(context);

		// Judge: L:criminal-exclusion, L:unassignment, L:all-assignment-operations
		// Contractor: L:approve-any-corp-role, L:notify-exclusion-violations, L:contractor-excludes-employee
		// Engineer: approve-any-corp-role, notify-exclusion-violations, employee-excludes-contractor, L:approve-any-corp-role, L:notify-exclusion-violations
		assertEvaluatedTargetPolicyRules(context, 15);
		EvaluatedExclusionTrigger engineerTrigger = (EvaluatedExclusionTrigger) assertTriggeredTargetPolicyRule(context, ROLE_CORP_ENGINEER_OID, PolicyConstraintKindType.EXCLUSION, 1, false);
		assertNotNull("No conflicting assignment in trigger", engineerTrigger.getConflictingAssignment());
		assertEquals("Wrong conflicting assignment in trigger", ROLE_CORP_CONTRACTOR_OID, engineerTrigger.getConflictingAssignment().getTarget().getOid());
		assertTriggeredTargetPolicyRule(context, ROLE_CORP_ENGINEER_OID, PolicyConstraintKindType.ASSIGNMENT_MODIFICATION, 1, false);
		assertTriggeredTargetPolicyRule(context, ROLE_CORP_ENGINEER_OID, PolicyConstraintKindType.SITUATION, 1, false);

		EvaluatedPolicyRule engineerRule = getTriggeredTargetPolicyRule(context, ROLE_CORP_ENGINEER_OID, PolicyConstraintKindType.EXCLUSION);
		display("exclusion rule for Engineer", engineerRule);
		display("exclusion trigger for Engineer", engineerTrigger);
		display("Engineer: assignmentPath", engineerRule.getAssignmentPath());
		display("Engineer: conflictingPath", engineerTrigger.getConflictingPath());
		assertAssignmentPath(engineerRule.getAssignmentPath(), ROLE_CORP_ENGINEER_OID, ROLE_CORP_EMPLOYEE_OID, null);
		assertAssignmentPath(engineerTrigger.getConflictingPath(), ROLE_CORP_CONTRACTOR_OID);

		EvaluatedPolicyRule contractorRule = getTriggeredTargetPolicyRule(context, ROLE_CORP_CONTRACTOR_OID, PolicyConstraintKindType.EXCLUSION);
		EvaluatedExclusionTrigger contractorTrigger = (EvaluatedExclusionTrigger) assertTriggeredTargetPolicyRule(context, ROLE_CORP_CONTRACTOR_OID, PolicyConstraintKindType.EXCLUSION, 1, false);
		display("exclusion rule for Contractor", contractorRule);
		display("exclusion trigger for Contractor", contractorTrigger);
		display("Contractor: assignmentPath", contractorRule.getAssignmentPath());
		display("Contractor: conflictingPath", contractorTrigger.getConflictingPath());
		assertAssignmentPath(contractorRule.getAssignmentPath(), ROLE_CORP_CONTRACTOR_OID, null);
		assertAssignmentPath(contractorTrigger.getConflictingPath(), ROLE_CORP_ENGINEER_OID, ROLE_CORP_EMPLOYEE_OID);
	}

	/**
	 * MID-4132
	 *
	 * Drake changes employeeType null to T. There's a global notification policy rule applicable to users with employeeType != T.
	 * Should we get the notification?
	 *
	 * Yes and no. The condition is checked on objectCurrent. So, in primary state the rule is applied (employeeType is null
	 * at that moment). But in final state (when notification actions are evaluated) the condition should be already false.
	 * So we should not get the notification.
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void test300DrakeChangeEmployeeType() throws Exception {
		final String TEST_NAME = "test300DrakeChangeEmployeeType";
		TestUtil.displayTestTitle(this, TEST_NAME);

		// GIVEN
		Task task = taskManager.createTaskInstance(TestPolicyRules.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();

		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		ObjectDelta<? extends ObjectType> delta = (ObjectDelta<? extends ObjectType>) DeltaBuilder.deltaFor(UserType.class, prismContext)
				.item(UserType.F_ASSIGNMENT)
				.add(ObjectTypeUtil.createAssignmentTo(ROLE_JUDGE_OID, ObjectTypes.ROLE, prismContext))
				.item(UserType.F_EMPLOYEE_TYPE).replace("T")
				.asObjectDelta(USER_DRAKE_OID);

		RecordingProgressListener recordingListener = new RecordingProgressListener();
		modelService.executeChanges(Collections.singletonList(delta), null, task, Collections.singleton(recordingListener), result);

		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
		TestUtil.assertSuccess(result);

		PrismObject<UserType> userAfter = getUser(USER_DRAKE_OID);
		display("User after", userAfter);
		assertAssignedRole(userAfter, ROLE_JUDGE_OID);

		LensFocusContext<?> focusContext = ((LensContext) recordingListener.getModelContext()).getFocusContext();
		display("focusContext", focusContext);
		assertEquals("Wrong # of focus policy rules", 0, focusContext.getPolicyRules().size());
	}

	private ObjectDelta<ShadowType> assertAssignAccountToJack(LensContext<UserType> context) {
        display("Output context", context);

        assertTrue(context.getFocusContext().getPrimaryDelta().getChangeType() == ChangeType.MODIFY);
        assertFalse("No account changes", context.getProjectionContexts().isEmpty());
        Collection<LensProjectionContext> accountContexts = context.getProjectionContexts();
        assertEquals(1, accountContexts.size());
        LensProjectionContext accContext = accountContexts.iterator().next();
        assertNull("Account primary delta sneaked in", accContext.getPrimaryDelta());

        ObjectDelta<ShadowType> accountSecondaryDelta = accContext.getSecondaryDelta();

        assertEquals("Wrong decision", SynchronizationPolicyDecision.KEEP,accContext.getSynchronizationPolicyDecision());

        assertEquals(ChangeType.MODIFY, accountSecondaryDelta.getChangeType());

        return accountSecondaryDelta;

	}


}
