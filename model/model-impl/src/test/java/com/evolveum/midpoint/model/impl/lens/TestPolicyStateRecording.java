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

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.builder.DeltaBuilder;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import java.io.File;
import java.util.Collections;
import java.util.List;

import static com.evolveum.midpoint.model.api.ModelExecuteOptions.createReconcile;
import static com.evolveum.midpoint.prism.delta.ObjectDelta.createEmptyModifyDelta;
import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.createAssignmentTo;
import static org.testng.AssertJUnit.assertEquals;

/**
 * Tests recording of policy situations into objects and assignments.
 *
 * @author mederly
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestPolicyStateRecording extends AbstractLensTest {

	private static final String ROLE_JUDGE_POLICY_RULE_EXCLUSION_NAME = "criminal exclusion";

	protected static final File TEST_DIR = new File(AbstractLensTest.TEST_DIR, "policy/state");

	private static final File USER_BOB_FILE = new File(TEST_DIR, "user-bob.xml");
	private static final String WRONG_URI = "http://test.org/wrong";
	private static String userBobOid;
	private static final File USER_EVE_FILE = new File(TEST_DIR, "user-eve.xml");
	private static String userEveOid;
	private static final File ROLE_A_TEST_2A_FILE = new File(TEST_DIR, "a-test-2a.xml");
	private static String roleATest2aOid;
	private static final File ROLE_A_TEST_2B_FILE = new File(TEST_DIR, "a-test-2b.xml");
	private static String roleATest2bOid;
	private static final File ROLE_A_TEST_2C_FILE = new File(TEST_DIR, "a-test-2c.xml");
	private static String roleATest2cOid;
	private static final File ROLE_A_TEST_3A_FILE = new File(TEST_DIR, "a-test-3a.xml");
	private static String roleATest3aOid;
	private static final File ROLE_A_TEST_3B_FILE = new File(TEST_DIR, "a-test-3b.xml");
	private static String roleATest3bOid;
	private static final File ROLE_A_TEST_3X_FILE = new File(TEST_DIR, "a-test-3x.xml");
	private static String roleATest3xOid;
	private static final File ROLE_A_TEST_3Y_FILE = new File(TEST_DIR, "a-test-3y.xml");
	private static String roleATest3yOid;
	private static final File ROLE_A_TEST_WRONG_FILE = new File(TEST_DIR, "a-test-wrong.xml");
	private static String roleATestWrongOid;
	private static final File METAROLE_COMMON_RULES_FILE = new File(TEST_DIR, "metarole-common-rules.xml");
	private static String metaroleCommonRulesOid;

	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);
		setDefaultUserTemplate(USER_TEMPLATE_OID);

		addObject(ROLE_PIRATE_RECORD_ONLY_FILE);
		addObject(ROLE_JUDGE_RECORD_ONLY_FILE);

		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

		metaroleCommonRulesOid = addAndRecompute(METAROLE_COMMON_RULES_FILE, initTask, initResult);
		roleATest2aOid = addAndRecompute(ROLE_A_TEST_2A_FILE, initTask, initResult);
		roleATest2bOid = addAndRecompute(ROLE_A_TEST_2B_FILE, initTask, initResult);
		roleATest2cOid = addAndRecompute(ROLE_A_TEST_2C_FILE, initTask, initResult);
		roleATest3xOid = addAndRecompute(ROLE_A_TEST_3X_FILE, initTask, initResult);
		roleATest3yOid = addAndRecompute(ROLE_A_TEST_3Y_FILE, initTask, initResult);
		roleATest3aOid = addAndRecompute(ROLE_A_TEST_3A_FILE, initTask, initResult);
		roleATest3bOid = addAndRecompute(ROLE_A_TEST_3B_FILE, initTask, initResult);
		roleATestWrongOid = addAndRecompute(ROLE_A_TEST_WRONG_FILE, initTask, initResult);

		userBobOid = addAndRecompute(USER_BOB_FILE, initTask, initResult);
		userEveOid = addAndRecompute(USER_EVE_FILE, initTask, initResult);

		InternalMonitor.reset();

		DebugUtil.setPrettyPrintBeansAs(PrismContext.LANG_YAML);
	}

	@Test
	public void test100JackAssignRoleJudge() throws Exception {
		TestCtx t = createContext(this, "test100JackAssignRoleJudge");

		// GIVEN

		// WHEN
		t.displayWhen();
		assignRole(USER_JACK_OID, ROLE_JUDGE_OID, t.task, t.result);

		// THEN
		t.displayThen();
		UserType jack = getUser(USER_JACK_OID).asObjectable();
		display("jack", jack);
		t.result.computeStatus();
		TestUtil.assertSuccess(t.result);

		assertAssignedRole(jack.asPrismObject(), ROLE_JUDGE_OID);
		assertEquals("Wrong # of assignments", 1, jack.getAssignment().size());
		assertEquals("Wrong policy situations",
				Collections.emptyList(),
				jack.getAssignment().get(0).getPolicySituation());

		display("Audit", dummyAuditService);
		dummyAuditService.assertExecutionRecords(1);
	}

	@Test
	public void test110JackAssignRolePirate() throws Exception {
		TestCtx t = createContext(this, "test110JackAssignRolePirate");

		// WHEN
		t.displayWhen();
		assignRole(USER_JACK_OID, ROLE_PIRATE_OID, t.task, t.result);

		// THEN
		t.displayThen();
		UserType jack = getUser(USER_JACK_OID).asObjectable();
		display("jack", jack);
		t.result.computeStatus();
		TestUtil.assertSuccess(t.result);

		assertAssignedRole(jack.asPrismObject(), ROLE_PIRATE_OID);
		assertEquals("Wrong # of assignments", 2, jack.getAssignment().size());
		for (AssignmentType assignment : jack.getAssignment()) {
			assertExclusionViolationState(assignment);
		}

		display("Audit", dummyAuditService);
		dummyAuditService.assertExecutionRecords(2);            // rules without IDs, with IDs
	}

	// should keep the situation for both assignments
	@Test
	public void test120RecomputeJack() throws Exception {
		TestCtx t = createContext(this, "test120RecomputeJack");

		// GIVEN

		// WHEN
		t.displayWhen();
		executeChanges(createEmptyModifyDelta(UserType.class, USER_JACK_OID, prismContext), createReconcile(), t.task, t.result);
		//recomputeUser(USER_JACK_OID, t.task, t.result);

		// THEN
		t.displayThen();
		UserType jack = getUser(USER_JACK_OID).asObjectable();
		display("jack", jack);
		t.result.computeStatus();
		TestUtil.assertSuccess(t.result);

		// TODO test that assignment IDs are filled in correctly (currently they are not)
		assertEquals("Wrong # of assignments", 2, jack.getAssignment().size());
		for (AssignmentType assignment : jack.getAssignment()) {
			assertEquals("Wrong policy situations",
					Collections.singletonList(SchemaConstants.MODEL_POLICY_SITUATION_EXCLUSION_VIOLATION),
					assignment.getPolicySituation());
		}

		display("Audit", dummyAuditService);
		dummyAuditService.assertExecutionRecords(1);
		dummyAuditService.assertExecutionDeltas(0);
	}

	@Test
	public void test130JackUnassignRolePirate() throws Exception {
		TestCtx t = createContext(this, "test130JackUnassignRolePirate");

		// GIVEN
		UserType jack = getUser(USER_JACK_OID).asObjectable();
		AssignmentType pirateAssignment = findAssignmentByTarget(jack.asPrismObject(), ROLE_PIRATE_OID).get();

		// WHEN
		t.displayWhen();
		ObjectDelta<UserType> delta = DeltaBuilder.deltaFor(UserType.class, prismContext)
				.item(UserType.F_ASSIGNMENT)
				.delete(pirateAssignment.clone())
				.asObjectDeltaCast(USER_JACK_OID);
		executeChangesAssertSuccess(delta, null, t.task, t.result);

		// THEN
		t.displayThen();
		jack = getUser(USER_JACK_OID).asObjectable();
		display("jack", jack);
		t.result.computeStatus();
		TestUtil.assertSuccess(t.result);

		assertNotAssignedRole(jack.asPrismObject(), ROLE_PIRATE_OID);
		assertEquals("Wrong # of assignments", 1, jack.getAssignment().size());
		assertEquals("Wrong policy situations",
				Collections.emptyList(),
				jack.getAssignment().get(0).getPolicySituation());

		display("Audit", dummyAuditService);
		dummyAuditService.assertExecutionRecords(1);            // executed in one shot
	}

	@Test
	public void test200BobAssign2a3a() throws Exception {
		TestCtx t = createContext(this, "test200BobAssign2a3a");

		// GIVEN

		// WHEN
		t.displayWhen();
		ObjectDelta<UserType> delta = DeltaBuilder.deltaFor(UserType.class, prismContext)
				.item(UserType.F_ASSIGNMENT)
						.add(createAssignmentTo(roleATest2aOid, ObjectTypes.ROLE, prismContext),
								createAssignmentTo(roleATest3aOid, ObjectTypes.ROLE, prismContext))
				.asObjectDeltaCast(userBobOid);
		executeChangesAssertSuccess(delta, null, t.task, t.result);

		// THEN
		t.displayThen();
		UserType bob = getUser(userBobOid).asObjectable();
		display("bob", bob);
		t.result.computeStatus();
		TestUtil.assertSuccess(t.result);

		assertAssignedRole(bob.asPrismObject(), roleATest2aOid);
		assertAssignedRole(bob.asPrismObject(), roleATest3aOid);
		assertEquals("Wrong # of assignments", 2, bob.getAssignment().size());
		assertEquals("Wrong policy situations for assignment 1",
				Collections.emptyList(),
				bob.getAssignment().get(0).getPolicySituation());
		assertEquals("Wrong policy situations for assignment 2",
				Collections.emptyList(),
				bob.getAssignment().get(1).getPolicySituation());

		display("Audit", dummyAuditService);
		dummyAuditService.assertExecutionRecords(1);            // no policy state update
	}

	@Test
	public void test210BobAssign2b3b() throws Exception {
		TestCtx t = createContext(this, "test210BobAssign2b3b");

		// GIVEN

		// WHEN
		t.displayWhen();
		ObjectDelta<UserType> delta = DeltaBuilder.deltaFor(UserType.class, prismContext)
				.item(UserType.F_ASSIGNMENT)
				.add(createAssignmentTo(roleATest2bOid, ObjectTypes.ROLE, prismContext),
						createAssignmentTo(roleATest3bOid, ObjectTypes.ROLE, prismContext))
				.asObjectDeltaCast(userBobOid);
		executeChangesAssertSuccess(delta, null, t.task, t.result);

		// THEN
		t.displayThen();
		UserType bob = getUser(userBobOid).asObjectable();
		display("bob", bob);
		t.result.computeStatus();
		TestUtil.assertSuccess(t.result);

		assertAssignedRole(bob.asPrismObject(), roleATest2aOid);
		assertAssignedRole(bob.asPrismObject(), roleATest2bOid);
		assertAssignedRole(bob.asPrismObject(), roleATest3aOid);
		assertAssignedRole(bob.asPrismObject(), roleATest3bOid);
		assertEquals("Wrong # of assignments", 4, bob.getAssignment().size());

		display("Audit", dummyAuditService);
		dummyAuditService.assertExecutionRecords(2);            // rules without IDs, with IDs

		for (AssignmentType assignment : bob.getAssignment()) {
			assertExclusionViolationState(assignment);
		}
	}

	private void assertExclusionViolationState(AssignmentType assignment) {
		assertEquals("Wrong policy situations",
				Collections.singletonList(SchemaConstants.MODEL_POLICY_SITUATION_EXCLUSION_VIOLATION),
				assignment.getPolicySituation());
		assertEquals("Wrong # of triggered policy rules in assignment " + assignment, 1, assignment.getTriggeredPolicyRule().size());
		List<EvaluatedPolicyRuleTriggerType> triggers = assignment.getTriggeredPolicyRule().get(0).getTrigger();
		assertEquals("Wrong # of triggers in triggered policy rule in assignment " + assignment, 1, triggers.size());
		assertEquals("Wrong type of trigger in " + assignment, PolicyConstraintKindType.EXCLUSION, triggers.get(0).getConstraintKind());
	}

	// new user, new assignments (no IDs)
	@Test
	public void test220AliceAssign2a2b() throws Exception {
		TestCtx t = createContext(this, "test220AliceAssign2a2b");

		// GIVEN
		UserType alice = prismContext.createObjectable(UserType.class)
				.name("alice")
				.assignment(createAssignmentTo(roleATest2aOid, ObjectTypes.ROLE, prismContext))
				.assignment(createAssignmentTo(roleATest2bOid, ObjectTypes.ROLE, prismContext));

		// WHEN
		t.displayWhen();
		addObject(alice.asPrismObject(), t.task, t.result);

		// THEN
		t.displayThen();
		alice = getUser(alice.getOid()).asObjectable();
		display("alice", alice);
		t.result.computeStatus();
		TestUtil.assertSuccess(t.result);

		assertAssignedRole(alice.asPrismObject(), roleATest2aOid);
		assertAssignedRole(alice.asPrismObject(), roleATest2bOid);
		assertEquals("Wrong # of assignments", 2, alice.getAssignment().size());

		display("Audit", dummyAuditService);
		dummyAuditService.assertExecutionRecords(2);            // rules without IDs, with IDs ?

		for (AssignmentType assignment : alice.getAssignment()) {
			assertExclusionViolationState(assignment);
		}
	}

	// new user, new assignments (explicit IDs)
	@Test
	public void test230ChuckAssign2a2b() throws Exception {
		TestCtx t = createContext(this, "test230ChuckAssign2a2b");

		// GIVEN
		AssignmentType assignment2a = createAssignmentTo(roleATest2aOid, ObjectTypes.ROLE, prismContext);
		assignment2a.setId(100L);
		AssignmentType assignment2b = createAssignmentTo(roleATest2bOid, ObjectTypes.ROLE, prismContext);
		assignment2b.setId(101L);
		UserType chuck = prismContext.createObjectable(UserType.class)
				.name("chuck")
				.assignment(assignment2a)
				.assignment(assignment2b);

		// WHEN
		t.displayWhen();
		addObject(chuck.asPrismObject(), t.task, t.result);

		// THEN
		t.displayThen();
		chuck = getUser(chuck.getOid()).asObjectable();
		display("chuck", chuck);
		t.result.computeStatus();
		TestUtil.assertSuccess(t.result);

		assertAssignedRole(chuck.asPrismObject(), roleATest2aOid);
		assertAssignedRole(chuck.asPrismObject(), roleATest2bOid);
		assertEquals("Wrong # of assignments", 2, chuck.getAssignment().size());

		display("Audit", dummyAuditService);
		dummyAuditService.assertExecutionRecords(2);            // sourceRef.oid is null on the first iteration (object OID is unknown before actual creation in repo)

		for (AssignmentType assignment : chuck.getAssignment()) {
			assertExclusionViolationState(assignment);
		}
	}

	// new user, new assignments (explicit IDs, explicit OID)
	@Test
	public void test240DanAssign2a2b() throws Exception {
		TestCtx t = createContext(this, "test240DanAssign2a2b");

		// GIVEN
		AssignmentType assignment2a = createAssignmentTo(roleATest2aOid, ObjectTypes.ROLE, prismContext);
		assignment2a.setId(100L);
		AssignmentType assignment2b = createAssignmentTo(roleATest2bOid, ObjectTypes.ROLE, prismContext);
		assignment2b.setId(101L);
		UserType dan = prismContext.createObjectable(UserType.class)
				.oid("207752fa-9559-496c-b04d-42b5e9af2779")
				.name("dan")
				.assignment(assignment2a)
				.assignment(assignment2b);

		// WHEN
		t.displayWhen();
		addObject(dan.asPrismObject(), t.task, t.result);

		// THEN
		t.displayThen();
		dan = getUser(dan.getOid()).asObjectable();
		display("dan", dan);
		t.result.computeStatus();
		TestUtil.assertSuccess(t.result);

		assertAssignedRole(dan.asPrismObject(), roleATest2aOid);
		assertAssignedRole(dan.asPrismObject(), roleATest2bOid);
		assertEquals("Wrong # of assignments", 2, dan.getAssignment().size());

		display("Audit", dummyAuditService);
		dummyAuditService.assertExecutionRecords(1);

		for (AssignmentType assignment : dan.getAssignment()) {
			assertExclusionViolationState(assignment);
		}
	}

	// modified user, new assignment (with ID)
	@Test
	public void test250EveAssign2b() throws Exception {
		TestCtx t = createContext(this, "test220AliceAssign2a2b");

		// WHEN
		t.displayWhen();
		AssignmentType assignment2b = createAssignmentTo(roleATest2bOid, ObjectTypes.ROLE, prismContext);
		assignment2b.setId(200L);
		ObjectDelta<UserType> delta = DeltaBuilder.deltaFor(UserType.class, prismContext)
				.item(UserType.F_ASSIGNMENT)
				.add(assignment2b)
				.asObjectDeltaCast(userEveOid);
		executeChangesAssertSuccess(delta, null, t.task, t.result);

		// THEN
		t.displayThen();
		UserType eve = getUser(userEveOid).asObjectable();
		display("alice", eve);
		t.result.computeStatus();
		TestUtil.assertSuccess(t.result);

		assertAssignedRole(eve.asPrismObject(), roleATest2aOid);
		assertAssignedRole(eve.asPrismObject(), roleATest2bOid);
		assertEquals("Wrong # of assignments", 2, eve.getAssignment().size());

		display("Audit", dummyAuditService);
		dummyAuditService.assertExecutionRecords(1);

		for (AssignmentType assignment : eve.getAssignment()) {
			assertExclusionViolationState(assignment);
		}
	}

	@Test
	public void test300MakeRoleWrong() throws Exception {
		TestCtx t = createContext(this, "test300MakeRoleWrong");

		// GIVEN

		// WHEN
		t.displayWhen();
		ObjectDelta<RoleType> delta = DeltaBuilder.deltaFor(RoleType.class, prismContext)
				.item(RoleType.F_DESCRIPTION).replace("wrong")
				.asObjectDeltaCast(roleATestWrongOid);
		executeChangesAssertSuccess(delta, null, t.task, t.result);

		// THEN
		t.displayThen();
		RoleType wrong = getRole(roleATestWrongOid).asObjectable();
		display("role 'wrong'", wrong);
		t.result.computeStatus();
		TestUtil.assertSuccess(t.result);

		assertEquals("Wrong policy situations for role", Collections.singletonList(WRONG_URI), wrong.getPolicySituation());

		display("Audit", dummyAuditService);
		dummyAuditService.assertExecutionRecords(1);            // no extra policy state update
	}

	@Test
	public void test310CreateWrongRole() throws Exception {
		TestCtx t = createContext(this, "test310CreateWrongRole");

		// GIVEN
		RoleType wrong2 = prismContext.createObjectable(RoleType.class)
				.name("wrong-2")
				.description("wrong")
				.assignment(createAssignmentTo(metaroleCommonRulesOid, ObjectTypes.ROLE, prismContext));

		// WHEN
		t.displayWhen();
		addObject(wrong2.asPrismObject(), t.task, t.result);

		// THEN
		t.displayThen();
		wrong2 = getRole(wrong2.getOid()).asObjectable();
		display("role 'wrong-2'", wrong2);
		t.result.computeStatus();
		TestUtil.assertSuccess(t.result);

		assertEquals("Wrong policy situations for role", Collections.singletonList(WRONG_URI), wrong2.getPolicySituation());

		display("Audit", dummyAuditService);
		dummyAuditService.assertExecutionRecords(2);            // extra policy state update because of OID
	}

	@Test
	public void test320CreateWrongRoleKnownOid() throws Exception {
		TestCtx t = createContext(this, "test320CreateWrongRoleKnownOid");

		// GIVEN
		AssignmentType assignmentCommon = createAssignmentTo(metaroleCommonRulesOid, ObjectTypes.ROLE, prismContext);
		assignmentCommon.setId(300L);
		RoleType wrong3 = prismContext.createObjectable(RoleType.class)
				.name("wrong-3")
				.oid("df6c6bdc-f938-4afc-98f3-10d18ceda274")
				.description("wrong")
				.assignment(assignmentCommon);

		// WHEN
		t.displayWhen();
		addObject(wrong3.asPrismObject(), t.task, t.result);

		// THEN
		t.displayThen();
		wrong3 = getRole(wrong3.getOid()).asObjectable();
		display("role 'wrong-3'", wrong3);
		t.result.computeStatus();
		TestUtil.assertSuccess(t.result);

		assertEquals("Wrong policy situations for role", Collections.singletonList(WRONG_URI), wrong3.getPolicySituation());

		display("Audit", dummyAuditService);
		dummyAuditService.assertExecutionRecords(1);            // no extra policy state update
	}

}
