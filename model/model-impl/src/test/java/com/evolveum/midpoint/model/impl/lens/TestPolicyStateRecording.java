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
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentPolicyEnforcementType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import java.io.File;
import java.util.Collections;

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
	private static String userBobOid;
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

		userBobOid = addAndRecompute(USER_BOB_FILE, initTask, initResult);

		InternalMonitor.reset();

		DebugUtil.setPrettyPrintBeansAs(PrismContext.LANG_YAML);
	}

	@Test
	public void test100JackAssignRoleJudge() throws Exception {
		final String TEST_NAME = "test100JackAssignRoleJudge";
		TestUtil.displayTestTitle(this, TEST_NAME);

		// GIVEN
		Task task = taskManager.createTaskInstance(TestPolicyStateRecording.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();

		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		assignRole(USER_JACK_OID, ROLE_JUDGE_OID, task, result);

		// THEN
		TestUtil.displayThen(TEST_NAME);
		UserType jack = getUser(USER_JACK_OID).asObjectable();
		display("jack", jack);
		result.computeStatus();
		TestUtil.assertSuccess(result);

		assertAssignedRole(USER_JACK_OID, ROLE_JUDGE_OID, task, result);
		assertEquals("Wrong # of assignments", 1, jack.getAssignment().size());
		assertEquals("Wrong policy situations",
				Collections.emptyList(),
				jack.getAssignment().get(0).getPolicySituation());
	}

	@Test
	public void test110JackAssignRolePirate() throws Exception {
		final String TEST_NAME = "test110JackAssignRolePirate";
		TestUtil.displayTestTitle(this, TEST_NAME);

		// GIVEN
		Task task = taskManager.createTaskInstance(TestPolicyStateRecording.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();

		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		assignRole(USER_JACK_OID, ROLE_PIRATE_OID, task, result);

		// THEN
		TestUtil.displayThen(TEST_NAME);
		UserType jack = getUser(USER_JACK_OID).asObjectable();
		display("jack", jack);
		result.computeStatus();
		TestUtil.assertSuccess(result);

		assertAssignedRole(USER_JACK_OID, ROLE_PIRATE_OID, task, result);
		assertEquals("Wrong # of assignments", 2, jack.getAssignment().size());
		for (AssignmentType assignment : jack.getAssignment()) {
			assertEquals("Wrong policy situations",
					Collections.singletonList(SchemaConstants.MODEL_POLICY_SITUATION_EXCLUSION_VIOLATION),
					assignment.getPolicySituation());
		}
	}

	// should keep the situation for both assignments
	@Test
	public void test120RecomputeJack() throws Exception {
		final String TEST_NAME = "test120RecomputeJack";
		TestUtil.displayTestTitle(this, TEST_NAME);

		// GIVEN
		Task task = taskManager.createTaskInstance(TestPolicyStateRecording.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();

		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		recomputeUser(USER_JACK_OID, task, result);

		// THEN
		TestUtil.displayThen(TEST_NAME);
		UserType jack = getUser(USER_JACK_OID).asObjectable();
		display("jack", jack);
		result.computeStatus();
		TestUtil.assertSuccess(result);

		// TODO test that assignment IDs are filled in correctly (currently they are not)
		assertEquals("Wrong # of assignments", 2, jack.getAssignment().size());
		for (AssignmentType assignment : jack.getAssignment()) {
			assertEquals("Wrong policy situations",
					Collections.singletonList(SchemaConstants.MODEL_POLICY_SITUATION_EXCLUSION_VIOLATION),
					assignment.getPolicySituation());
		}
	}

	@Test
	public void test200BobAssign2a3a() throws Exception {
		final String TEST_NAME = "test200BobAssign2a3a";
		TestUtil.displayTestTitle(this, TEST_NAME);

		// GIVEN
		Task task = taskManager.createTaskInstance(TestPolicyStateRecording.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();

		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		ObjectDelta<UserType> delta = DeltaBuilder.deltaFor(UserType.class, prismContext)
				.item(UserType.F_ASSIGNMENT)
						.add(createAssignmentTo(roleATest2aOid, ObjectTypes.ROLE, prismContext),
								createAssignmentTo(roleATest3aOid, ObjectTypes.ROLE, prismContext))
				.asObjectDeltaCast(userBobOid);
		executeChangesAssertSuccess(delta, null, task, result);

		// THEN
		TestUtil.displayThen(TEST_NAME);
		UserType bob = getUser(userBobOid).asObjectable();
		display("bob", bob);
		result.computeStatus();
		TestUtil.assertSuccess(result);

		assertAssignedRole(userBobOid, roleATest2aOid, task, result);
		assertAssignedRole(userBobOid, roleATest3aOid, task, result);
		assertEquals("Wrong # of assignments", 2, bob.getAssignment().size());
		assertEquals("Wrong policy situations for assignment 1",
				Collections.emptyList(),
				bob.getAssignment().get(0).getPolicySituation());
		assertEquals("Wrong policy situations for assignment 2",
				Collections.emptyList(),
				bob.getAssignment().get(1).getPolicySituation());
	}

	@Test
	public void test200BobAssign2b3b() throws Exception {
		final String TEST_NAME = "test200BobAssign2b3b";
		TestUtil.displayTestTitle(this, TEST_NAME);

		// GIVEN
		Task task = taskManager.createTaskInstance(TestPolicyStateRecording.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();

		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		ObjectDelta<UserType> delta = DeltaBuilder.deltaFor(UserType.class, prismContext)
				.item(UserType.F_ASSIGNMENT)
				.add(createAssignmentTo(roleATest2bOid, ObjectTypes.ROLE, prismContext),
						createAssignmentTo(roleATest3bOid, ObjectTypes.ROLE, prismContext))
				.asObjectDeltaCast(userBobOid);
		executeChangesAssertSuccess(delta, null, task, result);

		// THEN
		TestUtil.displayThen(TEST_NAME);
		UserType bob = getUser(userBobOid).asObjectable();
		display("bob", bob);
		result.computeStatus();
		TestUtil.assertSuccess(result);

		assertAssignedRole(userBobOid, roleATest2aOid, task, result);
		assertAssignedRole(userBobOid, roleATest2bOid, task, result);
		assertAssignedRole(userBobOid, roleATest3aOid, task, result);
		assertAssignedRole(userBobOid, roleATest3bOid, task, result);
		assertEquals("Wrong # of assignments", 4, bob.getAssignment().size());

		// TODO policy state
	}



}
