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
package com.evolveum.midpoint.wf.impl.policy.assignments.metarole;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.builder.DeltaBuilder;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.policy.AbstractWfTestPolicy;
import com.evolveum.midpoint.wf.impl.policy.ApprovalInstruction;
import com.evolveum.midpoint.wf.impl.policy.ExpectedTask;
import com.evolveum.midpoint.wf.impl.policy.ExpectedWorkItem;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkItemType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.createAssignmentTo;
import static org.testng.AssertJUnit.assertEquals;

/**
 * A special test dealing with assigning roles that have different metarole-induced approval policies.
 *
 * Role21 - uses default approval (org:approver)
 * Role22 - uses metarole 1 'default' induced approval (org:special-approver)
 * Role23 - uses both metarole 'default' and 'security' induced approval (org:special-approver and org:security-approver)
 *
 * @author mederly
 */
@ContextConfiguration(locations = {"classpath:ctx-workflow-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestAssignmentsWithDifferentMetaroles extends AbstractWfTestPolicy {

    protected static final Trace LOGGER = TraceManager.getTrace(TestAssignmentsWithDifferentMetaroles.class);

	protected static final File TEST_ASSIGNMENTS_RESOURCE_DIR = new File("src/test/resources/policy/assignments");

	protected static final File ROLE_ROLE21_FILE = new File(TEST_ASSIGNMENTS_RESOURCE_DIR, "role-role21-standard.xml");
	protected static final File ROLE_ROLE22_FILE = new File(TEST_ASSIGNMENTS_RESOURCE_DIR, "role-role22-special.xml");
	protected static final File ROLE_ROLE23_FILE = new File(TEST_ASSIGNMENTS_RESOURCE_DIR, "role-role23-special-and-security.xml");
	protected static final File ROLE_ROLE24_FILE = new File(TEST_ASSIGNMENTS_RESOURCE_DIR, "role-role24-approval-and-enforce.xml");

	protected static final File USER_LEAD21_FILE = new File(TEST_ASSIGNMENTS_RESOURCE_DIR, "user-lead21.xml");
	protected static final File USER_LEAD22_FILE = new File(TEST_ASSIGNMENTS_RESOURCE_DIR, "user-lead22.xml");
	protected static final File USER_LEAD23_FILE = new File(TEST_ASSIGNMENTS_RESOURCE_DIR, "user-lead23.xml");
	protected static final File USER_LEAD24_FILE = new File(TEST_ASSIGNMENTS_RESOURCE_DIR, "user-lead24.xml");

	protected String roleRole21Oid;
	protected String roleRole22Oid;
	protected String roleRole23Oid;
	protected String roleRole24Oid;

	protected String userLead21Oid;
	protected String userLead22Oid;
	protected String userLead23Oid;
	protected String userLead24Oid;

	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);

		roleRole21Oid = repoAddObjectFromFile(ROLE_ROLE21_FILE, initResult).getOid();
		roleRole22Oid = repoAddObjectFromFile(ROLE_ROLE22_FILE, initResult).getOid();
		roleRole23Oid = repoAddObjectFromFile(ROLE_ROLE23_FILE, initResult).getOid();
		roleRole24Oid = repoAddObjectFromFile(ROLE_ROLE24_FILE, initResult).getOid();

		userLead21Oid = addAndRecomputeUser(USER_LEAD21_FILE, initTask, initResult);
		userLead22Oid = addAndRecomputeUser(USER_LEAD22_FILE, initTask, initResult);
		userLead23Oid = addAndRecomputeUser(USER_LEAD23_FILE, initTask, initResult);
		userLead24Oid = addAndRecomputeUser(USER_LEAD24_FILE, initTask, initResult);
	}

	@Test
	public void test102AddRoles123AssignmentYYYYDeputy() throws Exception {
		final String TEST_NAME = "test102AddRoles123AssignmentYYYYDeputy";
		TestUtil.displayTestTile(this, TEST_NAME);
		login(userAdministrator);

		unassignAllRoles(userJackOid, true);
		executeAssignRoles123ToJack(TEST_NAME, false, true, true, true, true, true);
	}

	@Test
	public void test105AddRoles123AssignmentYYYYImmediate() throws Exception {
		final String TEST_NAME = "test105AddRoles123AssignmentYYYYImmediate";
		TestUtil.displayTestTile(this, TEST_NAME);
		login(userAdministrator);

		unassignAllRoles(userJackOid, true);
		executeAssignRoles123ToJack(TEST_NAME, true, true, true, true, true, false);
	}

	@Test
	public void test110AddRoles123AssignmentNNNN() throws Exception {
		final String TEST_NAME = "test110AddRoles123AssignmentNNNN";
		TestUtil.displayTestTile(this, TEST_NAME);
		login(userAdministrator);

		unassignAllRoles(userJackOid, true);
		executeAssignRoles123ToJack(TEST_NAME, true, false, false, false, false, false);
	}

	@Test
	public void test115AddRoles123AssignmentNNNNImmediate() throws Exception {
		final String TEST_NAME = "test115AddRoles123AssignmentNNNNImmediate";
		TestUtil.displayTestTile(this, TEST_NAME);
		login(userAdministrator);

		unassignAllRoles(userJackOid, true);
		executeAssignRoles123ToJack(TEST_NAME, true, false, false, false, false, false);
	}

	@Test
	public void test120AddRoles123AssignmentYNNN() throws Exception {
		final String TEST_NAME = "test120AddRoles123AssignmentYNNN";
		TestUtil.displayTestTile(this, TEST_NAME);
		login(userAdministrator);

		unassignAllRoles(userJackOid, true);
		executeAssignRoles123ToJack(TEST_NAME, true, true, false, false, false, false);
	}

	@Test
	public void test125AddRoles123AssignmentYNNNImmediate() throws Exception {
		final String TEST_NAME = "test125AddRoles123AssignmentYNNNImmediate";
		TestUtil.displayTestTile(this, TEST_NAME);
		login(userAdministrator);

		unassignAllRoles(userJackOid, true);
		executeAssignRoles123ToJack(TEST_NAME, true, true, false, false, false, false);
	}

	@Test
	public void test130AddRoles123AssignmentYYYN() throws Exception {
		final String TEST_NAME = "test130AddRoles123AssignmentYYYN";
		TestUtil.displayTestTile(this, TEST_NAME);
		login(userAdministrator);

		unassignAllRoles(userJackOid, true);
		executeAssignRoles123ToJack(TEST_NAME, false, true, true, true, false, false);
	}

	@Test
	public void test132AddRoles123AssignmentYYYNDeputy() throws Exception {
		final String TEST_NAME = "test132AddRoles123AssignmentYYYNDeputy";
		TestUtil.displayTestTile(this, TEST_NAME);
		login(userAdministrator);

		unassignAllRoles(userJackOid, true);
		executeAssignRoles123ToJack(TEST_NAME, false, true, true, true, false, true);
	}

	@Test
	public void test135AddRoles123AssignmentYYYNImmediate() throws Exception {
		final String TEST_NAME = "test135AddRoles123AssignmentYYYNImmediate";
		TestUtil.displayTestTile(this, TEST_NAME);
		login(userAdministrator);

		unassignAllRoles(userJackOid, true);
		executeAssignRoles123ToJack(TEST_NAME, true, true, true, true, false, false);
	}

	/**
	 * Attempt to assign roles 21-23 along with changing description.
	 */
	@Test
	public void test200AddRoles123AssignmentYYYY() throws Exception {
		final String TEST_NAME = "test200AddRoles012AssignmentYYYY";
		TestUtil.displayTestTile(this, TEST_NAME);
		login(userAdministrator);

		unassignAllRoles(userJackOid);
		executeAssignRoles123ToJack(TEST_NAME, false, true, true, true, true, false);
	}

	@Test
	public void test210DeleteRoles123AssignmentN() throws Exception {
		final String TEST_NAME = "test210DeleteRoles123AssignmentN";
		TestUtil.displayTestTile(this, TEST_NAME);
		login(userAdministrator);

		executeUnassignRoles123ToJack(TEST_NAME, false, false, false, true);
	}

	@Test
	public void test212DeleteRoles123AssignmentNById() throws Exception {
		final String TEST_NAME = "test212DeleteRoles123AssignmentNById";
		TestUtil.displayTestTile(this, TEST_NAME);
		login(userAdministrator);

		executeUnassignRoles123ToJack(TEST_NAME, false, false, true, false);
	}

	@Test
	public void test218DeleteRoles123AssignmentY() throws Exception {
		final String TEST_NAME = "test218DeleteRoles123AssignmentY";
		TestUtil.displayTestTile(this, TEST_NAME);
		login(userAdministrator);

		executeUnassignRoles123ToJack(TEST_NAME, false, true, false, false);
	}

	@Test
	public void test220AddRoles123AssignmentYYYY() throws Exception {
		final String TEST_NAME = "test220AddRoles012AssignmentYYYY";
		TestUtil.displayTestTile(this, TEST_NAME);
		login(userAdministrator);

		unassignAllRoles(userJackOid, true);
		executeAssignRoles123ToJack(TEST_NAME, false, true, true, true, true, false);
	}

	@Test
	public void test230DeleteRoles123AssignmentYById() throws Exception {
		final String TEST_NAME = "test230DeleteRoles123AssignmentYById";
		TestUtil.displayTestTile(this, TEST_NAME);
		login(userAdministrator);

		executeUnassignRoles123ToJack(TEST_NAME, false, true, true, true);
	}

	/**
	 * MID-3836
	 */
	@Test
	public void test300ApprovalAndEnforce() throws Exception {
		final String TEST_NAME = "test300ApprovalAndEnforce";
		TestUtil.displayTestTile(this, TEST_NAME);
		login(userAdministrator);
		Task task = createTask(TEST_NAME);
		task.setOwner(userAdministrator);
		OperationResult result = task.getResult();

		try {
			assignRole(userJackOid, roleRole24Oid, task, result);
		} catch (PolicyViolationException e) {
			// ok
			System.out.println("Got expected exception: " + e);
		}
		List<WorkItemType> currentWorkItems = modelService.searchContainers(WorkItemType.class, null, null, task, result);
		display("current work items", currentWorkItems);
		assertEquals("Wrong # of current work items", 0, currentWorkItems.size());
	}

	private void executeAssignRoles123ToJack(String TEST_NAME, boolean immediate,
			boolean approve1, boolean approve2, boolean approve3a, boolean approve3b, boolean securityDeputy) throws Exception {
		Task task = createTask("executeAssignRoles123ToJack");
		PrismObject<UserType> jack = getUser(userJackOid);
		@SuppressWarnings("unchecked")
		ObjectDelta<UserType> addRole1Delta = (ObjectDelta<UserType>) DeltaBuilder
				.deltaFor(UserType.class, prismContext)
				.item(UserType.F_ASSIGNMENT).add(createAssignmentTo(roleRole21Oid, ObjectTypes.ROLE, prismContext))
				.asObjectDelta(userJackOid);
		@SuppressWarnings("unchecked")
		ObjectDelta<UserType> addRole2Delta = (ObjectDelta<UserType>) DeltaBuilder
				.deltaFor(UserType.class, prismContext)
				.item(UserType.F_ASSIGNMENT).add(createAssignmentTo(roleRole22Oid, ObjectTypes.ROLE, prismContext))
				.asObjectDelta(userJackOid);
		@SuppressWarnings("unchecked")
		ObjectDelta<UserType> addRole3Delta = (ObjectDelta<UserType>) DeltaBuilder
				.deltaFor(UserType.class, prismContext)
				.item(UserType.F_ASSIGNMENT).add(createAssignmentTo(roleRole23Oid, ObjectTypes.ROLE, prismContext))
				.asObjectDelta(userJackOid);
		@SuppressWarnings("unchecked")
		ObjectDelta<UserType> changeDescriptionDelta = (ObjectDelta<UserType>) DeltaBuilder
				.deltaFor(UserType.class, prismContext)
				.item(UserType.F_DESCRIPTION).replace(TEST_NAME)
				.asObjectDelta(userJackOid);
		ObjectDelta<UserType> primaryDelta = ObjectDelta.summarize(addRole1Delta, addRole2Delta, addRole3Delta, changeDescriptionDelta);
		ObjectDelta<UserType> delta0 = changeDescriptionDelta.clone();
		String originalDescription = getUser(userJackOid).asObjectable().getDescription();
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
				return 3;
			}

			@Override
			protected List<Boolean> getApprovals() {
				return Arrays.asList(approve1, approve2, approve3a && approve3b);
			}

			@Override
			protected List<ObjectDelta<UserType>> getExpectedDeltasToApprove() {
				return Arrays.asList(addRole1Delta.clone(), addRole2Delta.clone(), addRole3Delta.clone());
			}

			@Override
			protected ObjectDelta<UserType> getExpectedDelta0() {
				return delta0.clone();
			}

			@Override
			protected String getObjectOid() {
				return jack.getOid();
			}

			@Override
			protected List<ExpectedTask> getExpectedTasks() {
				return Arrays.asList(
						new ExpectedTask(roleRole21Oid, "Assigning Role21 to jack"),
						new ExpectedTask(roleRole22Oid, "Assigning Role22 to jack"),
						new ExpectedTask(roleRole23Oid, "Assigning Role23 to jack"));
			}

			// after first step
			@Override
			protected List<ExpectedWorkItem> getExpectedWorkItems() {
				List<ExpectedTask> tasks = getExpectedTasks();
				return Arrays.asList(
						new ExpectedWorkItem(userLead21Oid, roleRole21Oid, tasks.get(0)),
						new ExpectedWorkItem(userLead22Oid, roleRole22Oid, tasks.get(1)),
						new ExpectedWorkItem(userLead23Oid, roleRole23Oid, tasks.get(2))
				);
			}

			@Override
			protected void assertDeltaExecuted(int number, boolean yes, Task rootTask, OperationResult result) throws Exception {
				switch (number) {
					case 0:
						if (yes) {
							assertUserProperty(userJackOid, UserType.F_DESCRIPTION, TEST_NAME);
						} else {
							if (originalDescription != null) {
								assertUserProperty(userJackOid, UserType.F_DESCRIPTION, originalDescription);
							} else {
								assertUserNoProperty(userJackOid, UserType.F_DESCRIPTION);
							}
						}
						break;
					case 1:
					case 2:
					case 3:
						String[] oids = { roleRole21Oid, roleRole22Oid, roleRole23Oid };
					if (yes) {
						assertAssignedRole(userJackOid, oids[number-1], rootTask, result);
					} else {
						assertNotAssignedRole(userJackOid, oids[number-1], rootTask, result);
					}
					break;

				}
			}

			@Override
			protected Boolean decideOnApproval(String executionId, org.activiti.engine.task.Task task) throws Exception {
				return null;            // ignore this way of approving
			}

			@Override
			public List<ApprovalInstruction> getApprovalSequence() {
				List<ExpectedTask> tasks = getExpectedTasks();
				List<ApprovalInstruction> instructions = new ArrayList<>();
				instructions.add(new ApprovalInstruction(
								new ExpectedWorkItem(userLead21Oid, roleRole21Oid, tasks.get(0)), approve1, userLead21Oid));
				instructions.add(new ApprovalInstruction(
								new ExpectedWorkItem(userLead22Oid, roleRole22Oid, tasks.get(1)), approve2, userLead22Oid));
				instructions.add(new ApprovalInstruction(
								new ExpectedWorkItem(userLead23Oid, roleRole23Oid, tasks.get(2)), approve3a, userLead23Oid));
				if (approve3a) {
					ExpectedWorkItem expectedWorkItem = new ExpectedWorkItem(userSecurityApproverOid, roleRole23Oid, tasks.get(2));
					ApprovalInstruction.CheckedRunnable before = () -> {
						login(getUserFromRepo(userSecurityApproverOid));
						checkVisibleWorkItem(expectedWorkItem, 1, task, task.getResult());
						login(getUserFromRepo(userSecurityApproverDeputyOid));
						checkVisibleWorkItem(expectedWorkItem, 1, task, task.getResult());
						login(getUserFromRepo(userSecurityApproverDeputyLimitedOid));
						checkVisibleWorkItem(null, 0, task, task.getResult());
					};
					instructions.add(new ApprovalInstruction(expectedWorkItem, approve3b,
							securityDeputy ? userSecurityApproverDeputyOid : userSecurityApproverOid, before, null));
				}
				return instructions;
			}
		}, 3, immediate);
	}

	private void executeUnassignRoles123ToJack(String TEST_NAME, boolean immediate, boolean approve, boolean byId, boolean has1and2) throws Exception {
		PrismObject<UserType> jack = getUser(userJackOid);
		AssignmentType a1 = has1and2 ? findAssignmentByTargetRequired(jack, roleRole21Oid) : null;
		AssignmentType a2 = has1and2 ? findAssignmentByTargetRequired(jack, roleRole22Oid) : null;
		AssignmentType a3 = findAssignmentByTargetRequired(jack, roleRole23Oid);
		AssignmentType del1 = toDelete(a1, byId);
		AssignmentType del2 = toDelete(a2, byId);
		AssignmentType del3 = toDelete(a3, byId);
		@SuppressWarnings("unchecked")
		ObjectDelta<UserType> deleteRole1Delta = has1and2 ? (ObjectDelta<UserType>) DeltaBuilder
				.deltaFor(UserType.class, prismContext)
				.item(UserType.F_ASSIGNMENT).delete(del1)
				.asObjectDelta(userJackOid) : null;
		@SuppressWarnings("unchecked")
		ObjectDelta<UserType> deleteRole2Delta = has1and2 ? (ObjectDelta<UserType>) DeltaBuilder
				.deltaFor(UserType.class, prismContext)
				.item(UserType.F_ASSIGNMENT).delete(del2)
				.asObjectDelta(userJackOid) : null;
		@SuppressWarnings("unchecked")
		ObjectDelta<UserType> deleteRole3Delta = (ObjectDelta<UserType>) DeltaBuilder
				.deltaFor(UserType.class, prismContext)
				.item(UserType.F_ASSIGNMENT).delete(del3)
				.asObjectDelta(userJackOid);
		@SuppressWarnings("unchecked")
		ObjectDelta<UserType> changeDescriptionDelta = (ObjectDelta<UserType>) DeltaBuilder
				.deltaFor(UserType.class, prismContext)
				.item(UserType.F_DESCRIPTION).replace(TEST_NAME)
				.asObjectDelta(userJackOid);
		ObjectDelta<UserType> primaryDelta = ObjectDelta.summarize(changeDescriptionDelta, deleteRole1Delta, deleteRole2Delta, deleteRole3Delta);
		ObjectDelta<UserType> delta0 = ObjectDelta.summarize(changeDescriptionDelta, deleteRole1Delta, deleteRole2Delta);
		String originalDescription = getUser(userJackOid).asObjectable().getDescription();
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
				return Arrays.asList(approve);
			}

			@Override
			protected List<ObjectDelta<UserType>> getExpectedDeltasToApprove() {
				return Arrays.asList(deleteRole3Delta.clone());
			}

			@Override
			protected ObjectDelta<UserType> getExpectedDelta0() {
				return delta0.clone();
			}

			@Override
			protected String getObjectOid() {
				return jack.getOid();
			}

			@Override
			protected List<ExpectedTask> getExpectedTasks() {
				return Arrays.asList(
						new ExpectedTask(roleRole23Oid, "Unassigning Role23 from jack"));
			}

			// after first step
			@Override
			protected List<ExpectedWorkItem> getExpectedWorkItems() {
				List<ExpectedTask> tasks = getExpectedTasks();
				return Arrays.asList(
						new ExpectedWorkItem(userSecurityApproverOid, roleRole23Oid, tasks.get(0))
				);
			}

			@Override
			protected void assertDeltaExecuted(int number, boolean yes, Task rootTask, OperationResult result) throws Exception {
				switch (number) {
					case 0:
						if (yes) {
							assertUserProperty(userJackOid, UserType.F_DESCRIPTION, TEST_NAME);
						} else {
							if (originalDescription != null) {
								assertUserProperty(userJackOid, UserType.F_DESCRIPTION, originalDescription);
							} else {
								assertUserNoProperty(userJackOid, UserType.F_DESCRIPTION);
							}
						}
						if (yes || !has1and2) {
							assertNotAssignedRole(userJackOid, roleRole21Oid, rootTask, result);
							assertNotAssignedRole(userJackOid, roleRole22Oid, rootTask, result);
						} else {
							assertAssignedRole(userJackOid, roleRole21Oid, rootTask, result);
							assertAssignedRole(userJackOid, roleRole22Oid, rootTask, result);
						}
						break;
					case 1:
						if (yes) {
							assertNotAssignedRole(userJackOid, roleRole23Oid, rootTask, result);
						} else {
							assertAssignedRole(userJackOid, roleRole23Oid, rootTask, result);
						}
						break;
					default:
						throw new IllegalArgumentException("Unexpected delta number: " + number);
				}
			}

			@Override
			protected Boolean decideOnApproval(String executionId, org.activiti.engine.task.Task task) throws Exception {
				return null;            // ignore this way of approving
			}

			@Override
			public List<ApprovalInstruction> getApprovalSequence() {
				List<ExpectedTask> tasks = getExpectedTasks();
				List<ApprovalInstruction> instructions = new ArrayList<>();
				instructions.add(new ApprovalInstruction(
						new ExpectedWorkItem(userSecurityApproverOid, roleRole23Oid, tasks.get(0)), approve,
						userSecurityApproverOid));
				return instructions;
			}
		}, 1, immediate);
	}

	private AssignmentType toDelete(AssignmentType assignment, boolean byId) {
		if (assignment == null) {
			return null;
		}
		if (!byId) {
			return assignment.clone();
		} else {
			AssignmentType rv = new AssignmentType(prismContext);
			rv.setId(assignment.getId());
			return rv;
		}
	}

	@Test
	public void zzzMarkAsNotInitialized() {
		display("Setting class as not initialized");
		unsetSystemInitialized();
	}

}
