/*
 * Copyright (c) 2010-2016 Evolveum
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
package com.evolveum.midpoint.wf.impl.policy.metarole;

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
import com.evolveum.midpoint.wf.impl.policy.ApprovalInstruction;
import com.evolveum.midpoint.wf.impl.policy.ExpectedTask;
import com.evolveum.midpoint.wf.impl.policy.ExpectedWorkItem;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.createAssignmentTo;
import static com.evolveum.midpoint.test.IntegrationTestTools.display;

/**
 * A special test dealing with assigning roles that have different metarole-induced approval policies.
 *
 * Role20 - uses default approval (org:approver)
 * Role21 - uses metarole 1 'default' induced approval (org:special-approver)
 * Role22 - uses both metarole 'default' and 'security' induced approval (org:special-approver and org:security-approver)
 *
 * @author mederly
 */
@ContextConfiguration(locations = {"classpath:ctx-workflow-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestAssignmentsWithDifferentMetaroles extends AbstractWfTestPolicy {

    protected static final Trace LOGGER = TraceManager.getTrace(TestAssignmentsWithDifferentMetaroles.class);

	/**
	 * Attempt to assign roles 21-23 along with changing description.
	 */
	@Test
	public void test100AddRoles123AssignmentYYYY() throws Exception {
		final String TEST_NAME = "test100AddRoles012AssignmentYYYY";
		TestUtil.displayTestTile(this, TEST_NAME);
		login(userAdministrator);

		unassignAllRoles(userJackOid);
		executeAssignRoles123ToJack(TEST_NAME, false, true, true, true, true, false);
	}

	@Test
	public void test102AddRoles123AssignmentYYYYDeputy() throws Exception {
		final String TEST_NAME = "test102AddRoles123AssignmentYYYYDeputy";
		TestUtil.displayTestTile(this, TEST_NAME);
		login(userAdministrator);

		unassignAllRoles(userJackOid);
		executeAssignRoles123ToJack(TEST_NAME, false, true, true, true, true, true);
	}

	@Test
	public void test105AddRoles123AssignmentYYYYImmediate() throws Exception {
		final String TEST_NAME = "test105AddRoles123AssignmentYYYYImmediate";
		TestUtil.displayTestTile(this, TEST_NAME);
		login(userAdministrator);

		unassignAllRoles(userJackOid);
		executeAssignRoles123ToJack(TEST_NAME, true, true, true, true, true, false);
	}

	@Test
	public void test110AddRoles123AssignmentNNNN() throws Exception {
		final String TEST_NAME = "test110AddRoles123AssignmentNNNN";
		TestUtil.displayTestTile(this, TEST_NAME);
		login(userAdministrator);

		unassignAllRoles(userJackOid);
		executeAssignRoles123ToJack(TEST_NAME, true, false, false, false, false, false);
	}

	@Test
	public void test115AddRoles123AssignmentNNNNImmediate() throws Exception {
		final String TEST_NAME = "test115AddRoles123AssignmentNNNNImmediate";
		TestUtil.displayTestTile(this, TEST_NAME);
		login(userAdministrator);

		unassignAllRoles(userJackOid);
		executeAssignRoles123ToJack(TEST_NAME, true, false, false, false, false, false);
	}

	@Test
	public void test120AddRoles123AssignmentYNNN() throws Exception {
		final String TEST_NAME = "test120AddRoles123AssignmentYNNN";
		TestUtil.displayTestTile(this, TEST_NAME);
		login(userAdministrator);

		unassignAllRoles(userJackOid);
		executeAssignRoles123ToJack(TEST_NAME, true, true, false, false, false, false);
	}

	@Test
	public void test125AddRoles123AssignmentYNNNImmediate() throws Exception {
		final String TEST_NAME = "test125AddRoles123AssignmentYNNNImmediate";
		TestUtil.displayTestTile(this, TEST_NAME);
		login(userAdministrator);

		unassignAllRoles(userJackOid);
		executeAssignRoles123ToJack(TEST_NAME, true, true, false, false, false, false);
	}

	@Test
	public void test130AddRoles123AssignmentYYYN() throws Exception {
		final String TEST_NAME = "test130AddRoles123AssignmentYYYN";
		TestUtil.displayTestTile(this, TEST_NAME);
		login(userAdministrator);

		unassignAllRoles(userJackOid);
		executeAssignRoles123ToJack(TEST_NAME, false, true, true, true, false, false);
	}

	@Test
	public void test132AddRoles123AssignmentYYYNDeputy() throws Exception {
		final String TEST_NAME = "test132AddRoles123AssignmentYYYNDeputy";
		TestUtil.displayTestTile(this, TEST_NAME);
		login(userAdministrator);

		unassignAllRoles(userJackOid);
		executeAssignRoles123ToJack(TEST_NAME, false, true, true, true, false, true);
	}

	@Test
	public void test135AddRoles123AssignmentYYYNImmediate() throws Exception {
		final String TEST_NAME = "test135AddRoles123AssignmentYYYNImmediate";
		TestUtil.displayTestTile(this, TEST_NAME);
		login(userAdministrator);

		unassignAllRoles(userJackOid);
		executeAssignRoles123ToJack(TEST_NAME, true, true, true, true, false, false);
	}


//	/**
//	 * Assigning Role1 with two deputies present. (Approved by one of the deputies.)
//	 */
//	@Test
//	public void test132AddRole1aAssignmentWithDeputyApprovedByDeputy1() throws Exception {
//		final String TEST_NAME = "test132AddRole1aAssignmentWithDeputyApprovedByDeputy1";
//		TestUtil.displayTestTile(this, TEST_NAME);
//		login(userAdministrator);
//
//		unassignAllRoles(userJackOid);
//		executeAssignRole1ToJack(TEST_NAME, false, true, userLead1Deputy1Oid);
//	}
//
//
//	protected List<PrismReferenceValue> getPotentialAssignees(PrismObject<UserType> user) {
//		List<PrismReferenceValue> rv = new ArrayList<>();
//		rv.add(ObjectTypeUtil.createObjectRef(user).asReferenceValue());
//		rv.addAll(DeputyUtils.getDelegatorReferences(user.asObjectable()));
//		return rv;
//	}
//
//	protected void assertActiveWorkItems(String approverOid, int expectedCount) throws Exception {
//		if (approverOid == null && expectedCount == 0) {
//			return;
//		}
//		Task task = createTask("query");
//		ObjectQuery query = QueryBuilder.queryFor(WorkItemType.class, prismContext)
//				.item(WorkItemType.F_ASSIGNEE_REF).ref(getPotentialAssignees(getUser(approverOid)))
//				.build();
//		List<WorkItemType> items = modelService.searchContainers(WorkItemType.class, query, null, task, task.getResult());
//		assertEquals("Wrong active work items for " + approverOid, expectedCount, items.size());
//	}

	private void executeAssignRoles123ToJack(String TEST_NAME, boolean immediate,
			boolean approve1, boolean approve2, boolean approve3a, boolean approve3b, boolean securityDeputy) throws Exception {
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
						new ExpectedWorkItem(userLead1Oid, roleRole21Oid, tasks.get(0)),
						new ExpectedWorkItem(userLead2Oid, roleRole22Oid, tasks.get(1)),
						new ExpectedWorkItem(userLead3Oid, roleRole23Oid, tasks.get(2))
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

			/*
			,
						new ExpectedWorkItem(userLead2Oid, roleRole22Oid, etasks.get(1)),

			 */
			@Override
			public List<ApprovalInstruction> getApprovalSequence() {
				List<ExpectedTask> tasks = getExpectedTasks();
				List<ApprovalInstruction> instructions = new ArrayList<>();
				instructions.add(new ApprovalInstruction(
								new ExpectedWorkItem(userLead1Oid, roleRole21Oid, tasks.get(0)), approve1, userLead1Oid));
				instructions.add(new ApprovalInstruction(
								new ExpectedWorkItem(userLead2Oid, roleRole22Oid, tasks.get(1)), approve2, userLead2Oid));
				instructions.add(new ApprovalInstruction(
								new ExpectedWorkItem(userLead3Oid, roleRole23Oid, tasks.get(2)), approve3a, userLead3Oid));
				if (approve3a) {
					instructions.add(new ApprovalInstruction(
							new ExpectedWorkItem(userSecurityApproverOid, roleRole23Oid, tasks.get(2)), approve3b,
									securityDeputy ? userSecurityApproverDeputyOid : userSecurityApproverOid));
				}
				return instructions;
			}
		}, 3, immediate);
	}

	@Test
	public void zzzMarkAsNotInitialized() {
		display("Setting class as not initialized");
		unsetSystemInitialized();
	}

}
