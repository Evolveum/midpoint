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
package com.evolveum.midpoint.wf.impl.policy;

import com.evolveum.midpoint.model.api.context.ModelState;
import com.evolveum.midpoint.model.api.util.DeputyUtils;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.builder.DeltaBuilder;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.processes.common.WorkflowResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkItemType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.createAssignmentTo;
import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.assertEquals;

/**
 * @author mederly
 */
@ContextConfiguration(locations = {"classpath:ctx-workflow-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public abstract class AbstractTestAssignmentApproval extends AbstractWfTestPolicy {

    protected static final Trace LOGGER = TraceManager.getTrace(AbstractTestAssignmentApproval.class);

    protected abstract String getRoleOid(int number);
	protected abstract String getRoleName(int number);

    /**
     * The simplest case: addition of an assignment of single security-sensitive role (Role1).
	 * Although it induces Role10 membership, it is not a problem, as Role10 approver (Lead10) is not imported yet.
     */
	@Test
    public void test010AddRole1Assignment() throws Exception {
        final String TEST_NAME = "test010AddRole1Assignment";
        TestUtil.displayTestTile(this, TEST_NAME);
        login(userAdministrator);

		executeAssignRole1ToJack(TEST_NAME, false, false, null);
	}

	/**
	 * Removing recently added assignment of single security-sensitive role. Should execute without approval (for now).
	 */
	@Test
	public void test020DeleteRole1Assignment() throws Exception {
		final String TEST_NAME = "test020DeleteRole1Assignment";
		TestUtil.displayTestTile(this, TEST_NAME);
		login(userAdministrator);

		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		LensContext<UserType> context = createUserAccountContext();
		fillContextWithUser(context, USER_JACK_OID, result);
		addFocusDeltaToContext(context,
				(ObjectDelta<UserType>) DeltaBuilder.deltaFor(UserType.class, prismContext)
						.item(UserType.F_ASSIGNMENT).delete(createAssignmentTo(getRoleOid(1), ObjectTypes.ROLE, prismContext))
						.asObjectDelta(USER_JACK_OID));
		clockwork.run(context, task, result);

		assertEquals("Wrong context state", ModelState.FINAL, context.getState());
		TestUtil.assertSuccess(result);
		assertNotAssignedRole(getUser(USER_JACK_OID), getRoleOid(1), task, result);
	}

	/**
	 * Repeating test010; this time with Lead10 present. So we are approving an assignment of single security-sensitive role (Role1),
	 * that induces another security-sensitive role (Role10). Because of current implementation constraints, only the first assignment
	 * should be brought to approval.
	 */
	@Test
	public void test030AddRole1AssignmentAgain() throws Exception {
		final String TEST_NAME = "test030AddRole1AssignmentAgain";
		TestUtil.displayTestTile(this, TEST_NAME);
		login(userAdministrator);

		Task task = createTask(TEST_NAME);
		importLead10(task, task.getResult());

		executeAssignRole1ToJack(TEST_NAME, false, false, null);
	}

	/**
	 * The same as above, but with immediate execution.
	 */
	@Test
	public void test040AddRole1AssignmentImmediate() throws Exception {
		final String TEST_NAME = "test040AddRole1AssignmentImmediate";
		TestUtil.displayTestTile(this, TEST_NAME);
		login(userAdministrator);

		unassignAllRoles(USER_JACK_OID);
		executeAssignRole1ToJack(TEST_NAME, true, false, null);
	}

	/**
	 * Attempt to assign roles 1, 2, 3, 4 along with changing description. Assignment of role 4 and description change
	 * are not to be approved.
	 *
	 * Decisions for roles 1-3 are rejected.
	 */
	@Test
	public void test050AddRoles123AssignmentNNN() throws Exception {
		final String TEST_NAME = "test050AddRoles123AssignmentNNN";
		TestUtil.displayTestTile(this, TEST_NAME);
		login(userAdministrator);

		unassignAllRoles(USER_JACK_OID);
		executeAssignRoles123ToJack(TEST_NAME, false, false, false, false);
	}

	/**
	 * The same as above, but with immediate execution.
	 */
	@Test
 	public void test052AddRoles123AssignmentNNNImmediate() throws Exception {
		final String TEST_NAME = "test052AddRoles123AssignmentNNNImmediate";
		TestUtil.displayTestTile(this, TEST_NAME);
		login(userAdministrator);

		unassignAllRoles(USER_JACK_OID);
		executeAssignRoles123ToJack(TEST_NAME, true, false, false, false);
	}

	/**
	 * Attempt to assign roles 1, 2, 3, 4 along with changing description. Assignment of role 4 and description change
	 * are not to be approved.
	 *
	 * Decision for role 1 is accepted.
	 */
	@Test
	public void test060AddRoles123AssignmentYNN() throws Exception {
		final String TEST_NAME = "test060AddRoles123AssignmentYNN";
		TestUtil.displayTestTile(this, TEST_NAME);
		login(userAdministrator);

		unassignAllRoles(USER_JACK_OID);
		executeAssignRoles123ToJack(TEST_NAME, false, true, false, false);
	}

	@Test
	public void test062AddRoles123AssignmentYNNImmediate() throws Exception {
		final String TEST_NAME = "test062AddRoles123AssignmentYNNImmediate";
		TestUtil.displayTestTile(this, TEST_NAME);
		login(userAdministrator);

		unassignAllRoles(USER_JACK_OID);
		executeAssignRoles123ToJack(TEST_NAME, true, true, false, false);
	}

	/**
	 * Attempt to assign roles 1, 2, 3, 4 along with changing description. Assignment of role 4 and description change
	 * are not to be approved.
	 *
	 * Decisions for roles 1-3 are accepted.
	 */
	@Test
	public void test070AddRoles123AssignmentYYY() throws Exception {
		final String TEST_NAME = "test070AddRoles123AssignmentYYY";
		TestUtil.displayTestTile(this, TEST_NAME);
		login(userAdministrator);

		unassignAllRoles(USER_JACK_OID);
		executeAssignRoles123ToJack(TEST_NAME, false, true, true, true);
	}

	@Test
	public void test072AddRoles123AssignmentYYYImmediate() throws Exception {
		final String TEST_NAME = "test072AddRoles123AssignmentYYYImmediate";
		TestUtil.displayTestTile(this, TEST_NAME);
		login(userAdministrator);

		unassignAllRoles(USER_JACK_OID);
		executeAssignRoles123ToJack(TEST_NAME, true, true, true, true);
	}

	/**
	 * Assigning Role1 with two deputies present. (But approved by the delegator.)
	 */
	@Test
	public void test130AddRole1aAssignmentWithDeputy() throws Exception {
		final String TEST_NAME = "test130AddRole1aAssignmentWithDeputy";
		TestUtil.displayTestTile(this, TEST_NAME);
		login(userAdministrator);

		Task task = createTask(TEST_NAME);
		importLead1Deputies(task, task.getResult());

		unassignAllRoles(USER_JACK_OID);
		executeAssignRole1ToJack(TEST_NAME, false, true, null);
	}

	/**
	 * Assigning Role1 with two deputies present. (Approved by one of the deputies.)
	 */
	@Test
	public void test132AddRole1aAssignmentWithDeputyApprovedByDeputy1() throws Exception {
		final String TEST_NAME = "test132AddRole1aAssignmentWithDeputyApprovedByDeputy1";
		TestUtil.displayTestTile(this, TEST_NAME);
		login(userAdministrator);

		unassignAllRoles(USER_JACK_OID);
		executeAssignRole1ToJack(TEST_NAME, false, true, USER_LEAD1_DEPUTY_1_OID);
	}


	private void executeAssignRole1ToJack(String TEST_NAME, boolean immediate, boolean deputy, String approverOid) throws Exception {
		PrismObject<UserType> jack = getUser(USER_JACK_OID);
		ObjectDelta<UserType> addRole1Delta = (ObjectDelta<UserType>) DeltaBuilder
				.deltaFor(UserType.class, prismContext)
				.item(UserType.F_ASSIGNMENT).add(createAssignmentTo(getRoleOid(1), ObjectTypes.ROLE, prismContext))
				.asObjectDelta(USER_JACK_OID);
		String realApproverOid = approverOid != null ? approverOid : USER_LEAD1_OID;
		executeTest2(TEST_NAME, new TestDetails2<UserType>() {
			@Override
			protected PrismObject<UserType> getFocus(OperationResult result) throws Exception {
				return jack.clone();
			}

			@Override
			protected ObjectDelta<UserType> getFocusDelta() throws SchemaException {
				return addRole1Delta.clone();
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
				return Collections.singletonList(addRole1Delta.clone());
			}

			@Override
			protected ObjectDelta<UserType> getExpectedDelta0() {
				return ObjectDelta.createModifyDelta(jack.getOid(), Collections.emptyList(), UserType.class, prismContext);
			}

			@Override
			protected String getObjectOid() {
				return jack.getOid();
			}

			@Override
			protected List<ExpectedTask> getExpectedTasks() {
				return Collections.singletonList(new ExpectedTask(getRoleOid(1), "Assigning " + getRoleName(1) + " to jack"));
			}

			@Override
			protected List<ExpectedWorkItem> getExpectedWorkItems() {
				ExpectedTask etask = getExpectedTasks().get(0);
				return Collections.singletonList(new ExpectedWorkItem(USER_LEAD1_OID, getRoleOid(1), etask));
			}

			@Override
			protected void assertDeltaExecuted(int number, boolean yes, Task rootTask, OperationResult result) throws Exception {
				if (number == 1) {
					if (yes) {
						assertAssignedRole(USER_JACK_OID, getRoleOid(1), rootTask, result);
						checkWorkItemAuditRecords(createResultMap(getRoleOid(1), WorkflowResult.APPROVED));
						checkUserApprovers(USER_JACK_OID, Collections.singletonList(realApproverOid), result);
					} else {
						assertNotAssignedRole(USER_JACK_OID, getRoleOid(1), rootTask, result);
					}
				}
			}

			@Override
			protected Boolean decideOnApproval(String executionId) throws Exception {
				assertActiveWorkItems(USER_LEAD1_OID, 1);
				assertActiveWorkItems(USER_LEAD1_DEPUTY_1_OID, deputy ? 1 : 0);
				assertActiveWorkItems(USER_LEAD1_DEPUTY_2_OID, deputy ? 1 : 0);
				checkTargetOid(executionId, getRoleOid(1));
				login(getUser(realApproverOid));
				return true;
			}
		}, 1, immediate);
	}

	protected List<PrismReferenceValue> getPotentialAssignees(PrismObject<UserType> user) {
		List<PrismReferenceValue> rv = new ArrayList<>();
		rv.add(ObjectTypeUtil.createObjectRef(user).asReferenceValue());
		rv.addAll(DeputyUtils.getDelegatorReferences(user.asObjectable()));
		return rv;
	}

	protected void assertActiveWorkItems(String approverOid, int expectedCount) throws Exception {
		if (approverOid == null && expectedCount == 0) {
			return;
		}
		Task task = createTask("query");
		ObjectQuery query = QueryBuilder.queryFor(WorkItemType.class, prismContext)
				.item(WorkItemType.F_ASSIGNEE_REF).ref(getPotentialAssignees(getUser(approverOid)))
				.build();
		List<WorkItemType> items = modelService.searchContainers(WorkItemType.class, query, null, task, task.getResult());
		assertEquals("Wrong active work items for " + approverOid, expectedCount, items.size());
	}

//	private void executeAssignRole1aToJack(String TEST_NAME, boolean immediate, boolean deputy, String approverOid) throws Exception {
//		PrismObject<UserType> jack = getUser(USER_JACK_OID);
//		ObjectDelta<UserType> addRole1aDelta = (ObjectDelta<UserType>) DeltaBuilder
//				.deltaFor(UserType.class, prismContext)
//				.item(UserType.F_ASSIGNMENT).add(createAssignmentTo(ROLE_ROLE1A_OID, ObjectTypes.ROLE, prismContext))
//				.asObjectDelta(USER_JACK_OID);
//		String realApproverOid = approverOid != null ? approverOid : USER_LEAD1_OID;
//		executeTest2(TEST_NAME, new TestDetails2<UserType>() {
//			@Override
//			protected PrismObject<UserType> getFocus(OperationResult result) throws Exception {
//				return jack.clone();
//			}
//
//			@Override
//			protected ObjectDelta<UserType> getFocusDelta() throws SchemaException {
//				return addRole1aDelta.clone();
//			}
//
//			@Override
//			protected int getNumberOfDeltasToApprove() {
//				return 1;
//			}
//
//			@Override
//			protected List<Boolean> getApprovals() {
//				return Collections.singletonList(true);
//			}
//
//			@Override
//			protected List<ObjectDelta<UserType>> getExpectedDeltasToApprove() {
//				return Collections.singletonList(addRole1aDelta.clone());
//			}
//
//			@Override
//			protected ObjectDelta<UserType> getExpectedDelta0() {
//				return ObjectDelta.createModifyDelta(jack.getOid(), Collections.emptyList(), UserType.class, prismContext);
//			}
//
//			@Override
//			protected String getObjectOid() {
//				return jack.getOid();
//			}
//
//			@Override
//			protected List<ExpectedTask> getExpectedTasks() {
//				return Collections.singletonList(new ExpectedTask(ROLE_ROLE1A_OID, "Assigning Role1a to jack"));
//			}
//
//			@Override
//			protected List<ExpectedWorkItem> getExpectedWorkItems() {
//				ExpectedTask etask = getExpectedTasks().get(0);
//				return Collections.singletonList(new ExpectedWorkItem(USER_LEAD1_OID, ROLE_ROLE1A_OID, etask));
//			}
//
//			@Override
//			protected void assertDeltaExecuted(int number, boolean yes, Task rootTask, OperationResult result) throws Exception {
//				if (number == 1) {
//					if (yes) {
//						assertAssignedRole(USER_JACK_OID, ROLE_ROLE1A_OID, rootTask, result);
//						checkWorkItemAuditRecords(createResultMap(ROLE_ROLE1A_OID, WorkflowResult.APPROVED));
//						checkUserApprovers(USER_JACK_OID, Collections.singletonList(realApproverOid), result);
//					} else {
//						assertNotAssignedRole(USER_JACK_OID, ROLE_ROLE1A_OID, rootTask, result);
//					}
//				}
//			}
//
//			@Override
//			protected Boolean decideOnApproval(String executionId) throws Exception {
//				assertActiveWorkItems(USER_LEAD1_OID, 1);
//				assertActiveWorkItems(USER_LEAD1_DEPUTY_1_OID, deputy ? 1 : 0);
//				assertActiveWorkItems(USER_LEAD1_DEPUTY_2_OID, deputy ? 1 : 0);
//				checkTargetOid(executionId, ROLE_ROLE1A_OID);
//				login(getUser(realApproverOid));
//				return true;
//			}
//		}, 1, immediate);
//	}

	private void executeAssignRoles123ToJack(String TEST_NAME, boolean immediate, boolean approve1, boolean approve2, boolean approve3) throws Exception {
		PrismObject<UserType> jack = getUser(USER_JACK_OID);
		@SuppressWarnings("unchecked")
		ObjectDelta<UserType> addRole1Delta = (ObjectDelta<UserType>) DeltaBuilder
				.deltaFor(UserType.class, prismContext)
				.item(UserType.F_ASSIGNMENT).add(createAssignmentTo(getRoleOid(1), ObjectTypes.ROLE, prismContext))
				.asObjectDelta(USER_JACK_OID);
		@SuppressWarnings("unchecked")
		ObjectDelta<UserType> addRole2Delta = (ObjectDelta<UserType>) DeltaBuilder
				.deltaFor(UserType.class, prismContext)
				.item(UserType.F_ASSIGNMENT).add(createAssignmentTo(getRoleOid(2), ObjectTypes.ROLE, prismContext))
				.asObjectDelta(USER_JACK_OID);
		@SuppressWarnings("unchecked")
		ObjectDelta<UserType> addRole3Delta = (ObjectDelta<UserType>) DeltaBuilder
				.deltaFor(UserType.class, prismContext)
				.item(UserType.F_ASSIGNMENT).add(createAssignmentTo(getRoleOid(3), ObjectTypes.ROLE, prismContext))
				.asObjectDelta(USER_JACK_OID);
		@SuppressWarnings("unchecked")
		ObjectDelta<UserType> addRole4Delta = (ObjectDelta<UserType>) DeltaBuilder
				.deltaFor(UserType.class, prismContext)
				.item(UserType.F_ASSIGNMENT).add(createAssignmentTo(getRoleOid(4), ObjectTypes.ROLE, prismContext))
				.asObjectDelta(USER_JACK_OID);
		@SuppressWarnings("unchecked")
		ObjectDelta<UserType> changeDescriptionDelta = (ObjectDelta<UserType>) DeltaBuilder
				.deltaFor(UserType.class, prismContext)
				.item(UserType.F_DESCRIPTION).replace(TEST_NAME)
				.asObjectDelta(USER_JACK_OID);
		ObjectDelta<UserType> primaryDelta = ObjectDelta.summarize(addRole1Delta, addRole2Delta, addRole3Delta, addRole4Delta, changeDescriptionDelta);
		ObjectDelta<UserType> delta0 = ObjectDelta.summarize(addRole4Delta, changeDescriptionDelta);
		String originalDescription = getUser(USER_JACK_OID).asObjectable().getDescription();
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
				return Arrays.asList(approve1, approve2, approve3);
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
						new ExpectedTask(getRoleOid(1), "Assigning "+getRoleName(1)+" to jack"),
						new ExpectedTask(getRoleOid(2), "Assigning "+getRoleName(2)+" to jack"),
						new ExpectedTask(getRoleOid(3), "Assigning "+getRoleName(3)+" to jack"));
			}

			@Override
			protected List<ExpectedWorkItem> getExpectedWorkItems() {
				List<ExpectedTask> etasks = getExpectedTasks();
				return Arrays.asList(
						new ExpectedWorkItem(USER_LEAD1_OID, getRoleOid(1), etasks.get(0)),
						new ExpectedWorkItem(USER_LEAD2_OID, getRoleOid(2), etasks.get(1)),
						new ExpectedWorkItem(USER_LEAD3_OID, getRoleOid(3), etasks.get(2))
				);
			}

			@Override
			protected void assertDeltaExecuted(int number, boolean yes, Task rootTask, OperationResult result) throws Exception {
				switch (number) {
					case 0:
						if (yes) {
							assertUserProperty(USER_JACK_OID, UserType.F_DESCRIPTION, TEST_NAME);
							assertAssignedRole(USER_JACK_OID, getRoleOid(4), rootTask, result);
						} else {
							if (originalDescription != null) {
								assertUserProperty(USER_JACK_OID, UserType.F_DESCRIPTION, originalDescription);
							} else {
								assertUserNoProperty(USER_JACK_OID, UserType.F_DESCRIPTION);
							}
							assertNotAssignedRole(USER_JACK_OID, getRoleOid(4), rootTask, result);
						}
						break;
					case 1:
					case 2:
					case 3:
					if (yes) {
						assertAssignedRole(USER_JACK_OID, getRoleOid(number), rootTask, result);
					} else {
						assertNotAssignedRole(USER_JACK_OID, getRoleOid(number), rootTask, result);
					}
					break;

				}
			}

			@Override
			protected Boolean decideOnApproval(String executionId) throws Exception {
				String targetOid = getTargetOid(executionId);
				if (getRoleOid(1).equals(targetOid)) {
					login(getUser(USER_LEAD1_OID));
					return approve1;
				} else if (getRoleOid(2).equals(targetOid)) {
					login(getUser(USER_LEAD2_OID));
					return approve2;
				} else if (getRoleOid(3).equals(targetOid)) {
					login(getUser(USER_LEAD3_OID));
					return approve3;
				} else {
					throw new IllegalStateException("Unexpected approval request for " + targetOid);
				}
			}
		}, 3, immediate);
	}

	@Test
	public void zzzMarkAsNotInitialized() {
		display("Setting class as not initialized");
		unsetSystemInitialized();
	}

}
