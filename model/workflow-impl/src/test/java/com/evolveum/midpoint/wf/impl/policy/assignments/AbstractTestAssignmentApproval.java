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
package com.evolveum.midpoint.wf.impl.policy.assignments;

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
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.policy.AbstractWfTestPolicy;
import com.evolveum.midpoint.wf.impl.policy.ExpectedTask;
import com.evolveum.midpoint.wf.impl.policy.ExpectedWorkItem;
import com.evolveum.midpoint.wf.impl.WorkflowResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkItemType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.createAssignmentTo;
import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.assertEquals;

/**
 * Testing approvals of role assignments: create/delete assignment, potentially for more roles and combined with other operations.
 * Testing also with deputies specified.
 *
 * Subclasses provide specializations regarding ways how rules and/or approvers are attached to roles.
 *
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
	 * (Even if it was, Role10 assignment would not be approved, see test030.)
     */
	@Test
    public void test010AddRole1Assignment() throws Exception {
        final String TEST_NAME = "test010AddRole1Assignment";
        TestUtil.displayTestTile(this, TEST_NAME);
        login(userAdministrator);

		executeAssignRole1ToJack(TEST_NAME, false, false, null, null);
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
		fillContextWithUser(context, userJackOid, result);
		addFocusDeltaToContext(context,
				(ObjectDelta<UserType>) DeltaBuilder.deltaFor(UserType.class, prismContext)
						.item(UserType.F_ASSIGNMENT).delete(createAssignmentTo(getRoleOid(1), ObjectTypes.ROLE, prismContext))
						.asObjectDelta(userJackOid));
		clockwork.run(context, task, result);

		assertEquals("Wrong context state", ModelState.FINAL, context.getState());
		TestUtil.assertSuccess(result);
		assertNotAssignedRole(getUser(userJackOid), getRoleOid(1), task, result);
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

		executeAssignRole1ToJack(TEST_NAME, false, false, null, null);
	}

	/**
	 * The same as above, but with immediate execution.
	 */
	@Test
	public void test040AddRole1AssignmentImmediate() throws Exception {
		final String TEST_NAME = "test040AddRole1AssignmentImmediate";
		TestUtil.displayTestTile(this, TEST_NAME);
		login(userAdministrator);

		unassignAllRoles(userJackOid);
		executeAssignRole1ToJack(TEST_NAME, true, false, null, null);
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

		unassignAllRoles(userJackOid);
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

		unassignAllRoles(userJackOid);
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

		unassignAllRoles(userJackOid);
		executeAssignRoles123ToJack(TEST_NAME, false, true, false, false);
	}

	@Test
	public void test062AddRoles123AssignmentYNNImmediate() throws Exception {
		final String TEST_NAME = "test062AddRoles123AssignmentYNNImmediate";
		TestUtil.displayTestTile(this, TEST_NAME);
		login(userAdministrator);

		unassignAllRoles(userJackOid);
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

		unassignAllRoles(userJackOid);
		executeAssignRoles123ToJack(TEST_NAME, false, true, true, true);
	}

	@Test
	public void test072AddRoles123AssignmentYYYImmediate() throws Exception {
		final String TEST_NAME = "test072AddRoles123AssignmentYYYImmediate";
		TestUtil.displayTestTile(this, TEST_NAME);
		login(userAdministrator);

		unassignAllRoles(userJackOid);
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

		unassignAllRoles(userJackOid);
		executeAssignRole1ToJack(TEST_NAME, false, true, null, null);
	}

	/**
	 * Assigning Role1 with two deputies present. (Approved by one of the deputies.)
	 */
	@Test
	public void test132AddRole1aAssignmentWithDeputyApprovedByDeputy1() throws Exception {
		final String TEST_NAME = "test132AddRole1aAssignmentWithDeputyApprovedByDeputy1";
		TestUtil.displayTestTile(this, TEST_NAME);
		login(userAdministrator);

		unassignAllRoles(userJackOid);
		executeAssignRole1ToJack(TEST_NAME, false, true, userLead1Deputy1Oid, null);
	}

	@Test(enabled = false)
	public void test150AddRole1ApproverAssignment() throws Exception {
		final String TEST_NAME = "test150AddRole1ApproverAssignment";
		TestUtil.displayTestTile(this, TEST_NAME);
		login(userAdministrator);

		unassignAllRoles(userJackOid);
		executeAssignRole1ToJack(TEST_NAME, false, true, null, SchemaConstants.ORG_APPROVER);
	}

	private void executeAssignRole1ToJack(String TEST_NAME, boolean immediate, boolean deputy, String approverOid, QName relation) throws Exception {
		PrismObject<UserType> jack = getUser(userJackOid);
		AssignmentType assignment = createAssignmentTo(getRoleOid(1), ObjectTypes.ROLE, prismContext);
		assignment.getTargetRef().setRelation(relation);
		ObjectDelta<UserType> addRole1Delta = (ObjectDelta<UserType>) DeltaBuilder
				.deltaFor(UserType.class, prismContext)
				.item(UserType.F_ASSIGNMENT).add(assignment)
				.asObjectDelta(userJackOid);
		String realApproverOid = approverOid != null ? approverOid : userLead1Oid;
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
				return Collections.singletonList(new ExpectedWorkItem(userLead1Oid, getRoleOid(1), etask));
			}

			@Override
			protected void assertDeltaExecuted(int number, boolean yes, Task rootTask, OperationResult result) throws Exception {
				if (number == 1) {
					if (yes) {
						assertAssignedRole(userJackOid, getRoleOid(1), rootTask, result);
						checkWorkItemAuditRecords(createResultMap(getRoleOid(1), WorkflowResult.APPROVED));
						checkUserApprovers(userJackOid, Collections.singletonList(realApproverOid), result);
					} else {
						assertNotAssignedRole(userJackOid, getRoleOid(1), rootTask, result);
					}
				}
			}

			@Override
			protected Boolean decideOnApproval(String executionId, org.activiti.engine.task.Task task) throws Exception {
				assertActiveWorkItems(userLead1Oid, 1);
				assertActiveWorkItems(userLead1Deputy1Oid, deputy ? 1 : 0);
				assertActiveWorkItems(userLead1Deputy2Oid, deputy ? 1 : 0);
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

	private void executeAssignRoles123ToJack(String TEST_NAME, boolean immediate, boolean approve1, boolean approve2, boolean approve3) throws Exception {
		PrismObject<UserType> jack = getUser(userJackOid);
		@SuppressWarnings("unchecked")
		ObjectDelta<UserType> addRole1Delta = (ObjectDelta<UserType>) DeltaBuilder
				.deltaFor(UserType.class, prismContext)
				.item(UserType.F_ASSIGNMENT).add(createAssignmentTo(getRoleOid(1), ObjectTypes.ROLE, prismContext))
				.asObjectDelta(userJackOid);
		@SuppressWarnings("unchecked")
		ObjectDelta<UserType> addRole2Delta = (ObjectDelta<UserType>) DeltaBuilder
				.deltaFor(UserType.class, prismContext)
				.item(UserType.F_ASSIGNMENT).add(createAssignmentTo(getRoleOid(2), ObjectTypes.ROLE, prismContext))
				.asObjectDelta(userJackOid);
		@SuppressWarnings("unchecked")
		ObjectDelta<UserType> addRole3Delta = (ObjectDelta<UserType>) DeltaBuilder
				.deltaFor(UserType.class, prismContext)
				.item(UserType.F_ASSIGNMENT).add(createAssignmentTo(getRoleOid(3), ObjectTypes.ROLE, prismContext))
				.asObjectDelta(userJackOid);
		@SuppressWarnings("unchecked")
		ObjectDelta<UserType> addRole4Delta = (ObjectDelta<UserType>) DeltaBuilder
				.deltaFor(UserType.class, prismContext)
				.item(UserType.F_ASSIGNMENT).add(createAssignmentTo(getRoleOid(4), ObjectTypes.ROLE, prismContext))
				.asObjectDelta(userJackOid);
		@SuppressWarnings("unchecked")
		ObjectDelta<UserType> changeDescriptionDelta = (ObjectDelta<UserType>) DeltaBuilder
				.deltaFor(UserType.class, prismContext)
				.item(UserType.F_DESCRIPTION).replace(TEST_NAME)
				.asObjectDelta(userJackOid);
		ObjectDelta<UserType> primaryDelta = ObjectDelta.summarize(addRole1Delta, addRole2Delta, addRole3Delta, addRole4Delta, changeDescriptionDelta);
		ObjectDelta<UserType> delta0 = ObjectDelta.summarize(addRole4Delta, changeDescriptionDelta);
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
						new ExpectedWorkItem(userLead1Oid, getRoleOid(1), etasks.get(0)),
						new ExpectedWorkItem(userLead2Oid, getRoleOid(2), etasks.get(1)),
						new ExpectedWorkItem(userLead3Oid, getRoleOid(3), etasks.get(2))
				);
			}

			@Override
			protected void assertDeltaExecuted(int number, boolean yes, Task rootTask, OperationResult result) throws Exception {
				switch (number) {
					case 0:
						if (yes) {
							assertUserProperty(userJackOid, UserType.F_DESCRIPTION, TEST_NAME);
							assertAssignedRole(userJackOid, getRoleOid(4), rootTask, result);
						} else {
							if (originalDescription != null) {
								assertUserProperty(userJackOid, UserType.F_DESCRIPTION, originalDescription);
							} else {
								assertUserNoProperty(userJackOid, UserType.F_DESCRIPTION);
							}
							assertNotAssignedRole(userJackOid, getRoleOid(4), rootTask, result);
						}
						break;
					case 1:
					case 2:
					case 3:
					if (yes) {
						assertAssignedRole(userJackOid, getRoleOid(number), rootTask, result);
					} else {
						assertNotAssignedRole(userJackOid, getRoleOid(number), rootTask, result);
					}
					break;

				}
			}

			@Override
			protected Boolean decideOnApproval(String executionId, org.activiti.engine.task.Task task) throws Exception {
				String targetOid = getTargetOid(executionId);
				if (getRoleOid(1).equals(targetOid)) {
					login(getUser(userLead1Oid));
					return approve1;
				} else if (getRoleOid(2).equals(targetOid)) {
					login(getUser(userLead2Oid));
					return approve2;
				} else if (getRoleOid(3).equals(targetOid)) {
					login(getUser(userLead3Oid));
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
