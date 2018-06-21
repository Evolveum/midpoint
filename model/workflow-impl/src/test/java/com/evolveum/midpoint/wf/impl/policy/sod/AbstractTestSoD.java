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
package com.evolveum.midpoint.wf.impl.policy.sod;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.builder.DeltaBuilder;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.policy.AbstractWfTestPolicy;
import com.evolveum.midpoint.wf.impl.policy.ExpectedTask;
import com.evolveum.midpoint.wf.impl.policy.ExpectedWorkItem;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.createAssignmentTo;

/**
 * Testing approvals of role SoD: assigning roles that are in conflict.
 *
 * Subclasses provide specializations regarding ways how rules and/or approvers are attached to roles.
 *
 * @author mederly
 */
@ContextConfiguration(locations = {"classpath:ctx-workflow-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class AbstractTestSoD extends AbstractWfTestPolicy {

    protected static final Trace LOGGER = TraceManager.getTrace(AbstractTestSoD.class);

	protected static final File TEST_SOD_RESOURCE_DIR = new File("src/test/resources/policy/sod");

	protected static final File METAROLE_CRIMINAL_EXCLUSION_FILE = new File(TEST_SOD_RESOURCE_DIR, "metarole-criminal-exclusion.xml");
	protected static final File ROLE_JUDGE_FILE = new File(TEST_SOD_RESOURCE_DIR, "role-judge.xml");
	protected static final File ROLE_PIRATE_FILE = new File(TEST_SOD_RESOURCE_DIR, "role-pirate.xml");
	protected static final File ROLE_THIEF_FILE = new File(TEST_SOD_RESOURCE_DIR, "role-thief.xml");
	protected static final File ROLE_RESPECTABLE_FILE = new File(TEST_SOD_RESOURCE_DIR, "role-respectable.xml");
	protected static final File USER_SOD_APPROVER_FILE = new File(TEST_SOD_RESOURCE_DIR, "user-sod-approver.xml");

	protected String metaroleCriminalExclusion;
	protected String roleJudgeOid;
	protected String rolePirateOid;
	protected String roleThiefOid;
	protected String roleRespectableOid;
	protected String userSodApproverOid;

	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);
		metaroleCriminalExclusion = repoAddObjectFromFile(METAROLE_CRIMINAL_EXCLUSION_FILE, initResult).getOid();
		roleJudgeOid = repoAddObjectFromFile(ROLE_JUDGE_FILE, initResult).getOid();
		rolePirateOid = repoAddObjectFromFile(ROLE_PIRATE_FILE, initResult).getOid();
		roleThiefOid = repoAddObjectFromFile(ROLE_THIEF_FILE, initResult).getOid();
		roleRespectableOid = repoAddObjectFromFile(ROLE_RESPECTABLE_FILE, initResult).getOid();
		userSodApproverOid = addAndRecomputeUser(USER_SOD_APPROVER_FILE, initTask, initResult);

		//DebugUtil.setDetailedDebugDump(true);
	}

	/**
	 * Assign Judge to jack. This should work without approvals.
	 */
	@Test
	public void test010AssignRoleJudge() throws Exception {
		final String TEST_NAME = "test010AssignRoleJudge";
		TestUtil.displayTestTitle(this, TEST_NAME);
		login(userAdministrator);

		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		// WHEN
		assignRole(userJackOid, roleJudgeOid, task, result);

		// THEN
		display("jack as a Judge", getUser(userJackOid));
		assertAssignedRole(userJackOid, roleJudgeOid, task, result);
	}

	/**
	 * Assign Pirate to jack. This should trigger an approval.
	 */
	@Test
	public void test020AssignRolePirate() throws Exception {
		final String TEST_NAME = "test020AssignRolePirate";
		TestUtil.displayTestTitle(this, TEST_NAME);
		login(userAdministrator);

		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		PrismObject<UserType> jack = getUser(userJackOid);
		String originalDescription = jack.asObjectable().getDescription();

		@SuppressWarnings("unchecked")
		ObjectDelta<UserType> addPirateDelta = (ObjectDelta<UserType>) DeltaBuilder
				.deltaFor(UserType.class, prismContext)
				.item(UserType.F_ASSIGNMENT).add(createAssignmentTo(rolePirateOid, ObjectTypes.ROLE, prismContext))
				.asObjectDelta(userJackOid);
		@SuppressWarnings("unchecked")
		ObjectDelta<UserType> changeDescriptionDelta = (ObjectDelta<UserType>) DeltaBuilder
				.deltaFor(UserType.class, prismContext)
				.item(UserType.F_DESCRIPTION).replace("Pirate Judge")
				.asObjectDelta(userJackOid);
		ObjectDelta<UserType> primaryDelta = ObjectDelta.summarize(addPirateDelta, changeDescriptionDelta);

		// WHEN+THEN
		executeTest2(TEST_NAME, new TestDetails2<UserType>() {
			@Override
			protected PrismObject<UserType> getFocus(OperationResult result) throws Exception {
				return jack.clone();
			}

			@Override
			protected ObjectDelta<UserType> getFocusDelta() throws SchemaException {
				return primaryDelta.clone();
			}

			@Override
			protected int getNumberOfDeltasToApprove() {
				return 1;
			}

			@Override
			protected List<Boolean> getApprovals() {
				return Collections.singletonList(true);
			}

			@Override
			protected List<ObjectDelta<UserType>> getExpectedDeltasToApprove() {
				return Arrays.asList(addPirateDelta.clone());
			}

			@Override
			protected ObjectDelta<UserType> getExpectedDelta0() {
				//return ObjectDelta.createEmptyModifyDelta(UserType.class, jack.getOid(), prismContext);
				//return ObjectDelta.createModifyDelta(jack.getOid(), Collections.emptyList(), UserType.class, prismContext);
				return changeDescriptionDelta.clone();
			}

			@Override
			protected String getObjectOid() {
				return jack.getOid();
			}

			@Override
			protected List<ExpectedTask> getExpectedTasks() {
				return Collections.singletonList(
						new ExpectedTask(rolePirateOid, "Role \"Pirate\" excludes role \"Judge\""));
			}

			@Override
			protected List<ExpectedWorkItem> getExpectedWorkItems() {
				List<ExpectedTask> etasks = getExpectedTasks();
				return Collections.singletonList(
						new ExpectedWorkItem(userSodApproverOid, rolePirateOid, etasks.get(0)));
			}

			@Override
			protected void assertDeltaExecuted(int number, boolean yes, Task rootTask, OperationResult result) throws Exception {
				switch (number) {
					case 0:
						if (yes) {
							assertUserProperty(userJackOid, UserType.F_DESCRIPTION, "Pirate Judge");
						} else {
							if (originalDescription != null) {
								assertUserProperty(userJackOid, UserType.F_DESCRIPTION, originalDescription);
							} else {
								assertUserNoProperty(userJackOid, UserType.F_DESCRIPTION);
							}
						}
						break;

					case 1:
						if (yes) {
							assertAssignedRole(userJackOid, rolePirateOid, rootTask, result);
						} else {
							assertNotAssignedRole(userJackOid, rolePirateOid, rootTask, result);
						}
						break;
				}
			}

			@Override
			protected Boolean decideOnApproval(String executionId, org.activiti.engine.task.Task task) throws Exception {
				login(getUser(userSodApproverOid));
				return true;
			}

		}, 1, false);

		// THEN
		display("jack as a Pirate + Judge", getUser(userJackOid));
		assertAssignedRole(userJackOid, roleJudgeOid, task, result);
		assertAssignedRole(userJackOid, rolePirateOid, task, result);
	}

	/**
	 * Assign Respectable to jack. This should trigger an approval as well (because it implies a Thief).
	 */
	@Test
	public void test030AssignRoleRespectable() throws Exception {
		final String TEST_NAME = "test030AssignRoleRespectable";
		TestUtil.displayTestTitle(this, TEST_NAME);
		login(userAdministrator);

		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		// GIVEN
		unassignRole(userJackOid, rolePirateOid, task, result);
		assertNotAssignedRole(userJackOid, rolePirateOid, task, result);

		// WHEN+THEN
		PrismObject<UserType> jack = getUser(userJackOid);
		@SuppressWarnings("unchecked")
		ObjectDelta<UserType> addRespectableDelta = (ObjectDelta<UserType>) DeltaBuilder
				.deltaFor(UserType.class, prismContext)
				.item(UserType.F_ASSIGNMENT).add(createAssignmentTo(roleRespectableOid, ObjectTypes.ROLE, prismContext))
				.asObjectDelta(userJackOid);

		// WHEN+THEN
		executeTest2(TEST_NAME, new TestDetails2<UserType>() {
			@Override
			protected PrismObject<UserType> getFocus(OperationResult result) throws Exception {
				return jack.clone();
			}

			@Override
			protected ObjectDelta<UserType> getFocusDelta() throws SchemaException {
				return addRespectableDelta.clone();
			}

			@Override
			protected int getNumberOfDeltasToApprove() {
				return 1;
			}

			@Override
			protected List<Boolean> getApprovals() {
				return Collections.singletonList(true);
			}

			@Override
			protected List<ObjectDelta<UserType>> getExpectedDeltasToApprove() {
				return Arrays.asList(addRespectableDelta.clone());
			}

			@Override
			protected ObjectDelta<UserType> getExpectedDelta0() {
				//return ObjectDelta.createEmptyModifyDelta(UserType.class, jack.getOid(), prismContext);
				return ObjectDelta.createModifyDelta(jack.getOid(), Collections.emptyList(), UserType.class, prismContext);
			}

			@Override
			protected String getObjectOid() {
				return jack.getOid();
			}

			@Override
			protected List<ExpectedTask> getExpectedTasks() {
				return Collections.singletonList(
						new ExpectedTask(roleRespectableOid, "Role \"Thief\" (Respectable -> Thief) excludes role \"Judge\""));
			}

			@Override
			protected List<ExpectedWorkItem> getExpectedWorkItems() {
				List<ExpectedTask> etasks = getExpectedTasks();
				return Collections.singletonList(
						new ExpectedWorkItem(userSodApproverOid, roleRespectableOid, etasks.get(0)));
			}

			@Override
			protected void assertDeltaExecuted(int number, boolean yes, Task rootTask, OperationResult result) throws Exception {
				switch (number) {
					case 1:
						if (yes) {
							assertAssignedRole(userJackOid, roleRespectableOid, rootTask, result);
						} else {
							assertNotAssignedRole(userJackOid, roleRespectableOid, rootTask, result);
						}
						break;
				}
			}

			@Override
			protected Boolean decideOnApproval(String executionId, org.activiti.engine.task.Task task) throws Exception {
				login(getUser(userSodApproverOid));
				return true;
			}

		}, 1, false);

		// THEN
		display("jack as a Judge + Respectable", getUser(userJackOid));
		assertAssignedRole(userJackOid, roleJudgeOid, task, result);
		assertAssignedRole(userJackOid, roleRespectableOid, task, result);
	}

	@Test
	public void zzzMarkAsNotInitialized() {
		display("Setting class as not initialized");
		unsetSystemInitialized();
	}

}
