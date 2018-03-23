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

package com.evolveum.midpoint.wf.impl.policy.other;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.builder.DeltaBuilder;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.wf.impl.policy.AbstractWfTestPolicy;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import java.io.File;
import java.util.List;

import static com.evolveum.midpoint.model.api.ModelExecuteOptions.createExecuteImmediatelyAfterApproval;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.TaskExecutionStatusType.CLOSED;
import static org.testng.AssertJUnit.assertEquals;

/**
 * Test for MID-4178.
 *
 * @author mederly
 */
@ContextConfiguration(locations = {"classpath:ctx-workflow-test-main.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestSecondaryChanges extends AbstractWfTestPolicy {

	protected static final File TEST_RESOURCE_DIR = new File("src/test/resources/policy/secondary");

	private static final File ROLE_ORDINARY_1_FILE = new File(TEST_RESOURCE_DIR, "role-ordinary-1.xml");
	private static final File ROLE_ORDINARY_2_FILE = new File(TEST_RESOURCE_DIR, "role-ordinary-2.xml");
	private static final File ROLE_PRIZE_BRONZE_FILE = new File(TEST_RESOURCE_DIR, "role-prize-bronze.xml");
	private static final File ROLE_PRIZE_SILVER_FILE = new File(TEST_RESOURCE_DIR, "role-prize-silver.xml");
	private static final File ROLE_PRIZE_GOLD_FILE = new File(TEST_RESOURCE_DIR, "role-prize-gold.xml");
	private static final File USER_RICHARD_FILE = new File(TEST_RESOURCE_DIR, "user-richard.xml");
	private static final File USER_PETER_FILE = new File(TEST_RESOURCE_DIR, "user-peter.xml");

	private String roleOrdinary1Oid;
	private String roleOrdinary2Oid;
	private String rolePrizeBronzeOid;
	private String rolePrizeSilverOid;
	private String rolePrizeGoldOid;
	private String userRichardOid;
	private String userPeterOid;

	@Override
	protected PrismObject<UserType> getDefaultActor() {
		return userAdministrator;
	}

	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);

		roleOrdinary1Oid = repoAddObjectFromFile(ROLE_ORDINARY_1_FILE, initResult).getOid();
		roleOrdinary2Oid = repoAddObjectFromFile(ROLE_ORDINARY_2_FILE, initResult).getOid();
		rolePrizeBronzeOid = repoAddObjectFromFile(ROLE_PRIZE_BRONZE_FILE, initResult).getOid();
		rolePrizeSilverOid = repoAddObjectFromFile(ROLE_PRIZE_SILVER_FILE, initResult).getOid();
		rolePrizeGoldOid = repoAddObjectFromFile(ROLE_PRIZE_GOLD_FILE, initResult).getOid();

		userRichardOid = addAndRecompute(USER_RICHARD_FILE, initTask, initResult);
		userPeterOid = addAndRecompute(USER_PETER_FILE, initTask, initResult);

		DebugUtil.setPrettyPrintBeansAs(PrismContext.LANG_YAML);
	}

	private String rootTaskOid;

	@Test
	public void test100RequestSilverAndOrdinaryImmediate() throws Exception {
		final String TEST_NAME = "test100RequestSilverAndOrdinaryImmediate";
		TestUtil.displayTestTitle(this, TEST_NAME);
		login(userAdministrator);

		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		// WHEN
		displayWhen(TEST_NAME);
		ObjectDelta<UserType> assignDelta = DeltaBuilder.deltaFor(UserType.class, prismContext)
				.item(UserType.F_ASSIGNMENT).add(
						ObjectTypeUtil.createAssignmentTo(rolePrizeSilverOid, ObjectTypes.ROLE, prismContext),
						ObjectTypeUtil.createAssignmentTo(roleOrdinary1Oid, ObjectTypes.ROLE, prismContext),
						ObjectTypeUtil.createAssignmentTo(roleOrdinary2Oid, ObjectTypes.ROLE, prismContext))
				.asObjectDeltaCast(userRichardOid);
		executeChanges(assignDelta, createExecuteImmediatelyAfterApproval(), task, result); // should start approval processes
		assertNotAssignedRole(userRichardOid, rolePrizeSilverOid, task, result);
		assertNotAssignedRole(userRichardOid, roleOrdinary1Oid, task, result);
		assertNotAssignedRole(userRichardOid, roleOrdinary2Oid, task, result);

		display("Task after operation", task);
		rootTaskOid = wfTaskUtil.getRootTaskOid(task);
		Task rootTask = taskManager.getTask(rootTaskOid, result);
		display("root task", rootTask);
		List<Task> subtasks = rootTask.listSubtasksDeeply(result);
		display("subtasks", subtasks);
	}

	@Test
	public void test110ApproveImmediate() throws Exception {
		final String TEST_NAME = "test110ApproveImmediate";
		TestUtil.displayTestTitle(this, TEST_NAME);
		login(userAdministrator);

		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		approveWorkItemsForTarget(task, result, true, true, roleOrdinary1Oid);
		PrismObject<UserType> richard = getUser(userRichardOid);
		display("richard after ordinary-1 approval", richard);
		assertRoleMembershipRef(richard, rolePrizeBronzeOid, roleOrdinary1Oid);

		approveWorkItemsForTarget(task, result, true, true, roleOrdinary2Oid);
		richard = getUser(userRichardOid);
		display("richard after ordinary-2 approval", richard);
		assertRoleMembershipRef(richard, rolePrizeBronzeOid, roleOrdinary1Oid, roleOrdinary2Oid);

		approveWorkItemsForTarget(task, result, true, true, rolePrizeSilverOid);
		richard = getUser(userRichardOid);
		display("richard after silver approval", richard);
		assertRoleMembershipRef(richard, rolePrizeSilverOid, roleOrdinary1Oid, roleOrdinary2Oid);

		waitForTaskCloseOrSuspend(rootTaskOid, 120000, 1000);

		// THEN

		PrismObject<TaskType> rootTask = getTask(rootTaskOid);
		assertEquals("Wrong root task1 status", CLOSED, rootTask.asObjectable().getExecutionStatus());

		richard = getUser(userRichardOid);
		assertAssignedRole(richard, rolePrizeSilverOid);
		assertAssignedRole(richard, roleOrdinary1Oid);
		assertAssignedRole(richard, roleOrdinary2Oid);
		assertNotAssignedRole(userRichardOid, rolePrizeBronzeOid, task, result);
	}

	@Test
	public void test200RequestSilverAndOrdinaryAfterAll() throws Exception {
		final String TEST_NAME = "test200RequestSilverAndOrdinaryAfterAll";
		TestUtil.displayTestTitle(this, TEST_NAME);
		login(userAdministrator);

		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		// WHEN
		displayWhen(TEST_NAME);
		ObjectDelta<UserType> assignDelta = DeltaBuilder.deltaFor(UserType.class, prismContext)
				.item(UserType.F_ASSIGNMENT).add(
						ObjectTypeUtil.createAssignmentTo(rolePrizeSilverOid, ObjectTypes.ROLE, prismContext),
						ObjectTypeUtil.createAssignmentTo(roleOrdinary1Oid, ObjectTypes.ROLE, prismContext),
						ObjectTypeUtil.createAssignmentTo(roleOrdinary2Oid, ObjectTypes.ROLE, prismContext))
				.asObjectDeltaCast(userPeterOid);
		executeChanges(assignDelta, null, task, result); // should start approval processes
		assertNotAssignedRole(userPeterOid, rolePrizeSilverOid, task, result);
		assertNotAssignedRole(userPeterOid, roleOrdinary1Oid, task, result);
		assertNotAssignedRole(userPeterOid, roleOrdinary2Oid, task, result);

		display("Task after operation", task);
		rootTaskOid = wfTaskUtil.getRootTaskOid(task);
		Task rootTask = taskManager.getTask(rootTaskOid, result);
		display("root task", rootTask);
		List<Task> subtasks = rootTask.listSubtasksDeeply(result);
		display("subtasks", subtasks);
	}

	@Test
	public void test210ApproveAfterAll() throws Exception {
		final String TEST_NAME = "test210ApproveAfterAll";
		TestUtil.displayTestTitle(this, TEST_NAME);
		login(userAdministrator);

		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		approveWorkItemsForTarget(task, result, true, true, roleOrdinary1Oid);
		PrismObject<UserType> peter = getUser(userPeterOid);
		display("peter after ordinary-1 approval", peter);
		assertRoleMembershipRef(peter, rolePrizeBronzeOid);

		approveWorkItemsForTarget(task, result, true, true, roleOrdinary2Oid);
		peter = getUser(userPeterOid);
		display("peter after ordinary-2 approval", peter);
		assertRoleMembershipRef(peter, rolePrizeBronzeOid);

		approveWorkItemsForTarget(task, result, false, true, rolePrizeSilverOid);
		peter = getUser(userPeterOid);
		display("peter after silver rejection", peter);
		assertRoleMembershipRef(peter, rolePrizeBronzeOid);

		waitForTaskCloseOrSuspend(rootTaskOid, 120000, 1000);

		// THEN

		PrismObject<TaskType> rootTask = getTask(rootTaskOid);
		assertEquals("Wrong root task status", CLOSED, rootTask.asObjectable().getExecutionStatus());

		peter = getUser(userPeterOid);
		display("peter after all approvals", peter);
		assertRoleMembershipRef(peter, roleOrdinary1Oid, roleOrdinary2Oid, rolePrizeBronzeOid);
		assertNotAssignedRole(peter, rolePrizeSilverOid);
		assertAssignedRole(peter, roleOrdinary1Oid);
		assertAssignedRole(peter, roleOrdinary2Oid);
		assertAssignedRole(userPeterOid, rolePrizeBronzeOid, task, result);
	}

}
