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

import com.evolveum.midpoint.model.api.context.*;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import static com.evolveum.midpoint.test.IntegrationTestTools.display;
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
		TestUtil.displayTestTile(this, TEST_NAME);

		// GIVEN
		Task task = taskManager.createTaskInstance(TestPolicyRules.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();

		LensContext<UserType> context = createUserLensContext();
		fillContextWithUser(context, USER_JACK_OID, result);
		addModificationToContextAssignRole(context, USER_JACK_OID, ROLE_JUDGE_OID);

		display("Input context", context);

		assertFocusModificationSanity(context);
		rememberShadowFetchOperationCount();

		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		projector.project(context, "test", task, result);

		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
		TestUtil.assertSuccess(result);

		dumpPolicyRules(context);

		assertEvaluatedRules(context, 4);
		assertTriggeredRules(context, 2, PolicyConstraintKindType.ASSIGNMENT);
	}

	@Test(enabled = false)
	public void test007JackAttemptAssignRoleJudgeAsOwner() throws Exception {
		final String TEST_NAME = "test007JackAttemptAssignRoleJudgeAsOwner";
		TestUtil.displayTestTile(this, TEST_NAME);

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
		rememberShadowFetchOperationCount();

		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		projector.project(context, "test", task, result);

		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
		TestUtil.assertSuccess(result);

		dumpPolicyRules(context);

		assertEvaluatedRules(context, 4);
		assertTriggeredRules(context, 0, PolicyConstraintKindType.ASSIGNMENT);
	}


	/**
	 * Mostly preparation for other tests. But good check for no exclusion conflict.
	 */
	@Test
    public void test010JackAssignRoleJudge() throws Exception {
		final String TEST_NAME = "test010JackAssignRoleJudge";
        TestUtil.displayTestTile(this, TEST_NAME);

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
		TestUtil.displayTestTile(this, TEST_NAME);

		// GIVEN
		Task task = taskManager.createTaskInstance(TestPolicyRules.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();

		LensContext<UserType> context = createUserLensContext();
		fillContextWithUser(context, USER_JACK_OID, result);
		addModificationToContextUnassignRole(context, USER_JACK_OID, ROLE_JUDGE_OID);

		display("Input context", context);

		assertFocusModificationSanity(context);
		rememberShadowFetchOperationCount();

		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		projector.project(context, "test", task, result);

		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
		TestUtil.assertSuccess(result);

		dumpPolicyRules(context);

		assertEvaluatedRules(context, 4);
		assertTriggeredRules(context, 2, PolicyConstraintKindType.ASSIGNMENT);
	}


	/**
	 * No exclusion here. The assignment should go smoothly.
	 */
	@Test
    public void test100AssignRoleMutinierToJack() throws Exception {
		final String TEST_NAME = "test100AssignRoleMutinierToJack";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestPolicyRules.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        LensContext<UserType> context = createUserLensContext();
        fillContextWithUser(context, USER_JACK_OID, result);
        addModificationToContextAssignRole(context, USER_JACK_OID, ROLE_MUTINIER_OID);

        display("Input context", context);

        assertFocusModificationSanity(context);
        rememberShadowFetchOperationCount();
        
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
        
        assertEvaluatedRules(context, 4);
        assertTriggeredRules(context, 0, null);
	}
	
	@Test
    public void test110AssignRolePirateToJack() throws Exception {
		final String TEST_NAME = "test110AssignRolePirateToJack";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestPolicyRules.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        LensContext<UserType> context = createUserLensContext();
        fillContextWithUser(context, USER_JACK_OID, result);
        addModificationToContextAssignRole(context, USER_JACK_OID, ROLE_PIRATE_OID);

        display("Input context", context);

        assertFocusModificationSanity(context);
        rememberShadowFetchOperationCount();
        
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
        
        assertEvaluatedRules(context, 4);
        EvaluatedExclusionTrigger trigger = (EvaluatedExclusionTrigger) assertTriggeredRule(context, null, PolicyConstraintKindType.EXCLUSION, 1, true);
        assertNotNull("No conflicting assignment in trigger", trigger.getConflictingAssignment());
        assertEquals("Wrong conflicting assignment in trigger", ROLE_PIRATE_OID, trigger.getConflictingAssignment().getTarget().getOid());
	}
	
	/**
	 * Assignment with an exception from the exclusion rule.
	 */
	@Test
    public void test112AssignRolePirateWithExceptionToJack() throws Exception {
		final String TEST_NAME = "test112AssignRolePirateWithExceptionToJack";
        TestUtil.displayTestTile(this, TEST_NAME);

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
        rememberShadowFetchOperationCount();
        
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
        
        List<EvaluatedPolicyRule> evaluatedRules = assertEvaluatedRules(context, 4);
        assertTriggeredRules(context, 0, null);

        EvaluatedPolicyRule evaluatedPolicyRule = evaluatedRules.get(0);
        Collection<PolicyExceptionType> exceptions = evaluatedPolicyRule.getPolicyExceptions();
        assertEquals("Wrong number of exceptions", 1, exceptions.size());
        PolicyExceptionType policyException = exceptions.iterator().next();
        assertEquals("Wrong rule name in poliy excpetion", ROLE_JUDGE_POLICY_RULE_EXCLUSION_NAME, policyException.getRuleName());        
	}
	
	@Test
    public void test120AssignRoleConstableToJack() throws Exception {
		final String TEST_NAME = "test120AssignRoleConstableToJack";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestPolicyRules.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        LensContext<UserType> context = createUserLensContext();
        fillContextWithUser(context, USER_JACK_OID, result);
        addModificationToContextAssignRole(context, USER_JACK_OID, ROLE_CONSTABLE_OID);

        display("Input context", context);

        assertFocusModificationSanity(context);
        rememberShadowFetchOperationCount();
        
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
        
        assertEvaluatedRules(context, 5);
        // conflicting assignment was pruned, so the exclusion is no longer present here
//		EvaluatedExclusionTrigger trigger = (EvaluatedExclusionTrigger) assertTriggeredRule(context, null, PolicyConstraintKindType.EXCLUSION, 1, true);
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
		TestUtil.displayTestTile(this, TEST_NAME);

		// GIVEN
		Task task = taskManager.createTaskInstance(TestPolicyRules.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();

		LensContext<UserType> context = createUserLensContext();
		fillContextWithUser(context, USER_JACK_OID, result);
		addModificationToContextAssignRole(context, USER_JACK_OID, ROLE_THIEF_OID);

		display("Input context", context);

		assertFocusModificationSanity(context);
		rememberShadowFetchOperationCount();

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

		assertEvaluatedRules(context, 6);
		EvaluatedExclusionTrigger triggerExclusion = (EvaluatedExclusionTrigger) assertTriggeredRule(context, null, PolicyConstraintKindType.EXCLUSION, 1, false);
		assertNotNull("No conflicting assignment in trigger", triggerExclusion.getConflictingAssignment());
		assertEquals("Wrong conflicting assignment in trigger", ROLE_JUDGE_OID, triggerExclusion.getConflictingAssignment().getTarget().getOid());

		EvaluatedSituationTrigger triggerSituation = (EvaluatedSituationTrigger) assertTriggeredRule(context, null, PolicyConstraintKindType.SITUATION, 1, false);
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
		TestUtil.displayTestTile(this, TEST_NAME);

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
		TestUtil.displayTestTile(this, TEST_NAME);

		// GIVEN
		Task task = taskManager.createTaskInstance(TestPolicyRules.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();

		LensContext<UserType> context = createUserLensContext();
		fillContextWithUser(context, USER_JACK_OID, result);
		addModificationToContextAssignRole(context, USER_JACK_OID, ROLE_CORP_EMPLOYEE_OID);

		display("Input context", context);

		assertFocusModificationSanity(context);
		rememberShadowFetchOperationCount();

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

		// Judge: criminal-exclusion, unassignment, all-assignment-operations
		// Employee: approve-any-corp-role, notify-exclusion-violations, employee-excludes-contractor
		// Contractor: approve-any-corp-role, notify-exclusion-violations, contractor-excludes-employee
		assertEvaluatedRules(context, 10);
		EvaluatedExclusionTrigger trigger = (EvaluatedExclusionTrigger) assertTriggeredRule(context, ROLE_CORP_EMPLOYEE_OID, PolicyConstraintKindType.EXCLUSION, 1, false);
		assertNotNull("No conflicting assignment in trigger", trigger.getConflictingAssignment());
		assertEquals("Wrong conflicting assignment in trigger", ROLE_CORP_CONTRACTOR_OID, trigger.getConflictingAssignment().getTarget().getOid());
	}

	/**
	 *  Engineer->Employee conflicts with Contractor.
	 */
	@Test
	public void test220AssignRoleEngineerToJack() throws Exception {
		final String TEST_NAME = "test220AssignRoleEngineerToJack";
		TestUtil.displayTestTile(this, TEST_NAME);

		// GIVEN
		Task task = taskManager.createTaskInstance(TestPolicyRules.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();

		LensContext<UserType> context = createUserLensContext();
		fillContextWithUser(context, USER_JACK_OID, result);
		addModificationToContextAssignRole(context, USER_JACK_OID, ROLE_CORP_ENGINEER_OID);

		display("Input context", context);

		assertFocusModificationSanity(context);
		rememberShadowFetchOperationCount();

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

		// Judge: L:criminal-exclusion, L:unassignment, L:all-assignment-operations
		// Contractor: L:approve-any-corp-role, L:notify-exclusion-violations, L:contractor-excludes-employee
		// Engineer: approve-any-corp-role, notify-exclusion-violations, employee-excludes-contractor, L:approve-any-corp-role, L:notify-exclusion-violations
		assertEvaluatedRules(context, 12);
		EvaluatedExclusionTrigger engineerTrigger = (EvaluatedExclusionTrigger) assertTriggeredRule(context, ROLE_CORP_ENGINEER_OID, PolicyConstraintKindType.EXCLUSION, 1, false);
		assertNotNull("No conflicting assignment in trigger", engineerTrigger.getConflictingAssignment());
		assertEquals("Wrong conflicting assignment in trigger", ROLE_CORP_CONTRACTOR_OID, engineerTrigger.getConflictingAssignment().getTarget().getOid());
		assertTriggeredRule(context, ROLE_CORP_ENGINEER_OID, PolicyConstraintKindType.ASSIGNMENT, 1, false);
		assertTriggeredRule(context, ROLE_CORP_ENGINEER_OID, PolicyConstraintKindType.SITUATION, 1, false);

		EvaluatedPolicyRule engineerRule = getTriggeredRule(context, ROLE_CORP_ENGINEER_OID, PolicyConstraintKindType.EXCLUSION);
		display("exclusion rule for Engineer", engineerRule);
		display("exclusion trigger for Engineer", engineerTrigger);
		display("Engineer: assignmentPath", engineerRule.getAssignmentPath());
		display("Engineer: conflictingPath", engineerTrigger.getConflictingPath());
		assertAssignmentPath(engineerRule.getAssignmentPath(), ROLE_CORP_ENGINEER_OID, ROLE_CORP_EMPLOYEE_OID, null);
		assertAssignmentPath(engineerTrigger.getConflictingPath(), ROLE_CORP_CONTRACTOR_OID);

		EvaluatedPolicyRule contractorRule = getTriggeredRule(context, ROLE_CORP_CONTRACTOR_OID, PolicyConstraintKindType.EXCLUSION);
		EvaluatedExclusionTrigger contractorTrigger = (EvaluatedExclusionTrigger) assertTriggeredRule(context, ROLE_CORP_CONTRACTOR_OID, PolicyConstraintKindType.EXCLUSION, 1, false);
		display("exclusion rule for Contractor", contractorRule);
		display("exclusion trigger for Contractor", contractorTrigger);
		display("Contractor: assignmentPath", contractorRule.getAssignmentPath());
		display("Contractor: conflictingPath", contractorTrigger.getConflictingPath());
		assertAssignmentPath(contractorRule.getAssignmentPath(), ROLE_CORP_CONTRACTOR_OID, null);
		assertAssignmentPath(contractorTrigger.getConflictingPath(), ROLE_CORP_ENGINEER_OID, ROLE_CORP_EMPLOYEE_OID);
	}

	private void assertAssignmentPath(AssignmentPath path, String... targetOids) {
		assertEquals("Wrong path size", targetOids.length, path.size());
		for (int i = 0; i < targetOids.length; i++) {
			ObjectType target = path.getSegments().get(i).getTarget();
			if (targetOids[i] == null) {
				assertNull("Target #" + (i+1) + " should be null; it is: " + target, target);
			} else {
				assertNotNull("Target #" + (i+1) + " should not be null", target);
				assertEquals("Wrong OID in target #" + (i+1), targetOids[i], target.getOid());
			}
		}
	}

	private List<EvaluatedPolicyRule> assertEvaluatedRules(LensContext<UserType> context, int expected) {
		List<EvaluatedPolicyRule> rules = new ArrayList<>();
		forEvaluatedRule(context, null, rules::add);
		assertEquals("Unexpected number of evaluated policy rules in the context", expected, rules.size());
		return rules;
	}
	
	private void assertTriggeredRules(LensContext<UserType> context, int expected, PolicyConstraintKindType expectedConstraintKind) {
		List<EvaluatedPolicyRuleTrigger> triggers = new ArrayList<>();
		forTriggeredRule(context, null, trigger -> {
			display("Triggered rule", trigger);
			triggers.add(trigger);
			if (expectedConstraintKind != null) {
				assertEquals("Wrong trigger constraint type in "+trigger, expectedConstraintKind, trigger.getConstraintKind());
			}
		});
		assertEquals("Unexpected number of triggered policy rules in the context", expected, triggers.size());
	}

	// exclusive=true : there can be no other triggers than 'expectedCount' of 'expectedConstraintKind'
	private EvaluatedPolicyRuleTrigger assertTriggeredRule(LensContext<UserType> context, String targetOid, PolicyConstraintKindType expectedConstraintKind, int expectedCount, boolean exclusive) {
		List<EvaluatedPolicyRuleTrigger> triggers = new ArrayList<>();
		forTriggeredRule(context, targetOid, trigger -> {
			if (!exclusive && trigger.getConstraintKind() != expectedConstraintKind) {
				return;
			}
			display("Triggered rule", trigger);
			triggers.add(trigger);
			if (expectedConstraintKind != null) {
				assertEquals("Wrong trigger constraint type in "+trigger, expectedConstraintKind, trigger.getConstraintKind());
			}
		});
		assertEquals("Unexpected number of triggered policy rules in the context", expectedCount, triggers.size());
		return triggers.get(0);
	}

	private EvaluatedPolicyRule getTriggeredRule(LensContext<UserType> context, String targetOid, PolicyConstraintKindType expectedConstraintKind) {
		List<EvaluatedPolicyRule> rules = new ArrayList<>();
		forEvaluatedRule(context, targetOid, rule -> {
			if (rule.getTriggers().stream().anyMatch(t -> t.getConstraintKind() == expectedConstraintKind)) {
				rules.add(rule);
			}
		});
		if (rules.size() != 1) {
			fail("Wrong # of triggered rules for " + targetOid + ": expected 1, got " + rules.size() + ": " + rules);
		}
		return rules.get(0);
	}
	
	private void forTriggeredRule(LensContext<UserType> context, String targetOid, Consumer<EvaluatedPolicyRuleTrigger> handler) {
		forEvaluatedRule(context, targetOid, rule -> {
			Collection<EvaluatedPolicyRuleTrigger<?>> triggers = rule.getTriggers();
    		for (EvaluatedPolicyRuleTrigger<?> trigger: triggers) {
    			handler.accept(trigger);
    		}
		});
	}
	
	private void forEvaluatedRule(LensContext<UserType> context, String targetOid, Consumer<EvaluatedPolicyRule> handler) {
		DeltaSetTriple<EvaluatedAssignmentImpl<UserType>> evaluatedAssignmentTriple = 
        		(DeltaSetTriple)context.getEvaluatedAssignmentTriple();
        evaluatedAssignmentTriple.simpleAccept(assignment -> {
        	if (targetOid == null || assignment.getTarget() != null && targetOid.equals(assignment.getTarget().getOid())) {
				assignment.getAllTargetsPolicyRules().forEach(handler::accept);
			}
        });
	}

	private void dumpPolicyRules(LensContext<UserType> context) {
		display("Policy rules", context.dumpPolicyRules(3));
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
